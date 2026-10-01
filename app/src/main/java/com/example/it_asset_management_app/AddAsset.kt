package com.example.it_asset_management_app

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore


class AddAsset : AppCompatActivity() {

    // EditTexts
    private lateinit var etAssetName: EditText
    private lateinit var etAssetNumber: EditText
    private lateinit var etSerialNumber: EditText
    private lateinit var etLocation: EditText
    private lateinit var etDepartment: EditText
    private lateinit var etPurchaseDate: EditText

    // Spinners
    private lateinit var spAssetType: Spinner
    private lateinit var spCondition: Spinner
    private lateinit var spAssignedUser: Spinner

    // Button
    private lateinit var btnSaveAsset: Button

    // Firebase
    private lateinit var database: DatabaseReference
    private lateinit var firestore: FirebaseFirestore

    // Users loaded from Firestore
    private val users = mutableListOf<User>()
    private val userNames = mutableListOf<String>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_asset)
        // --------------------------------
        // Firebase initialization
        // --------------------------------

        database = FirebaseDatabase
            .getInstance()
            .getReference("assets")

        firestore = FirebaseFirestore
            .getInstance()

        // --------------------------------
        // Find views
        // --------------------------------

        etAssetName = findViewById(R.id.etAssetName)
        etAssetNumber = findViewById(R.id.etAssetNumber)
        etSerialNumber = findViewById(R.id.etSerialNumber)
        etLocation = findViewById(R.id.etLocation)
        etDepartment = findViewById(R.id.etDepartment)
        etPurchaseDate = findViewById(R.id.etPurchaseDate)

        spAssetType = findViewById(R.id.spAssetType)
        spCondition = findViewById(R.id.spCondition)
        spAssignedUser = findViewById(R.id.spAssignedUser)

        btnSaveAsset = findViewById(R.id.btnSaveAsset)
        // --------------------------------
        // Setup dropdowns
        // --------------------------------

        setupAssetTypeSpinner()
        setupConditionSpinner()

        // Load employees from Firestore
        loadUsers()

        // --------------------------------
        // Save button
        // --------------------------------

        btnSaveAsset.setOnClickListener {

            saveAsset()
        }
    }
    // =========================================================
    // ASSET TYPE SPINNER
    // =========================================================

    private fun setupAssetTypeSpinner() {

        val assetTypes = arrayOf(
            "Select asset type",
            "Laptop",
            "Desktop",
            "Monitor",
            "Printer",
            "Keyboard",
            "Mouse",
            "Mobile Phone",
            "Tablet",
            "Server",
            "Network Equipment",
            "Other"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            assetTypes
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spAssetType.adapter = adapter
    }


    // =========================================================
    // CONDITION SPINNER
    // =========================================================

    private fun setupConditionSpinner() {

        val conditions = arrayOf(
            "Select condition",
            "New",
            "Good",
            "Fair",
            "Damaged",
            "Under Maintenance"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            conditions
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spCondition.adapter = adapter
    }


    // =========================================================
    // LOAD USERS FROM FIRESTORE
    // =========================================================

    private fun loadUsers() {

        firestore
            .collection("users")
            .addSnapshotListener { snapshot, error ->

                // Firebase error
                if (error != null) {

                    Toast.makeText(
                        this,
                        "Failed to load employees: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addSnapshotListener
                }

                // No data
                if (snapshot == null) {
                    return@addSnapshotListener
                }

                // Clear old users
                users.clear()
                userNames.clear()

                // Default option
                userNames.add("Select employee")

                // Read every Firestore user document
                for (document in snapshot.documents) {

                    val user = User(
                        id = document.id,
                        name = document.getString("name") ?: "",
                        email = document.getString("email") ?: "",
                        department = document.getString("department") ?: ""
                    )

                    // Only add users with names
                    if (user.name.isNotBlank()) {

                        users.add(user)

                        userNames.add(user.name)
                    }
                }

                // Create spinner adapter
                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    userNames
                )

                adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )

                // Attach adapter to spinner
                spAssignedUser.adapter = adapter

                Toast.makeText(
                    this,
                    "${users.size} employees loaded",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    // =========================================================
    // SAVE ASSET
    // =========================================================

    private fun saveAsset() {

        // --------------------------------
        // Get text values
        // --------------------------------

        val assetName =
            etAssetName.text.toString().trim()

        val assetNumber =
            etAssetNumber.text.toString().trim()

        val serialNumber =
            etSerialNumber.text.toString().trim()

        val location =
            etLocation.text.toString().trim()

        val department =
            etDepartment.text.toString().trim()

        val purchaseDate =
            etPurchaseDate.text.toString().trim()

        // --------------------------------
        // Get spinner values
        // --------------------------------

        val assetType =
            spAssetType.selectedItem.toString()

        val condition =
            spCondition.selectedItem.toString()

        // --------------------------------
        // Validation
        // --------------------------------

        if (assetName.isEmpty()) {

            etAssetName.error = "Enter asset name"
            etAssetName.requestFocus()
            return
        }

        if (assetNumber.isEmpty()) {

            etAssetNumber.error = "Enter asset number"
            etAssetNumber.requestFocus()
            return
        }

        if (serialNumber.isEmpty()) {

            etSerialNumber.error = "Enter serial number"
            etSerialNumber.requestFocus()
            return
        }

        if (assetType == "Select asset type") {

            Toast.makeText(
                this,
                "Please select an asset type",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (condition == "Select condition") {

            Toast.makeText(
                this,
                "Please select a condition",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // --------------------------------
        // Get selected employee
        // --------------------------------

        val selectedPosition =
            spAssignedUser.selectedItemPosition

        if (selectedPosition <= 0) {

            Toast.makeText(
                this,
                "Please select an employee",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Position 0 is "Select employee"
        val selectedUser =
            users[selectedPosition - 1]

        val assignedUserId =
            selectedUser.id

        val assignedUserName =
            selectedUser.name

        // --------------------------------
        // Generate asset ID
        // --------------------------------

        val assetId =
            database.push().key

        if (assetId == null) {

            Toast.makeText(
                this,
                "Could not generate asset ID",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // --------------------------------
        // Create asset
        // --------------------------------

        val asset = HashMap<String, Any>()

        asset["assetId"] = assetId
        asset["assetName"] = assetName
        asset["assetNumber"] = assetNumber
        asset["serialNumber"] = serialNumber
        asset["assetType"] = assetType
        asset["condition"] = condition
        asset["location"] = location
        asset["department"] = department
        asset["assignedUser"] = assignedUserId
        asset["assignedUserName"] = assignedUserName
        asset["purchaseDate"] = purchaseDate
        asset["createdAt"] = System.currentTimeMillis()

        // --------------------------------
        // Save to Realtime Database
        // --------------------------------

        database
            .child(assetId)
            .setValue(asset)
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Asset saved successfully",
                    Toast.LENGTH_SHORT
                ).show()

                clearFields()
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Failed to save asset: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =========================================================
    // CLEAR FORM
    // =========================================================

    private fun clearFields() {

        etAssetName.text.clear()
        etAssetNumber.text.clear()
        etSerialNumber.text.clear()
        etLocation.text.clear()
        etDepartment.text.clear()
        etPurchaseDate.text.clear()

        spAssetType.setSelection(0)
        spCondition.setSelection(0)
        spAssignedUser.setSelection(0)
    }
}