package com.example.it_asset_management_app

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AssetHistory : AppCompatActivity() {
// =====================================================
    // FIREBASE
    // =====================================================

    private val realtimeDb by lazy {
        FirebaseDatabase.getInstance().reference
    }

    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private lateinit var auth: FirebaseAuth

    // =====================================================
    // USER ACCESS
    // =====================================================

    private val allowedRoles = setOf(
        "admin",
        "it staff",
        "asset personnel"
    )

    private var currentUserRole = ""

    // =====================================================
    // VIEWS
    // =====================================================

    private lateinit var spAssetNumber: MaterialAutoCompleteTextView

    private lateinit var historyContainer: LinearLayout

    private lateinit var emptyHistoryView: TextView

    // =====================================================
    // ASSET DETAILS
    // =====================================================

    private lateinit var txtAssetName: TextView
    private lateinit var txtAssetNumber: TextView
    private lateinit var txtSerialNumber: TextView
    private lateinit var txtAssetType: TextView
    private lateinit var txtCondition: TextView
    private lateinit var txtAssignedUser: TextView
    private lateinit var txtDepartment: TextView
    private lateinit var txtLocation: TextView

    // =====================================================
    // ASSET DATA
    // =====================================================

    private val assetNumbers = mutableListOf<String>()

    private var selectedAssetNumber = ""

    // =====================================================
    // DATE FORMATTER
    // SOUTH AFRICAN LOCAL TIME
    // =====================================================

    private val southAfricanTimeZone: TimeZone
        get() = TimeZone.getTimeZone("Africa/Johannesburg")

    private fun formatLocalDate(timestamp: Long): String {

        if (timestamp <= 0L) {
            return "Date not recorded"
        }

        val formatter = SimpleDateFormat(
            "dd MMM yyyy, HH:mm:ss",
            Locale.getDefault()
        )

        formatter.timeZone = southAfricanTimeZone

        return formatter.format(Date(timestamp))
    }

    // =====================================================
    // ON CREATE
    // =====================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_asset_history)
        auth = FirebaseAuth.getInstance()

        bindViews()

        // -------------------------------------------------
        // BACK BUTTON
        // -------------------------------------------------

        findViewById<ImageButton>(R.id.btnBack)
            .setOnClickListener {
                finish()
            }

        // -------------------------------------------------
        // ASSET NUMBER SELECTION
        // -------------------------------------------------

        spAssetNumber.setOnItemClickListener { _, _, position, _ ->

            if (position in assetNumbers.indices) {

                selectedAssetNumber = assetNumbers[position]

                loadSelectedAsset(selectedAssetNumber)

                loadAssetHistory(selectedAssetNumber)
            }
        }

        // -------------------------------------------------
        // VERIFY ROLE BEFORE LOADING ASSETS
        // -------------------------------------------------

        checkUserAccess()
    }

    // =====================================================
    // BIND VIEWS
    // =====================================================

    private fun bindViews() {

        spAssetNumber =
            findViewById(R.id.spAssetNumber)

        historyContainer =
            findViewById(R.id.historyContainer)

        emptyHistoryView =
            findViewById(R.id.emptyHistoryView)

        txtAssetName =
            findViewById(R.id.txtAssetName)

        txtAssetNumber =
            findViewById(R.id.txtAssetNumber)

        txtSerialNumber =
            findViewById(R.id.txtSerialNumber)

        txtAssetType =
            findViewById(R.id.txtAssetType)

        txtCondition =
            findViewById(R.id.txtCondition)

        txtAssignedUser =
            findViewById(R.id.txtAssignedUser)

        txtDepartment =
            findViewById(R.id.txtDepartment)

        txtLocation =
            findViewById(R.id.txtLocation)
    }

    // =====================================================
    // VERIFY USER ACCESS
    // FIRESTORE: users/{UID}
    // =====================================================

    private fun checkUserAccess() {

        val currentUser = auth.currentUser

        if (currentUser == null) {

            toast("Please log in to access Asset History.")

            finish()

            return
        }

        val uid = currentUser.uid

        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {

                    toast("User profile not found.")

                    finish()

                    return@addOnSuccessListener
                }

                val role = document.getString("role")
                    .orEmpty()
                    .trim()
                    .lowercase(Locale.ROOT)

                if (role !in allowedRoles) {

                    toast(
                        "You are not authorised to access Asset History."
                    )

                    finish()

                    return@addOnSuccessListener
                }

                currentUserRole = role

                Log.d(
                    "AssetHistory",
                    "Access granted to role: $role"
                )

                loadAssetNumbers()
            }
            .addOnFailureListener { error ->

                Log.e(
                    "AssetHistory",
                    "Role verification failed",
                    error
                )

                toast(
                    "Unable to verify your role: ${error.message}"
                )

                finish()
            }
    }

    // =====================================================
    // LOAD ASSET NUMBERS
    // REALTIME DATABASE: assets
    // =====================================================

    private fun loadAssetNumbers() {

        showEmpty("Loading available assets...")

        realtimeDb.child("assets")
            .addListenerForSingleValueEvent(

                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        assetNumbers.clear()

                        for (asset in snapshot.children) {

                            val number = asset.stringValue("assetNumber")
                                .ifBlank {
                                    asset.stringValue("assetId")
                                }
                                .ifBlank {
                                    asset.stringValue("assetTag")
                                }
                                .ifBlank {
                                    asset.key.orEmpty()
                                }

                            if (number.isNotBlank()) {

                                assetNumbers.add(number)
                            }
                        }

                        assetNumbers.sort()

                        spAssetNumber.setAdapter(
                            ArrayAdapter(
                                this@AssetHistory,
                                android.R.layout.simple_dropdown_item_1line,
                                assetNumbers
                            )
                        )

                        if (assetNumbers.isEmpty()) {

                            showEmpty("No assets were found.")

                            return
                        }

                        // Automatically select when there is one asset.
                        if (assetNumbers.size == 1) {

                            spAssetNumber.setText(
                                assetNumbers[0],
                                false
                            )

                            selectedAssetNumber = assetNumbers[0]

                            loadSelectedAsset(selectedAssetNumber)

                            loadAssetHistory(selectedAssetNumber)

                        } else {

                            showEmpty(
                                "Select an asset number to view its history."
                            )
                        }

                        Log.d(
                            "AssetHistory",
                            "Loaded ${assetNumbers.size} assets"
                        )
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        showEmpty(
                            "Unable to load assets: ${error.message}"
                        )
                    }
                }
            )
    }

    // =====================================================
    // LOAD SELECTED ASSET DETAILS
    // =====================================================

    private fun loadSelectedAsset(assetNumber: String) {

        realtimeDb.child("assets")
            .addListenerForSingleValueEvent(

                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        val asset = snapshot.children.firstOrNull {

                            it.stringValue("assetNumber")
                                .equals(assetNumber, true) ||

                                    it.stringValue("assetId")
                                        .equals(assetNumber, true) ||

                                    it.stringValue("assetTag")
                                        .equals(assetNumber, true) ||

                                    it.key.equals(assetNumber, true)
                        }

                        if (asset == null) {

                            displayEmptyAssetDetails(assetNumber)

                            return
                        }

                        txtAssetName.text =
                            "Asset name: " +
                                    asset.stringValue("assetName")
                                        .ifBlank {
                                            asset.stringValue("name")
                                        }
                                        .ifBlank { "—" }

                        txtAssetNumber.text =
                            "Asset number: " +
                                    assetNumber

                        txtSerialNumber.text =
                            "Serial number: " +
                                    asset.stringValue("serialNumber")
                                        .ifBlank { "—" }

                        txtAssetType.text =
                            "Asset type: " +
                                    asset.stringValue("assetType")
                                        .ifBlank { "—" }

                        txtCondition.text =
                            "Condition: " +
                                    asset.stringValue("condition")
                                        .ifBlank { "—" }

                        txtAssignedUser.text =
                            "Assigned user: " +
                                    asset.stringValue("assignedUser")
                                        .ifBlank { "—" }

                        txtDepartment.text =
                            "Department: " +
                                    asset.stringValue("department")
                                        .ifBlank { "—" }

                        txtLocation.text =
                            "Location: " +
                                    asset.stringValue("location")
                                        .ifBlank { "—" }
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        toast(
                            "Unable to load asset details: ${error.message}"
                        )
                    }
                }
            )
    }

    // =====================================================
    // DISPLAY EMPTY ASSET DETAILS
    // =====================================================

    private fun displayEmptyAssetDetails(assetNumber: String) {

        txtAssetName.text = "Asset name: —"

        txtAssetNumber.text = "Asset number: $assetNumber"

        txtSerialNumber.text = "Serial number: —"

        txtAssetType.text = "Asset type: —"

        txtCondition.text = "Condition: —"

        txtAssignedUser.text = "Assigned user: —"

        txtDepartment.text = "Department: —"

        txtLocation.text = "Location: —"
    }

    // =====================================================
    // LOAD ASSET HISTORY
    // SOURCES:
    // 1. REALTIME DATABASE: assetHistory
    // 2. FIRESTORE: transfers
    // =====================================================

    private fun loadAssetHistory(assetNumber: String) {

        historyContainer.removeAllViews()

        showEmpty("Loading asset history...")

        val records = mutableListOf<HistoryRecord>()

        var sourcesCompleted = 0

        // -------------------------------------------------
        // WAIT FOR BOTH DATABASE SOURCES
        // -------------------------------------------------

        fun finishSource() {

            sourcesCompleted++

            if (sourcesCompleted == 2) {

                // Prevent an earlier selection from replacing
                // the currently selected asset's results.
                if (selectedAssetNumber != assetNumber) {
                    return
                }

                val uniqueRecords = records
                    .distinctBy {
                        "${it.source}|${it.title}|${it.timestamp}|${it.details}"
                    }
                    .sortedByDescending {
                        it.timestamp
                    }

                historyContainer.removeAllViews()

                if (uniqueRecords.isEmpty()) {

                    showEmpty(
                        "No history records found for $assetNumber."
                    )

                } else {

                    emptyHistoryView.visibility = View.GONE

                    uniqueRecords.forEach {
                        addHistoryCard(it)
                    }
                }

                Log.d(
                    "AssetHistory",
                    "Total records for $assetNumber: ${uniqueRecords.size}"
                )
            }
        }

        // =================================================
        // SOURCE 1: REALTIME DATABASE ASSET HISTORY
        // =================================================

        realtimeDb.child("assetHistory")
            .child(assetNumber)
            .addListenerForSingleValueEvent(

                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        for (item in snapshot.children) {

                            val title = item.stringValue("action")
                                .ifBlank {
                                    item.stringValue("eventType")
                                }
                                .ifBlank {
                                    item.stringValue("type")
                                }
                                .ifBlank {
                                    "Asset history event"
                                }

                            val timestamp = item.timestampValue()

                            val date = if (timestamp > 0L) {

                                formatLocalDate(timestamp)

                            } else {

                                "Date not recorded"
                            }

                            val details = buildString {

                                appendField(
                                    "Performed by",
                                    item.stringValue("performedBy")
                                )

                                appendField(
                                    "User",
                                    item.stringValue("user")
                                )

                                appendField(
                                    "Role",
                                    item.stringValue("performedByRole")
                                )

                                appendField(
                                    "Department",
                                    item.stringValue("department")
                                )

                                appendField(
                                    "Location",
                                    item.stringValue("location")
                                )

                                appendField(
                                    "Details",
                                    item.stringValue("details")
                                )
                            }.ifBlank {
                                "No additional details recorded."
                            }

                            records.add(

                                HistoryRecord(

                                    title = title,

                                    details = details,

                                    date = date,

                                    timestamp = timestamp,

                                    source = "Realtime Database",

                                    verification = item.stringValue(
                                        "verificationStatus"
                                    ).ifBlank {
                                        "Not recorded"
                                    }
                                )
                            )
                        }

                        finishSource()
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        Log.e(
                            "AssetHistory",
                            "Realtime history failed",
                            error.toException()
                        )

                        toast(
                            "History database: ${error.message}"
                        )

                        finishSource()
                    }
                }
            )

        // =================================================
        // SOURCE 2: FIRESTORE TRANSFER RECORDS
        // =================================================

        firestore.collection("transfers")
            .get()
            .addOnSuccessListener { documents ->

                for (document in documents) {

                    val data = document.data

                    val recordAssetNumber = (

                            data["assetNumber"]
                                ?: data["assetId"]
                                ?: data["assetKey"]
                                ?: ""

                            ).toString()

                    if (
                        !recordAssetNumber.equals(
                            assetNumber,
                            ignoreCase = true
                        )
                    ) {
                        continue
                    }

                    val timestamp = document.timestampValue()

                    val date = if (timestamp > 0L) {

                        formatLocalDate(timestamp)

                    } else {

                        "Date not recorded"
                    }

                    val details = buildString {

                        appendField(
                            "Previous user",
                            data["oldUser"]?.toString()
                                ?: data["oldAssignedUser"]?.toString()
                        )

                        appendField(
                            "New user",
                            data["newUser"]?.toString()
                                ?: data["newAssignedUser"]?.toString()
                        )

                        appendField(
                            "From department",
                            data["oldDepartment"]?.toString()
                        )

                        appendField(
                            "To department",
                            data["newDepartment"]?.toString()
                        )

                        appendField(
                            "From location",
                            data["oldLocation"]?.toString()
                        )

                        appendField(
                            "To location",
                            data["newLocation"]?.toString()
                        )

                        appendField(
                            "Transferred by",
                            data["transferredBy"]?.toString()
                                ?: data["transferredByUid"]?.toString()
                                ?: data["performedBy"]?.toString()
                        )

                        appendField(
                            "Transfer ID",
                            document.id
                        )

                        appendField(
                            "Hash",
                            data["hash"]?.toString()
                                ?: data["hashValue"]?.toString()
                        )
                    }.ifBlank {
                        "No additional transfer details recorded."
                    }

                    val verification = (

                            data["verificationStatus"]
                                ?: data["signatureStatus"]
                                ?: if (data["verified"] == true) {
                                    "Verified"
                                } else {
                                    "Not recorded"
                                }

                            ).toString()

                    records.add(

                        HistoryRecord(

                            title = "Asset transfer",

                            details = details,

                            date = date,

                            timestamp = timestamp,

                            source = "Cloud Firestore",

                            verification = verification
                        )
                    )
                }

                finishSource()
            }
            .addOnFailureListener { error ->

                Log.e(
                    "AssetHistory",
                    "Firestore transfer history failed",
                    error
                )

                toast(
                    "Unable to load transfers: ${error.message}"
                )

                finishSource()
            }
    }

    // =====================================================
    // CREATE HISTORY CARD
    // =====================================================

    private fun addHistoryCard(record: HistoryRecord) {

        val card = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(16),
                dp(14),
                dp(16),
                dp(14)
            )

            setBackgroundColor(
                android.graphics.Color.WHITE
            )

            elevation = dp(2).toFloat()
        }

        val params = LinearLayout.LayoutParams(

            LinearLayout.LayoutParams.MATCH_PARENT,

            LinearLayout.LayoutParams.WRAP_CONTENT

        ).apply {

            bottomMargin = dp(12)
        }

        // -------------------------------------------------
        // EVENT TITLE
        // -------------------------------------------------

        val title = TextView(this).apply {

            text = record.title

            textSize = 17f

            setTextColor(
                android.graphics.Color.rgb(22, 139, 232)
            )

            setTypeface(
                null,
                android.graphics.Typeface.BOLD
            )
        }

        // -------------------------------------------------
        // DATE
        // -------------------------------------------------

        val date = TextView(this).apply {

            text = "Date: ${record.date}"

            textSize = 13f

            setTextColor(
                android.graphics.Color.DKGRAY
            )

            setPadding(
                0,
                dp(6),
                0,
                dp(8)
            )
        }

        // -------------------------------------------------
        // DETAILS
        // -------------------------------------------------

        val details = TextView(this).apply {

            text = record.details

            textSize = 14f

            setTextColor(
                android.graphics.Color.rgb(38, 50, 72)
            )
        }

        // -------------------------------------------------
        // SOURCE
        // -------------------------------------------------

        val source = TextView(this).apply {

            text = "Source: ${record.source}"

            textSize = 12f

            setTextColor(
                android.graphics.Color.GRAY
            )

            setPadding(
                0,
                dp(10),
                0,
                dp(3)
            )
        }

        // -------------------------------------------------
        // SIGNATURE VERIFICATION
        // -------------------------------------------------

        val verification = TextView(this).apply {

            text = "Verification: ${record.verification}"

            textSize = 12f

            setTextColor(
                android.graphics.Color.GRAY
            )
        }

        card.addView(title)

        card.addView(date)

        card.addView(details)

        card.addView(source)

        card.addView(verification)

        historyContainer.addView(card, params)
    }

    // =====================================================
    // EMPTY HISTORY MESSAGE
    // =====================================================

    private fun showEmpty(message: String) {

        emptyHistoryView.text = message

        emptyHistoryView.visibility = View.VISIBLE
    }

    // =====================================================
    // DP CONVERSION
    // =====================================================

    private fun dp(value: Int): Int {

        return (
                value * resources.displayMetrics.density
                ).toInt()
    }

    // =====================================================
    // FIREBASE STRING VALUE
    // =====================================================

    private fun DataSnapshot.stringValue(
        field: String
    ): String {

        val value = child(field).value

        return value?.toString()?.takeIf {
            it.isNotBlank()
        } ?: ""
    }

    // =====================================================
    // REALTIME DATABASE TIMESTAMP
    // =====================================================

    private fun DataSnapshot.timestampValue(): Long {

        val raw = child("timestamp").value
            ?: child("createdAt").value
            ?: child("date").value

        return convertToMillis(raw)
    }

    // =====================================================
    // FIRESTORE TIMESTAMP
    // =====================================================

    private fun DocumentSnapshot.timestampValue(): Long {

        val raw = get("transferredAt")
            ?: get("timestamp")
            ?: get("createdAt")
            ?: get("transferDate")
            ?: get("date")

        return convertToMillis(raw)
    }

    // =====================================================
    // CONVERT FIREBASE DATE TO MILLISECONDS
    // =====================================================

    private fun convertToMillis(value: Any?): Long {

        return when (value) {

            is Number -> {

                val number = value.toLong()

                // Epoch seconds converted to milliseconds.
                if (number in 1L..9_999_999_999L) {

                    number * 1000L

                } else {

                    number
                }
            }

            is Date -> {

                value.time
            }

            is Timestamp -> {

                value.toDate().time
            }

            is String -> {

                parseDate(value)
            }

            else -> 0L
        }
    }

    // =====================================================
    // PARSE DATE STRINGS
    // =====================================================

    private fun parseDate(value: String): Long {

        val cleanValue = value.trim()

        if (cleanValue.isBlank()) {
            return 0L
        }

        // Support numeric timestamps stored as strings.
        cleanValue.toLongOrNull()?.let { number ->

            return if (number in 1L..9_999_999_999L) {

                number * 1000L

            } else {

                number
            }
        }

        val formats = listOf(

            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",

            "yyyy-MM-dd'T'HH:mm:ssXXX",

            "yyyy-MM-dd'T'HH:mm:ss.SSSX",

            "yyyy-MM-dd'T'HH:mm:ssX",

            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",

            "yyyy-MM-dd'T'HH:mm:ss'Z'",

            "yyyy-MM-dd HH:mm:ss",

            "yyyy-MM-dd HH:mm",

            "yyyy-MM-dd",

            "dd/MM/yyyy HH:mm:ss",

            "dd/MM/yyyy HH:mm",

            "dd MMM yyyy, HH:mm:ss",

            "dd MMM yyyy, HH:mm"
        )

        for (pattern in formats) {

            try {

                val formatter = SimpleDateFormat(
                    pattern,
                    Locale.getDefault()
                )

                formatter.isLenient = false

                // Explicit offsets are interpreted from the input.
                // Literal Z dates represent UTC.
                // Dates without a timezone are treated as SAST.
                when {

                    pattern.contains("XXX") ||
                            pattern.endsWith("X") -> {
                        // The supplied offset determines the instant.
                    }

                    pattern.endsWith("'Z'") -> {
                        formatter.timeZone =
                            TimeZone.getTimeZone("UTC")
                    }

                    else -> {
                        formatter.timeZone =
                            southAfricanTimeZone
                    }
                }

                val parsed = formatter.parse(cleanValue)

                if (parsed != null) {

                    return parsed.time
                }

            } catch (_: Exception) {

                // Try the next supported date format.
            }
        }

        Log.w(
            "AssetHistory",
            "Could not parse date: $cleanValue"
        )

        return 0L
    }

    // =====================================================
    // APPEND DETAILS
    // =====================================================

    private fun StringBuilder.appendField(
        label: String,
        value: String?
    ) {

        if (!value.isNullOrBlank() && value != "null") {

            if (isNotEmpty()) {
                append("\n")
            }

            append("$label: $value")
        }
    }

    // =====================================================
    // HISTORY RECORD MODEL
    // =====================================================

    data class HistoryRecord(

        val title: String = "",

        val details: String = "",

        val date: String = "",

        val timestamp: Long = 0L,

        val source: String = "",

        val verification: String = "Not recorded"
    )

    // =====================================================
    // TOAST
    // =====================================================

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

}