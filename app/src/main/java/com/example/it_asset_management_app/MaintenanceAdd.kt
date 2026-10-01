package com.example.it_asset_management_app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MaintenanceAdd : AppCompatActivity() {

    private lateinit var etAssetNumber: MaterialAutoCompleteTextView
    private lateinit var etAssetName: EditText
    private lateinit var etIssue: EditText
    private lateinit var spPriority: MaterialAutoCompleteTextView
    private lateinit var spStatus: MaterialAutoCompleteTextView
    private lateinit var btnSaveMaintenance: MaterialButton
    private lateinit var btnBack: ImageButton

    private lateinit var assetsDatabase: DatabaseReference
    private lateinit var maintenanceDatabase: DatabaseReference

    private val assetList = mutableListOf<Asset>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_maintenance_add)

        val database = FirebaseDatabase.getInstance()
        assetsDatabase = database.getReference("assets")
        maintenanceDatabase = database.getReference("maintenance")

        etAssetNumber = findViewById(R.id.etAssetNumber)
        etAssetName = findViewById(R.id.etAssetName)
        etIssue = findViewById(R.id.etIssue)
        spPriority = findViewById(R.id.spPriority)
        spStatus = findViewById(R.id.spStatus)
        btnSaveMaintenance = findViewById(R.id.btnSaveMaintenance)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        setupDropdowns()
        loadAssets()

        btnSaveMaintenance.setOnClickListener {
            saveMaintenance()
        }
    }

    private fun setupDropdowns() {
        val priorities = arrayOf("Normal", "Low", "High", "Urgent")
        val priorityAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, priorities)
        spPriority.setAdapter(priorityAdapter)
        spPriority.setText(priorities[0], false)

        val statuses = arrayOf("Pending", "In Progress", "Completed")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statuses)
        spStatus.setAdapter(statusAdapter)
        spStatus.setText(statuses[0], false)
    }

    private fun loadAssets() {
        assetsDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                assetList.clear()
                val assetNumbers = mutableListOf<String>()

                for (child in snapshot.children) {
                    try {
                        val asset = child.getValue(Asset::class.java)
                        if (asset != null) {
                            assetList.add(asset)
                            if (asset.assetNumber.isNotBlank()) {
                                assetNumbers.add(asset.assetNumber)
                            }
                        }
                    } catch (_: Exception) {
                    }
                }

                val adapter = ArrayAdapter(
                    this@MaintenanceAdd,
                    android.R.layout.simple_dropdown_item_1line,
                    assetNumbers
                )
                etAssetNumber.setAdapter(adapter)
                etAssetNumber.threshold = 1

                etAssetNumber.setOnItemClickListener { parent, _, position, _ ->
                    val selectedAssetNumber = parent.getItemAtPosition(position).toString()
                    val selectedAsset = assetList.find { it.assetNumber == selectedAssetNumber }
                    if (selectedAsset != null) {
                        etAssetName.setText(selectedAsset.assetName)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun saveMaintenance() {
        val assetNumber = etAssetNumber.text.toString().trim()
        val assetName = etAssetName.text.toString().trim()
        val issue = etIssue.text.toString().trim()
        val priority = spPriority.text.toString().trim()
        val status = spStatus.text.toString().trim()

        if (assetNumber.isEmpty() && assetName.isEmpty()) {
            Toast.makeText(this, "Please enter asset number or name", Toast.LENGTH_SHORT).show()
            return
        }

        if (issue.isEmpty()) {
            Toast.makeText(this, "Please describe the issue", Toast.LENGTH_SHORT).show()
            return
        }

        val key = maintenanceDatabase.push().key
        if (key == null) {
            Toast.makeText(this, "Failed to generate record key", Toast.LENGTH_SHORT).show()
            return
        }

        val record = hashMapOf(
            "id" to key,
            "assetNumber" to assetNumber,
            "assetName" to assetName,
            "issue" to issue,
            "priority" to priority,
            "status" to status,
            "timestamp" to System.currentTimeMillis()
        )

        maintenanceDatabase.child(key).setValue(record)
            .addOnSuccessListener {
                Toast.makeText(this, "Maintenance record saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Failed to save: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
