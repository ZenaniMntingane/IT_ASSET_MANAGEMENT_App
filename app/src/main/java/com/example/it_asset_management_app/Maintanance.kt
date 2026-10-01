package com.example.it_asset_management_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Maintanance : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase

    // Summary
    private lateinit var txtTotalMaintenance: TextView
    private lateinit var txtPendingMaintenance: TextView
    private lateinit var txtInProgressMaintenance: TextView
    private lateinit var txtCompletedMaintenance: TextView

    // Records
    private lateinit var maintenanceRecordCard1: MaterialCardView
    private lateinit var maintenanceRecordCard2: MaterialCardView

    private lateinit var txtAssetName1: TextView
    private lateinit var txtAssetNumber1: TextView
    private lateinit var txtIssue1: TextView
    private lateinit var txtStatus1: TextView

    private lateinit var txtAssetName2: TextView
    private lateinit var txtAssetNumber2: TextView
    private lateinit var txtIssue2: TextView
    private lateinit var txtStatus2: TextView

    private lateinit var btnAddMaintenance: MaterialButton
    private lateinit var btnBack: View


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_maintanance)

        database = FirebaseDatabase.getInstance()

        // -----------------------------------------------------
        // FIND VIEWS
        // -----------------------------------------------------

        txtTotalMaintenance =
            findViewById(R.id.txtTotalMaintenance)

        txtPendingMaintenance =
            findViewById(R.id.txtPendingMaintenance)

        txtInProgressMaintenance =
            findViewById(R.id.txtInProgressMaintenance)

        txtCompletedMaintenance =
            findViewById(R.id.txtCompletedMaintenance)

        maintenanceRecordCard1 =
            findViewById(R.id.maintenanceRecordCard1)

        maintenanceRecordCard2 =
            findViewById(R.id.maintenanceRecordCard2)

        txtAssetName1 =
            findViewById(R.id.txtAssetName1)

        txtAssetNumber1 =
            findViewById(R.id.txtAssetNumber1)

        txtIssue1 =
            findViewById(R.id.txtIssue1)

        txtStatus1 =
            findViewById(R.id.txtStatus1)

        txtAssetName2 =
            findViewById(R.id.txtAssetName2)

        txtAssetNumber2 =
            findViewById(R.id.txtAssetNumber2)

        txtIssue2 =
            findViewById(R.id.txtIssue2)

        txtStatus2 =
            findViewById(R.id.txtStatus2)

        btnAddMaintenance =
            findViewById(R.id.btnAddMaintenance)

        btnBack =
            findViewById(R.id.btnBack)

        // -----------------------------------------------------
        // BACK
        // -----------------------------------------------------

        btnBack.setOnClickListener {
            finish()
        }

        // -----------------------------------------------------
        // ADD MAINTENANCE
        // -----------------------------------------------------

        btnAddMaintenance.setOnClickListener {

            val intent = Intent(
                this,
                MaintenanceAdd::class.java
            )

            startActivity(intent)
        }

        // -----------------------------------------------------
        // LOAD MAINTENANCE
        // -----------------------------------------------------

        loadMaintenance()
    }

    // =========================================================
    // LOAD MAINTENANCE DATA
    // =========================================================

    private fun loadMaintenance() {

        database
            .getReference("maintenance")
            .addValueEventListener(

                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        var total = 0
                        var pending = 0
                        var inProgress = 0
                        var completed = 0

                        val records =
                            mutableListOf<DataSnapshot>()

                        for (record in snapshot.children) {

                            total++

                            val status =
                                record.child("status")
                                    .getValue(String::class.java)
                                    ?.trim()
                                    ?.lowercase()

                            when (status) {

                                "pending" -> {
                                    pending++
                                }

                                "in progress",
                                "in_progress" -> {
                                    inProgress++
                                }

                                "completed" -> {
                                    completed++
                                }
                            }

                            records.add(record)
                        }

                        // -------------------------------------------------
                        // UPDATE SUMMARY
                        // -------------------------------------------------

                        txtTotalMaintenance.text =
                            total.toString()

                        txtPendingMaintenance.text =
                            pending.toString()

                        txtInProgressMaintenance.text =
                            inProgress.toString()

                        txtCompletedMaintenance.text =
                            completed.toString()

                        // -------------------------------------------------
                        // DISPLAY RECORDS
                        // -------------------------------------------------

                        displayRecord(
                            records,
                            0,
                            maintenanceRecordCard1,
                            txtAssetName1,
                            txtAssetNumber1,
                            txtIssue1,
                            txtStatus1
                        )

                        displayRecord(
                            records,
                            1,
                            maintenanceRecordCard2,
                            txtAssetName2,
                            txtAssetNumber2,
                            txtIssue2,
                            txtStatus2
                        )
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {
                        // Firebase read cancelled
                    }
                }
            )
    }

    // =========================================================
    // DISPLAY MAINTENANCE RECORD
    // =========================================================

    private fun displayRecord(
        records: List<DataSnapshot>,
        position: Int,
        card: MaterialCardView,
        assetName: TextView,
        assetNumber: TextView,
        issue: TextView,
        status: TextView
    ) {

        if (position >= records.size) {

            card.visibility = View.GONE

            return
        }

        card.visibility = View.VISIBLE

        val record =
            records[position]

        val name =
            record.child("assetName")
                .getValue(String::class.java)

        val number =
            record.child("assetNumber")
                .getValue(String::class.java)

        val problem =
            record.child("issue")
                .getValue(String::class.java)

        val maintenanceStatus =
            record.child("status")
                .getValue(String::class.java)

        val priority =
            record.child("priority")
                .getValue(String::class.java)

        assetName.text =
            name ?: "Unknown Asset"

        assetNumber.text =
            number ?: "No Asset Number"

        issue.text =
            problem ?: "No issue recorded"

        status.text =
            "${priority ?: "Normal"}  •  ${maintenanceStatus ?: "Pending"}"
    }
}