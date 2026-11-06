package com.example.it_asset_management_app

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MaintenanceAdd : AppCompatActivity() {
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val database: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().reference
    }

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private lateinit var assetNumberDropdown: MaterialAutoCompleteTextView
    private lateinit var assetNameInput: TextInputEditText
    private lateinit var technicianDropdown: MaterialAutoCompleteTextView
    private lateinit var issueInput: TextInputEditText
    private lateinit var priorityDropdown: MaterialAutoCompleteTextView
    private lateinit var statusDropdown: MaterialAutoCompleteTextView
    private lateinit var saveButton: MaterialButton

    private val assetNames = mutableMapOf<String, String>()
    private val technicianIds = mutableMapOf<String, String>()

    private var selectedAssetHasActiveMaintenance = false
    private var checkingMaintenance = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_maintenance_add)
        bindViews()
        setupPriorityDropdown()
        setupStatusDropdown()
        setupClickListeners()

        saveButton.isEnabled = false

        loadAssets()
        loadITStaff()
    }

    // ------------------------------------------------
    // BIND VIEWS
    // ------------------------------------------------

    private fun bindViews() {

        assetNumberDropdown =
            findViewById(R.id.etAssetNumber)

        assetNameInput =
            findViewById(R.id.etAssetName)

        technicianDropdown =
            findViewById(R.id.tilTechnician)

        issueInput =
            findViewById(R.id.etIssue)

        priorityDropdown =
            findViewById(R.id.spPriority)

        statusDropdown =
            findViewById(R.id.spStatus)

        saveButton =
            findViewById(R.id.btnSaveMaintenance)
    }

    // ------------------------------------------------
    // PRIORITY DROPDOWN
    // ------------------------------------------------

    private fun setupPriorityDropdown() {

        val priorities = listOf(
            "Low",
            "Medium",
            "High",
            "Critical"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            priorities
        )

        priorityDropdown.setAdapter(adapter)

        priorityDropdown.setOnClickListener {
            priorityDropdown.showDropDown()
        }

        priorityDropdown.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                priorityDropdown.showDropDown()
            }
        }
    }

    // ------------------------------------------------
    // STATUS DROPDOWN
    // ------------------------------------------------

    private fun setupStatusDropdown() {

        val statuses = listOf(
            "Pending",
            "In Progress",
            "Completed"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            statuses
        )

        statusDropdown.setAdapter(adapter)

        statusDropdown.setOnClickListener {
            statusDropdown.showDropDown()
        }

        statusDropdown.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                statusDropdown.showDropDown()
            }
        }
    }

    // ------------------------------------------------
    // CLICK LISTENERS
    // ------------------------------------------------

    private fun setupClickListeners() {

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        assetNumberDropdown.setOnItemClickListener {
                _, _, position, _ ->

            val selectedNumber =
                assetNumberDropdown.text.toString().trim()

            assetNameInput.setText(
                assetNames[selectedNumber].orEmpty()
            )

            // Check whether this asset is already
            // registered for maintenance.
            selectedAssetHasActiveMaintenance = false
            checkingMaintenance = true
            saveButton.isEnabled = false

            checkExistingMaintenance(selectedNumber) {
                    alreadyLogged, existingStatus ->

                // Ignore an old result if another asset
                // has since been selected.
                if (assetNumberDropdown.text.toString().trim()
                    != selectedNumber
                ) {
                    return@checkExistingMaintenance
                }

                checkingMaintenance = false

                if (alreadyLogged == null) {

                    saveButton.isEnabled = false

                    showToast(
                        "Could not check existing maintenance. Please try again."
                    )

                    return@checkExistingMaintenance
                }

                selectedAssetHasActiveMaintenance =
                    alreadyLogged

                if (alreadyLogged) {

                    assetNumberDropdown.error =
                        "Already logged for maintenance"

                    showToast(
                        "Asset Number $selectedNumber has already been logged for maintenance. Status: $existingStatus"
                    )

                    saveButton.isEnabled = false

                } else {

                    assetNumberDropdown.error = null
                    saveButton.isEnabled = true
                }
            }
        }

        saveButton.setOnClickListener {
            saveMaintenance()
        }
    }

    // ------------------------------------------------
    // LOAD ASSETS FROM FIREBASE REALTIME DATABASE
    // ------------------------------------------------

    private fun loadAssets() {

        database.child("assets")
            .addListenerForSingleValueEvent(
                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        val assetNumbers = mutableListOf<String>()

                        assetNames.clear()

                        for (item in snapshot.children) {

                            val assetNumber = item
                                .child("assetNumber")
                                .getValue(String::class.java)
                                ?.trim()
                                .orEmpty()

                            val assetName = item
                                .child("assetName")
                                .getValue(String::class.java)
                                ?.trim()
                                .orEmpty()

                            if (assetNumber.isNotBlank()) {

                                assetNumbers.add(assetNumber)

                                assetNames[assetNumber] = assetName
                            }
                        }

                        val adapter = ArrayAdapter(
                            this@MaintenanceAdd,
                            android.R.layout.simple_dropdown_item_1line,
                            assetNumbers.distinct()
                        )

                        assetNumberDropdown.setAdapter(adapter)

                        assetNumberDropdown.setOnClickListener {
                            assetNumberDropdown.showDropDown()
                        }

                        if (assetNumbers.isEmpty()) {

                            showToast(
                                "No assets found in the database."
                            )
                        }
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        showToast(
                            "Failed to load assets: ${error.message}"
                        )
                    }
                }
            )
    }

    // ------------------------------------------------
    // CHECK EXISTING MAINTENANCE BY ASSET NUMBER
    // ------------------------------------------------

    private fun checkExistingMaintenance(
        assetNumber: String,
        callback: (Boolean?, String?) -> Unit
    ) {

        if (assetNumber.isBlank()) {
            callback(false, null)
            return
        }

        database.child("maintenance")
            .orderByChild("assetNumber")
            .equalTo(assetNumber)
            .addListenerForSingleValueEvent(
                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        var alreadyLogged = false
                        var existingStatus: String? = null

                        for (record in snapshot.children) {

                            val status = record
                                .child("status")
                                .getValue(String::class.java)
                                ?.trim()

                            // Only Completed maintenance records
                            // allow a new maintenance request.
                            if (!status.equals(
                                    "Completed",
                                    ignoreCase = true
                                )
                            ) {

                                alreadyLogged = true

                                existingStatus =
                                    status?.takeIf { it.isNotBlank() }
                                        ?: "Unknown"

                                break
                            }
                        }

                        callback(
                            alreadyLogged,
                            existingStatus
                        )
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        callback(null, error.message)
                    }
                }
            )
    }

    // ------------------------------------------------
    // LOAD IT STAFF FROM CLOUD FIRESTORE
    // ------------------------------------------------

    private fun loadITStaff() {

        firestore.collection("users")
            .get()
            .addOnSuccessListener { documents ->

                val staffNames = mutableListOf<String>()

                technicianIds.clear()

                for (document in documents) {

                    val role = (
                            document.getString("role")
                                ?: document.getString("userRole")
                                ?: ""
                            ).trim()

                    if (role.equals("IT Staff", ignoreCase = true)) {

                        val name = (
                                document.getString("fullName")
                                    ?: document.getString("name")
                                    ?: document.getString("email")
                                    ?: ""
                                ).trim()

                        if (name.isNotBlank()) {

                            staffNames.add(name)

                            technicianIds[name] = document.id
                        }
                    }
                }

                val adapter = ArrayAdapter(
                    this@MaintenanceAdd,
                    android.R.layout.simple_dropdown_item_1line,
                    staffNames.distinct()
                )

                technicianDropdown.setAdapter(adapter)

                technicianDropdown.setOnClickListener {
                    technicianDropdown.showDropDown()
                }

                if (staffNames.isEmpty()) {

                    showToast(
                        "No IT Staff found. Check the role field in Firestore."
                    )
                }
            }
            .addOnFailureListener { exception ->

                showToast(
                    "Failed to load IT Staff: ${exception.message}"
                )
            }
    }

    // ------------------------------------------------
    // SAVE MAINTENANCE REQUEST
    // ------------------------------------------------

    private fun saveMaintenance() {

        val assetNumber =
            assetNumberDropdown.text.toString().trim()

        val assetName =
            assetNameInput.text.toString().trim()

        val technician =
            technicianDropdown.text.toString().trim()

        val issue =
            issueInput.text.toString().trim()

        val priority =
            priorityDropdown.text.toString().trim()

        val status =
            statusDropdown.text.toString().trim()

        // VALIDATION

        if (assetNumber.isBlank()) {

            assetNumberDropdown.error =
                "Select an asset number"

            return
        }

        if (assetName.isBlank()) {

            assetNameInput.error =
                "Asset name is required"

            return
        }

        if (technician.isBlank()) {

            technicianDropdown.error =
                "Select an IT Staff member"

            return
        }

        if (issue.isBlank()) {

            issueInput.error =
                "Enter the maintenance issue"

            return
        }

        if (priority !in listOf(
                "Low",
                "Medium",
                "High",
                "Critical"
            )
        ) {

            showToast("Please select a valid priority.")

            return
        }

        if (status !in listOf(
                "Pending",
                "In Progress",
                "Completed"
            )
        ) {

            showToast("Please select a valid status.")

            return
        }

        val currentUser = auth.currentUser

        if (currentUser == null) {

            showToast(
                "Please log in before submitting."
            )

            return
        }

        if (checkingMaintenance) {

            showToast(
                "Please wait while the asset maintenance status is checked."
            )

            return
        }

        if (selectedAssetHasActiveMaintenance) {

            showToast(
                "This asset is already logged for maintenance."
            )

            return
        }

        // Prevent multiple submissions.
        saveButton.isEnabled = false

        // CHECK FIREBASE AGAIN BEFORE SAVING.
        // This ensures the latest maintenance status is checked.

        checkExistingMaintenance(assetNumber) {
                alreadyLogged, existingStatus ->

            if (alreadyLogged == null) {

                saveButton.isEnabled = true

                showToast(
                    "Could not verify maintenance: $existingStatus"
                )

                return@checkExistingMaintenance
            }

            if (alreadyLogged) {

                selectedAssetHasActiveMaintenance = true

                saveButton.isEnabled = false

                assetNumberDropdown.error =
                    "Already logged for maintenance"

                showToast(
                    "Asset Number $assetNumber has already been logged for maintenance. Status: $existingStatus"
                )

                return@checkExistingMaintenance
            }

            // Save only if no active maintenance exists.
            saveMaintenanceRecord(
                assetNumber = assetNumber,
                assetName = assetName,
                technician = technician,
                issue = issue,
                priority = priority,
                status = status,
                currentUser = currentUser.uid,
                userEmail = currentUser.email ?: "",
                displayName = currentUser.displayName
            )
        }
    }

    // ------------------------------------------------
    // SAVE MAINTENANCE RECORD TO FIREBASE
    // ------------------------------------------------

    private fun saveMaintenanceRecord(
        assetNumber: String,
        assetName: String,
        technician: String,
        issue: String,
        priority: String,
        status: String,
        currentUser: String,
        userEmail: String,
        displayName: String?
    ) {

        val maintenanceRef =
            database.child("maintenance")

        val maintenanceId =
            maintenanceRef.push().key

        if (maintenanceId.isNullOrBlank()) {

            saveButton.isEnabled = true

            showToast(
                "Could not generate a maintenance ID."
            )

            return
        }

        val maintenanceData = hashMapOf<String, Any>(

            "id" to maintenanceId,

            "maintenanceId" to maintenanceId,

            "assetNumber" to assetNumber,

            "assetName" to assetName,

            "issue" to issue,

            "priority" to priority,

            "status" to status,

            "technician" to technician,

            "technicianId" to (
                    technicianIds[technician] ?: ""
                    ),

            "reportedBy" to (
                    displayName
                        ?: userEmail
                        ?: currentUser
                    ),

            "reportedByEmail" to userEmail,

            "reportedByUID" to currentUser,

            "timestamp" to System.currentTimeMillis(),

            "reportedDate" to SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            ).format(Date())
        )

        maintenanceRef.child(maintenanceId)
            .setValue(maintenanceData)
            .addOnSuccessListener {

                showToast(
                    "Maintenance request saved successfully. Asset Number $assetNumber has been logged for maintenance."
                )

                finish()
            }
            .addOnFailureListener { exception ->

                saveButton.isEnabled = true

                showToast(
                    "Failed to save maintenance: ${exception.message}"
                )
            }
    }

    // ------------------------------------------------
    // TOAST MESSAGE
    // ------------------------------------------------

    private fun showToast(message: String) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
