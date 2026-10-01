package com.example.it_asset_management_app

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class Allocate : AppCompatActivity() {


    // ---------------------------------------------------------
    // Asset fields
    // ---------------------------------------------------------

    private lateinit var etAssetNumber: MaterialAutoCompleteTextView
    private lateinit var etAssetName: EditText
    private lateinit var etSerialNumber: EditText
    private lateinit var etAssetType: EditText

    // ---------------------------------------------------------
    // Employee fields
    // ---------------------------------------------------------

    private lateinit var etAssignedUser: EditText
    private lateinit var etDepartment: EditText

    // ---------------------------------------------------------
    // Allocation fields
    // ---------------------------------------------------------

    private lateinit var etAllocationDate: EditText
    private lateinit var btnAllocateAsset: Button

    // ---------------------------------------------------------
    // Firebase references
    // ---------------------------------------------------------

    private lateinit var assetsDatabase: DatabaseReference
    private lateinit var allocationsDatabase: DatabaseReference

    // ---------------------------------------------------------
    // Store loaded assets
    // ---------------------------------------------------------

    private val assetList = mutableListOf<Asset>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_allocate)

        // =====================================================
        // Firebase
        // =====================================================

        val database = FirebaseDatabase.getInstance()

        assetsDatabase = database.getReference("assets")
        allocationsDatabase = database.getReference("allocations")

        // =====================================================
        // Find views
        // =====================================================

        etAssetNumber = findViewById(R.id.etAssetNumber)
        etAssetName = findViewById(R.id.etAssetName)
        etSerialNumber = findViewById(R.id.etSerialNumber)
        etAssetType = findViewById(R.id.etAssetType)

        etAssignedUser = findViewById(R.id.etAssignedUser)
        etDepartment = findViewById(R.id.etDepartment)

        etAllocationDate = findViewById(R.id.etAllocationDate)

        btnAllocateAsset = findViewById(R.id.btnAllocateAsset)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // =====================================================
        // Back button
        // =====================================================

        btnBack.setOnClickListener {
            finish()
        }

        // =====================================================
        // Load all assets from Firebase
        // =====================================================

        loadAssets()

        // =====================================================
        // Asset number dropdown
        // =====================================================

        etAssetNumber.threshold = 0

        etAssetNumber.setOnClickListener {
            etAssetNumber.showDropDown()
        }

        // =====================================================
        // When an asset is selected
        // =====================================================

        etAssetNumber.setOnItemClickListener { parent, _, position, _ ->

            val selectedAssetNumber =
                parent.getItemAtPosition(position).toString()

            val selectedAsset = assetList.find { asset ->

                asset.assetNumber == selectedAssetNumber
            }

            if (selectedAsset != null) {

                // Fill asset information automatically

                etAssetName.setText(selectedAsset.assetName)

                etSerialNumber.setText(selectedAsset.serialNumber)

                etAssetType.setText(selectedAsset.assetType)
            }
        }

        // =====================================================
        // Allocation date
        // =====================================================

        etAllocationDate.setOnClickListener {

            showDatePicker()
        }

        // =====================================================
        // Allocate button
        // =====================================================

        btnAllocateAsset.setOnClickListener {

            allocateAsset()
        }
    }

    // =========================================================
    // LOAD ASSETS
    // =========================================================

    private fun loadAssets() {

        assetsDatabase.addListenerForSingleValueEvent(

            object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    // Clear previous data

                    assetList.clear()

                    val assetNumbers = mutableListOf<String>()

                    // -----------------------------------------
                    // Read every asset from /assets
                    // -----------------------------------------

                    for (child in snapshot.children) {

                        try {

                            val asset =
                                child.getValue(Asset::class.java)

                            if (asset != null) {

                                // Add asset to list

                                assetList.add(asset)

                                // Add asset number to dropdown

                                if (asset.assetNumber.isNotBlank()) {

                                    assetNumbers.add(
                                        asset.assetNumber
                                    )
                                }
                            }

                        } catch (e: Exception) {

                            Toast.makeText(
                                this@Allocate,
                                "Error reading asset: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    // -----------------------------------------
                    // Remove duplicate asset numbers
                    // -----------------------------------------

                    val uniqueAssetNumbers =
                        assetNumbers.distinct()

                    // -----------------------------------------
                    // Create dropdown adapter
                    // -----------------------------------------

                    val adapter = ArrayAdapter(
                        this@Allocate,
                        android.R.layout.simple_dropdown_item_1line,
                        uniqueAssetNumbers
                    )

                    etAssetNumber.setAdapter(adapter)

                    etAssetNumber.threshold = 0

                    // -----------------------------------------
                    // Show result
                    // -----------------------------------------

                    if (uniqueAssetNumbers.isEmpty()) {

                        Toast.makeText(
                            this@Allocate,
                            "No assets found in Firebase",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        Toast.makeText(
                            this@Allocate,
                            "${uniqueAssetNumbers.size} assets loaded",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                    Toast.makeText(
                        this@Allocate,
                        "Firebase error: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    // =========================================================
    // DATE PICKER
    // =========================================================

    private fun showDatePicker() {

        val calendar = Calendar.getInstance()

        val year =
            calendar.get(Calendar.YEAR)

        val month =
            calendar.get(Calendar.MONTH)

        val day =
            calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(

            this,

            { _, selectedYear, selectedMonth, selectedDay ->

                val formattedDate = String.format(
                    "%04d/%02d/%02d",
                    selectedYear,
                    selectedMonth + 1,
                    selectedDay
                )

                etAllocationDate.setText(
                    formattedDate
                )
            },

            year,
            month,
            day
        )

        datePickerDialog.show()
    }

    // =========================================================
    // ALLOCATE ASSET
    // =========================================================

    private fun allocateAsset() {

        // -----------------------------------------------------
        // Get values from screen
        // -----------------------------------------------------

        val assetNumber =
            etAssetNumber.text.toString().trim()

        val assetName =
            etAssetName.text.toString().trim()

        val serialNumber =
            etSerialNumber.text.toString().trim()

        val assetType =
            etAssetType.text.toString().trim()

        val assignedUser =
            etAssignedUser.text.toString().trim()

        val department =
            etDepartment.text.toString().trim()

        val allocationDate =
            etAllocationDate.text.toString().trim()

        // -----------------------------------------------------
        // Validate asset
        // -----------------------------------------------------

        if (assetNumber.isEmpty()) {

            Toast.makeText(
                this,
                "Please select an asset",
                Toast.LENGTH_SHORT
            ).show()

            etAssetNumber.requestFocus()

            return
        }

        // -----------------------------------------------------
        // Validate assigned user
        // -----------------------------------------------------

        if (assignedUser.isEmpty()) {

            Toast.makeText(
                this,
                "Please enter assigned user",
                Toast.LENGTH_SHORT
            ).show()

            etAssignedUser.requestFocus()

            return
        }

        // -----------------------------------------------------
        // Validate allocation date
        // -----------------------------------------------------

        if (allocationDate.isEmpty()) {

            Toast.makeText(
                this,
                "Please select allocation date",
                Toast.LENGTH_SHORT
            ).show()

            etAllocationDate.requestFocus()

            return
        }

        // -----------------------------------------------------
        // Generate Firebase allocation ID
        // -----------------------------------------------------

        val allocationId =
            allocationsDatabase.push().key

        if (allocationId == null) {

            Toast.makeText(
                this,
                "Could not create allocation ID",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        // -----------------------------------------------------
        // Create allocation object
        // -----------------------------------------------------

        val allocation = Allocation(

            allocationId = allocationId,

            assetNumber = assetNumber,

            assetName = assetName,

            serialNumber = serialNumber,

            assetType = assetType,

            assignedUser = assignedUser,

            department = department,

            allocationDate = allocationDate
        )

        // -----------------------------------------------------
        // Save allocation to Firebase
        // -----------------------------------------------------

        allocationsDatabase
            .child(allocationId)
            .setValue(allocation)

            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Asset allocated successfully",
                    Toast.LENGTH_SHORT
                ).show()

                // Clear fields

                clearFields()

                // Close activity

                finish()
            }

            .addOnFailureListener { error ->

                Toast.makeText(
                    this,
                    "Allocation failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // CLEAR FORM
    // =========================================================

    private fun clearFields() {

        etAssetNumber.setText("")

        etAssetName.setText("")

        etSerialNumber.setText("")

        etAssetType.setText("")

        etAssignedUser.setText("")

        etDepartment.setText("")

        etAllocationDate.setText("")
    }
}


