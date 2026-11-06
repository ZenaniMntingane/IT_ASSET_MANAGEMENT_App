package com.example.it_asset_management_app

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuditTrail : AppCompatActivity() {

    // ---------------------------------------------------------
    // FIREBASE
    // ---------------------------------------------------------

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var realtimeDB: DatabaseReference

    // ---------------------------------------------------------
    // VIEWS
    // ---------------------------------------------------------

    private lateinit var auditTrailContainer: LinearLayout
    private lateinit var emptyAuditView: TextView
    private lateinit var tvTotalRecords: TextView
    private lateinit var etSearchAudit: TextInputEditText

    // ---------------------------------------------------------
    // FIREBASE LISTENERS
    // ---------------------------------------------------------

    private var firestoreListener: ListenerRegistration? = null

    private var realtimeListener: ValueEventListener? = null

    // ---------------------------------------------------------
    // AUDIT RECORD COLLECTIONS
    // ---------------------------------------------------------

    private val firestoreRecords = mutableListOf<AuditRecord>()

    private val realtimeRecords = mutableListOf<AuditRecord>()

    private val auditRecords = mutableListOf<AuditRecord>()

    private val dateFormatter = SimpleDateFormat(
        "dd MMM yyyy, HH:mm:ss",
        Locale.getDefault()
    )

    // ---------------------------------------------------------
    // ON CREATE
    // ---------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_audit_trail)
        auth = FirebaseAuth.getInstance()

        firestore = FirebaseFirestore.getInstance()

        realtimeDB = FirebaseDatabase.getInstance()
            .reference
            .child("auditTrail")

        // Bind views.
        auditTrailContainer =
            findViewById(R.id.auditTrailContainer)

        emptyAuditView =
            findViewById(R.id.emptyAuditView)

        tvTotalRecords =
            findViewById(R.id.tvTotalRecords)

        etSearchAudit =
            findViewById(R.id.etSearchAudit)

        // Back button.
        findViewById<ImageButton>(R.id.btnBack)
            .setOnClickListener {
                finish()
            }

        // Search activities.
        etSearchAudit.addTextChangedListener(
            object : android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    displayAuditRecords(
                        s?.toString().orEmpty()
                    )
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) = Unit
            }
        )

        if (auth.currentUser == null) {

            toast("Please log in to view the audit trail.")

            finish()

            return
        }

        loadAuditRecords()
    }

    // ---------------------------------------------------------
    // LOAD ALL AUDIT RECORDS
    // ---------------------------------------------------------

    private fun loadAuditRecords() {

        emptyAuditView.text = "Loading system activities..."

        emptyAuditView.visibility = View.VISIBLE

        // Load Firestore audit records.
        loadFirestoreAuditRecords()

        // Load Realtime Database audit records.
        loadRealtimeAuditRecords()
    }

    // ---------------------------------------------------------
    // LOAD FROM FIRESTORE
    // COLLECTION: auditTrails
    // ---------------------------------------------------------

    private fun loadFirestoreAuditRecords() {

        firestoreListener = firestore
            .collection("auditTrails")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {

                    Log.e(
                        "AuditTrail",
                        "Firestore audit loading failed",
                        error
                    )

                    firestoreRecords.clear()

                    combineAuditRecords()

                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                firestoreRecords.clear()

                for (document in snapshot.documents) {

                    firestoreRecords.add(
                        mapDocumentToRecord(document)
                    )
                }

                Log.d(
                    "AuditTrail",
                    "Firestore records: ${firestoreRecords.size}"
                )

                combineAuditRecords()
            }
    }

    // ---------------------------------------------------------
    // LOAD FROM REALTIME DATABASE
    // NODE: auditTrail
    // ---------------------------------------------------------

    private fun loadRealtimeAuditRecords() {

        realtimeListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                realtimeRecords.clear()

                for (recordSnapshot in snapshot.children) {

                    val record = mapRealtimeRecord(recordSnapshot)

                    realtimeRecords.add(record)
                }

                Log.d(
                    "AuditTrail",
                    "Realtime Database records: ${realtimeRecords.size}"
                )

                combineAuditRecords()
            }

            override fun onCancelled(error: DatabaseError) {

                Log.e(
                    "AuditTrail",
                    "Realtime Database audit loading failed",
                    error.toException()
                )

                realtimeRecords.clear()

                combineAuditRecords()

                toast(
                    "Could not load Realtime Database audit records: " +
                            error.message
                )
            }
        }

        realtimeDB.addValueEventListener(realtimeListener!!)
    }

    // ---------------------------------------------------------
    // COMBINE FIRESTORE AND REALTIME DATABASE
    // ---------------------------------------------------------

    private fun combineAuditRecords() {

        auditRecords.clear()

        auditRecords.addAll(firestoreRecords)

        auditRecords.addAll(realtimeRecords)

        // Newest activities appear first.
        auditRecords.sortByDescending {
            it.timestamp
        }

        // Update total record count.
        tvTotalRecords.text = auditRecords.size.toString()

        // Display records.
        displayAuditRecords(
            etSearchAudit.text?.toString().orEmpty()
        )

        Log.d(
            "AuditTrail",
            "TOTAL AUDIT RECORDS: ${auditRecords.size}"
        )
    }

    // ---------------------------------------------------------
    // MAP FIRESTORE DOCUMENT
    // ---------------------------------------------------------

    private fun mapDocumentToRecord(
        document: DocumentSnapshot
    ): AuditRecord {

        return AuditRecord(

            id = document.id,

            action = readString(
                document,
                "action",
                "activity",
                "event",
                "operation",
                "actionType"
            ),

            user = readString(
                document,
                "performedByName",
                "userName",
                "performedByEmail",
                "performedBy",
                "user",
                "email"
            ),

            role = readString(
                document,
                "performedByRole",
                "role"
            ),

            assetNumber = readString(
                document,
                "assetNumber",
                "assetId",
                "assetID"
            ),

            details = readString(
                document,
                "details",
                "description",
                "message"
            ),

            status = readString(
                document,
                "status",
                "verificationStatus"
            ),

            hash = readString(
                document,
                "hash",
                "hashValue"
            ),

            digitalSignature = readString(
                document,
                "digitalSignature"
            ),

            timestamp = readTimestamp(
                document,
                "timestamp",
                "createdAt",
                "performedAt",
                "transferredAt",
                "date",
                "time"
            ),

            source = "Firestore"
        )
    }

    // ---------------------------------------------------------
    // MAP REALTIME DATABASE RECORD
    // ---------------------------------------------------------

    private fun mapRealtimeRecord(
        snapshot: DataSnapshot
    ): AuditRecord {

        return AuditRecord(

            id = snapshot.key.orEmpty(),

            action = readRealtimeString(
                snapshot,
                "action",
                "activity",
                "event",
                "operation",
                "actionType"
            ),

            user = readRealtimeString(
                snapshot,
                "performedByName",
                "userName",
                "performedByEmail",
                "performedBy",
                "user",
                "email"
            ),

            role = readRealtimeString(
                snapshot,
                "performedByRole",
                "role"
            ),

            assetNumber = readRealtimeString(
                snapshot,
                "assetNumber",
                "assetId",
                "assetID"
            ),

            details = readRealtimeString(
                snapshot,
                "details",
                "description",
                "message"
            ).ifBlank {

                buildString {

                    val oldUser = readRealtimeString(
                        snapshot,
                        "oldUser"
                    )

                    val newUser = readRealtimeString(
                        snapshot,
                        "newUser"
                    )

                    if (oldUser.isNotBlank()) {
                        append("Transferred from $oldUser")
                    }

                    if (newUser.isNotBlank()) {

                        if (isNotEmpty()) append(" to ")

                        else append("Transferred to ")

                        append(newUser)
                    }
                }
            },

            status = readRealtimeString(
                snapshot,
                "status",
                "verificationStatus"
            ),

            hash = readRealtimeString(
                snapshot,
                "hash",
                "hashValue"
            ),

            digitalSignature = readRealtimeString(
                snapshot,
                "digitalSignature"
            ),

            timestamp = readRealtimeTimestamp(
                snapshot,
                "timestamp",
                "createdAt",
                "performedAt",
                "transferredAt",
                "date",
                "time"
            ),

            source = "Realtime Database"
        )
    }

    // ---------------------------------------------------------
    // READ FIRESTORE STRINGS
    // ---------------------------------------------------------

    private fun readString(
        document: DocumentSnapshot,
        vararg fieldNames: String
    ): String {

        for (field in fieldNames) {

            val value = document.get(field) ?: continue

            when (value) {

                is String -> {
                    if (value.isNotBlank()) return value
                }

                is Number, is Boolean -> {
                    return value.toString()
                }

                is Map<*, *> -> {

                    val displayValue = listOf(
                        "name",
                        "fullName",
                        "email",
                        "value"
                    ).firstNotNullOfOrNull { key ->

                        value[key]?.toString()
                            ?.takeIf { it.isNotBlank() }
                    }

                    if (displayValue != null) {
                        return displayValue
                    }
                }
            }
        }

        return ""
    }

    // ---------------------------------------------------------
    // READ REALTIME DATABASE STRINGS
    // ---------------------------------------------------------

    private fun readRealtimeString(
        snapshot: DataSnapshot,
        vararg fieldNames: String
    ): String {

        for (field in fieldNames) {

            val value = snapshot.child(field)
                .getValue(Any::class.java)
                ?: continue

            when (value) {

                is String -> {
                    if (value.isNotBlank()) return value
                }

                is Number, is Boolean -> {
                    return value.toString()
                }

                is Map<*, *> -> {

                    val displayValue = listOf(
                        "name",
                        "fullName",
                        "email",
                        "value"
                    ).firstNotNullOfOrNull { key ->

                        value[key]?.toString()
                            ?.takeIf { it.isNotBlank() }
                    }

                    if (displayValue != null) {
                        return displayValue
                    }
                }
            }
        }

        return ""
    }

    // ---------------------------------------------------------
    // READ FIRESTORE TIMESTAMPS
    // ---------------------------------------------------------

    private fun readTimestamp(
        document: DocumentSnapshot,
        vararg fieldNames: String
    ): Long {

        for (field in fieldNames) {

            val value = document.get(field) ?: continue

            val result = parseTimestamp(value)

            if (result > 0L) return result
        }

        return 0L
    }

    // ---------------------------------------------------------
    // READ REALTIME DATABASE TIMESTAMPS
    // ---------------------------------------------------------

    private fun readRealtimeTimestamp(
        snapshot: DataSnapshot,
        vararg fieldNames: String
    ): Long {

        for (field in fieldNames) {

            val value = snapshot.child(field)
                .getValue(Any::class.java)
                ?: continue

            val result = parseTimestamp(value)

            if (result > 0L) return result
        }

        return 0L
    }

    // ---------------------------------------------------------
    // PARSE DATE AND TIME
    // ---------------------------------------------------------

    private fun parseTimestamp(value: Any): Long {

        when (value) {

            is Timestamp -> return value.toDate().time

            is Date -> return value.time

            is Number -> {

                val number = value.toLong()

                return if (
                    number in 1L..9_999_999_999L
                ) {
                    number * 1000L
                } else {
                    number
                }
            }

            is String -> {

                value.toLongOrNull()?.let { number ->

                    return if (
                        number in 1L..9_999_999_999L
                    ) {
                        number * 1000L
                    } else {
                        number
                    }
                }

                val formats = listOf(

                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",

                    "yyyy-MM-dd'T'HH:mm:ss'Z'",

                    "yyyy-MM-dd HH:mm:ss",

                    "yyyy-MM-dd HH:mm",

                    "dd/MM/yyyy HH:mm:ss",

                    "dd/MM/yyyy HH:mm",

                    "dd MMM yyyy, HH:mm"
                )

                for (format in formats) {

                    try {

                        val parsed = SimpleDateFormat(
                            format,
                            Locale.getDefault()
                        ).parse(value)

                        if (parsed != null) {
                            return parsed.time
                        }

                    } catch (_: Exception) {
                        // Try the next date format.
                    }
                }
            }
        }

        return 0L
    }

    // ---------------------------------------------------------
    // SEARCH AND DISPLAY AUDIT RECORDS
    // ---------------------------------------------------------

    private fun displayAuditRecords(query: String) {

        auditTrailContainer.removeAllViews()

        val searchText = query.trim()
            .lowercase(Locale.ROOT)

        val filteredRecords = auditRecords.filter { record ->

            listOf(

                record.id,

                record.action,

                record.user,

                record.role,

                record.assetNumber,

                record.details,

                record.status,

                record.hash,

                record.digitalSignature,

                record.source

            ).any { field ->

                field.lowercase(Locale.ROOT)
                    .contains(searchText)
            }
        }

        if (filteredRecords.isEmpty()) {

            emptyAuditView.text = if (auditRecords.isEmpty()) {

                "No system activities have been recorded yet."

            } else {

                "No activities match your search."
            }

            emptyAuditView.visibility = View.VISIBLE

            return
        }

        emptyAuditView.visibility = View.GONE

        filteredRecords.forEach { record ->

            auditTrailContainer.addView(
                createAuditCard(record)
            )
        }
    }

    // ---------------------------------------------------------
    // CREATE AUDIT CARD
    // ---------------------------------------------------------

    private fun createAuditCard(
        record: AuditRecord
    ): View {

        val card = MaterialCardView(this).apply {

            radius = dp(14).toFloat()

            cardElevation = dp(2).toFloat()

            setCardBackgroundColor(
                ContextCompat.getColor(
                    this@AuditTrail,
                    android.R.color.white
                )
            )

            strokeWidth = dp(1)

            strokeColor = 0xFFE7EAF1.toInt()

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        }

        val content = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(16)
            )
        }

        // Activity title.
        val actionView = TextView(this).apply {

            text = record.action.ifBlank {
                "Activity recorded"
            }

            textSize = 16f

            setTextColor(0xFF202B46.toInt())

            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        content.addView(actionView)

        // User.
        if (record.user.isNotBlank()) {

            content.addView(
                detailText("User: ${record.user}")
            )
        }

        // Role.
        if (record.role.isNotBlank()) {

            content.addView(
                detailText("Role: ${record.role}")
            )
        }

        // Asset number.
        if (record.assetNumber.isNotBlank()) {

            content.addView(
                detailText("Asset: ${record.assetNumber}")
            )
        }

        // Description.
        if (record.details.isNotBlank()) {

            content.addView(
                detailText("Details: ${record.details}")
            )
        }

        // Status.
        if (record.status.isNotBlank()) {

            content.addView(
                detailText("Status: ${record.status}")
            )
        }

        // Date and time.
        if (record.timestamp > 0L) {

            content.addView(
                detailText(
                    "Date: ${dateFormatter.format(Date(record.timestamp))}"
                )
            )
        }

        // Hash.
        if (record.hash.isNotBlank()) {

            content.addView(
                detailText("SHA-256 Hash: ${record.hash}")
            )
        }

        // Digital signature.
        if (record.digitalSignature.isNotBlank()) {

            content.addView(
                detailText(
                    "Digital Signature: ${record.digitalSignature}"
                )
            )
        }

        // Source database.
        content.addView(
            detailText("Source: ${record.source}")
        )

        // Record ID.
        content.addView(
            detailText("Record ID: ${record.id}")
        )

        card.addView(content)

        return card
    }

    // ---------------------------------------------------------
    // DETAIL TEXT
    // ---------------------------------------------------------

    private fun detailText(value: String): TextView {

        return TextView(this).apply {

            text = value

            textSize = 13f

            setTextColor(0xFF778198.toInt())

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        }
    }

    // ---------------------------------------------------------
    // DP CONVERSION
    // ---------------------------------------------------------

    private fun dp(value: Int): Int {

        return (
                value * resources.displayMetrics.density
                ).toInt()
    }

    // ---------------------------------------------------------
    // TOAST
    // ---------------------------------------------------------

    private fun toast(message: String) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    // ---------------------------------------------------------
    // RELEASE FIREBASE LISTENERS
    // ---------------------------------------------------------

    override fun onDestroy() {

        firestoreListener?.remove()

        firestoreListener = null

        realtimeListener?.let {
            realtimeDB.removeEventListener(it)
        }

        realtimeListener = null

        super.onDestroy()
    }

    // ---------------------------------------------------------
    // AUDIT RECORD MODEL
    // ---------------------------------------------------------

    data class AuditRecord(

        val id: String = "",

        val action: String = "",

        val user: String = "",

        val role: String = "",

        val assetNumber: String = "",

        val details: String = "",

        val status: String = "",

        val hash: String = "",

        val digitalSignature: String = "",

        val timestamp: Long = 0L,

        val source: String = ""
    )
}
