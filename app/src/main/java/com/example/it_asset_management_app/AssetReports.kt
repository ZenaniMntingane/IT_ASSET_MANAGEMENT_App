package com.example.it_asset_management_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.FileOutputStream


class AssetReports : AppCompatActivity() {
    // =====================================================
    // FIREBASE
    // =====================================================

    private lateinit var database: DatabaseReference
    private lateinit var maintenanceDatabase: DatabaseReference
    private lateinit var reportExportsDatabase: DatabaseReference
    private lateinit var auth: FirebaseAuth

    // =====================================================
    // REPORT BUTTONS
    // =====================================================

    private lateinit var reportAllAssets: LinearLayout
    private lateinit var reportStatus: LinearLayout
    private lateinit var reportCategory: LinearLayout
    private lateinit var reportLocation: LinearLayout
    private lateinit var reportMaintenance: LinearLayout
    private lateinit var reportAllocation: LinearLayout

    private lateinit var btnExportReport: Button

    // =====================================================
    // CURRENT REPORT
    // =====================================================

    private var currentReport = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_asset_reports)
        // =====================================================
        // FIREBASE
        // =====================================================

        val firebaseDatabase =
            FirebaseDatabase.getInstance()
        maintenanceDatabase = firebaseDatabase.getReference("maintenance")

        database =
            firebaseDatabase.getReference("assets")

        // This is where export history will be stored
        reportExportsDatabase =
            firebaseDatabase.getReference("reportExports")

        auth =
            FirebaseAuth.getInstance()

        // =====================================================
        // CONNECT XML COMPONENTS
        // =====================================================

        reportAllAssets =
            findViewById(R.id.reportAllAssets)

        reportStatus =
            findViewById(R.id.reportStatus)

        reportCategory =
            findViewById(R.id.reportCategory)

        reportLocation =
            findViewById(R.id.reportLocation)

        reportMaintenance =
            findViewById(R.id.reportMaintenance)

        reportAllocation =
            findViewById(R.id.reportAllocation)

        btnExportReport =
            findViewById(R.id.btnExportReport)

        // =====================================================
        // ALL ASSETS REPORT
        // =====================================================

        reportAllAssets.setOnClickListener {

            generateAllAssetsReport()
        }

        // =====================================================
        // STATUS REPORT
        // =====================================================

        reportStatus.setOnClickListener {

            generateStatusReport()
        }

        // =====================================================
        // CATEGORY REPORT
        // =====================================================

        reportCategory.setOnClickListener {

            generateCategoryReport()
        }

        // =====================================================
        // LOCATION REPORT
        // =====================================================

        reportLocation.setOnClickListener {

            generateLocationReport()
        }

        // =====================================================
        // MAINTENANCE REPORT
        // =====================================================

        reportMaintenance.setOnClickListener {

            generateMaintenanceReport()
        }

        // =====================================================
        // ALLOCATION REPORT
        // =====================================================

        reportAllocation.setOnClickListener {

            generateAllocationReport()
        }

        // =====================================================
        // EXPORT REPORT
        // =====================================================

        btnExportReport.setOnClickListener {

            if (currentReport.isEmpty()) {

                Toast.makeText(
                    this,
                    "Please generate a report first.",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                exportReport(
                    "IT_Asset_Report.txt",
                    currentReport
                )
            }
        }
    }

    // =====================================================
    // 1. ALL ASSETS REPORT
    // =====================================================

    private fun generateAllAssetsReport() {

        database.addListenerForSingleValueEvent(
            object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    val report =
                        StringBuilder()

                    report.append(
                        "IT ASSET MANAGEMENT REPORT\n"
                    )

                    report.append(
                        "============================\n\n"
                    )

                    report.append(
                        "ALL ASSETS REPORT\n"
                    )

                    report.append(
                        "=================\n\n"
                    )

                    report.append(
                        "Total Assets: ${snapshot.childrenCount}\n\n"
                    )

                    for (assetSnapshot in snapshot.children) {

                        val asset =
                            assetSnapshot.getValue(
                                Asset::class.java
                            )

                        if (asset != null) {

                            report.append(
                                "Asset Name: ${asset.assetName}\n"
                            )

                            report.append(
                                "Asset Number: ${asset.assetNumber}\n"
                            )

                            report.append(
                                "Serial Number: ${asset.serialNumber}\n"
                            )

                            report.append(
                                "Asset Type: ${asset.assetType}\n"
                            )

                            report.append(
                                "Condition: ${asset.condition}\n"
                            )

                            report.append(
                                "Location: ${asset.location}\n"
                            )

                            report.append(
                                "Department: ${asset.department}\n"
                            )

                            report.append(
                                "Assigned User: ${asset.assignedUser}\n"
                            )

                            report.append(
                                "Purchase Date: ${asset.purchaseDate}\n"
                            )

                            report.append(
                                "----------------------------\n\n"
                            )
                        }
                    }

                    currentReport =
                        report.toString()

                    showReportCreatedMessage()
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {

                    showDatabaseError(error)
                }
            }
        )
    }

    // =====================================================
    // 2. STATUS REPORT
    // =====================================================

    private fun generateStatusReport() {

        database.addListenerForSingleValueEvent(
            object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    val report =
                        StringBuilder()

                    report.append(
                        "IT ASSET STATUS REPORT\n"
                    )

                    report.append(
                        "======================\n\n"
                    )

                    for (assetSnapshot in snapshot.children) {

                        val asset =
                            assetSnapshot.getValue(
                                Asset::class.java
                            )

                        if (asset != null) {

                            report.append(
                                "Asset Name: ${asset.assetName}\n"
                            )

                            report.append(
                                "Asset Number: ${asset.assetNumber}\n"
                            )

                            report.append(
                                "Condition: ${asset.condition}\n"
                            )

                            report.append(
                                "----------------------------\n"
                            )
                        }
                    }

                    currentReport =
                        report.toString()

                    showReportCreatedMessage()
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {

                    showDatabaseError(error)
                }
            }
        )
    }

    // =====================================================
    // 3. CATEGORY REPORT
    // =====================================================

    private fun generateCategoryReport() {

        database.addListenerForSingleValueEvent(
            object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    val report =
                        StringBuilder()

                    report.append(
                        "IT ASSET CATEGORY REPORT\n"
                    )

                    report.append(
                        "========================\n\n"
                    )

                    for (assetSnapshot in snapshot.children) {

                        val asset =
                            assetSnapshot.getValue(
                                Asset::class.java
                            )

                        if (asset != null) {

                            report.append(
                                "Asset Name: ${asset.assetName}\n"
                            )

                            report.append(
                                "Asset Number: ${asset.assetNumber}\n"
                            )

                            report.append(
                                "Category: ${asset.assetType}\n"
                            )

                            report.append(
                                "----------------------------\n"
                            )
                        }
                    }

                    currentReport =
                        report.toString()

                    showReportCreatedMessage()
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {

                    showDatabaseError(error)
                }
            }
        )
    }

    // =====================================================
    // 4. LOCATION REPORT
    // =====================================================

    private fun generateLocationReport() {

        database.addListenerForSingleValueEvent(
            object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    val report =
                        StringBuilder()

                    report.append(
                        "IT ASSET LOCATION REPORT\n"
                    )

                    report.append(
                        "========================\n\n"
                    )

                    for (assetSnapshot in snapshot.children) {

                        val asset =
                            assetSnapshot.getValue(
                                Asset::class.java
                            )

                        if (asset != null) {

                            report.append(
                                "Asset Name: ${asset.assetName}\n"
                            )

                            report.append(
                                "Asset Number: ${asset.assetNumber}\n"
                            )

                            report.append(
                                "Location: ${asset.location}\n"
                            )

                            report.append(
                                "Department: ${asset.department}\n"
                            )

                            report.append(
                                "----------------------------\n"
                            )
                        }
                    }

                    currentReport =
                        report.toString()

                    showReportCreatedMessage()
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {

                    showDatabaseError(error)
                }
            }
        )
    }

    // =====================================================
// 5. MAINTENANCE REPORT
// =====================================================

    private fun generateMaintenanceReport() {

        maintenanceDatabase.addListenerForSingleValueEvent(
            object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    val report =
                        StringBuilder()

                    report.append(
                        "IT ASSET MAINTENANCE REPORT\n"
                    )

                    report.append(
                        "===========================\n\n"
                    )

                    report.append(
                        "Total Maintenance Records: ${snapshot.childrenCount}\n\n"
                    )

                    if (!snapshot.exists()) {

                        report.append(
                            "No maintenance records found.\n"
                        )

                    } else {

                        for (maintenanceSnapshot in snapshot.children) {

                            report.append(
                                "Maintenance Record\n"
                            )

                            report.append(
                                "----------------------------\n"
                            )

                            report.append(
                                "Asset Name: ${
                                    maintenanceSnapshot
                                        .child("assetName")
                                        .value ?: "N/A"
                                }\n"
                            )

                            report.append(
                                "Asset Number: ${
                                    maintenanceSnapshot
                                        .child("assetNumber")
                                        .value ?: "N/A"
                                }\n"
                            )

                            report.append(
                                "Issue: ${
                                    maintenanceSnapshot
                                        .child("issue")
                                        .value ?: "N/A"
                                }\n"
                            )

                            report.append(
                                "Maintenance Type: ${
                                    maintenanceSnapshot
                                        .child("maintenanceType")
                                        .value ?: "N/A"
                                }\n"
                            )

                            report.append(
                                "Priority: ${
                                    maintenanceSnapshot
                                        .child("priority")
                                        .value ?: "N/A"
                                }\n"
                            )

                            report.append(
                                "Status: ${
                                    maintenanceSnapshot
                                        .child("status")
                                        .value ?: "N/A"
                                }\n"
                            )

                            report.append(
                                "Technician: ${
                                    maintenanceSnapshot
                                        .child("technician")
                                        .value ?: "N/A"
                                }\n"
                            )

                            report.append(
                                "Department: ${
                                    maintenanceSnapshot
                                        .child("department")
                                        .value ?: "N/A"
                                }\n"
                            )

                            report.append(
                                "Location: ${
                                    maintenanceSnapshot
                                        .child("location")
                                        .value ?: "N/A"
                                }\n"
                            )

                            report.append(
                                "Reported Date: ${
                                    maintenanceSnapshot
                                        .child("reportedDate")
                                        .value ?: "N/A"
                                }\n"
                            )

                            report.append(
                                "Estimated Cost: ${
                                    maintenanceSnapshot
                                        .child("estimatedCost")
                                        .value ?: "N/A"
                                }\n"
                            )

                            report.append(
                                "Actual Cost: ${
                                    maintenanceSnapshot
                                        .child("actualCost")
                                        .value ?: "N/A"
                                }\n"
                            )

                            report.append(
                                "\n============================\n\n"
                            )
                        }
                    }

                    currentReport =
                        report.toString()

                    showReportCreatedMessage()
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {

                    showDatabaseError(error)
                }
            }
        )
    }
    // =====================================================
    // 6. ALLOCATION REPORT
    // =====================================================

    private fun generateAllocationReport() {

        database.addListenerForSingleValueEvent(
            object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    val report =
                        StringBuilder()

                    report.append(
                        "IT ASSET ALLOCATION REPORT\n"
                    )

                    report.append(
                        "==========================\n\n"
                    )

                    for (assetSnapshot in snapshot.children) {

                        val asset =
                            assetSnapshot.getValue(
                                Asset::class.java
                            )

                        if (asset != null) {

                            report.append(
                                "Asset Name: ${asset.assetName}\n"
                            )

                            report.append(
                                "Asset Number: ${asset.assetNumber}\n"
                            )

                            report.append(
                                "Serial Number: ${asset.serialNumber}\n"
                            )

                            report.append(
                                "Assigned User: ${asset.assignedUser}\n"
                            )

                            report.append(
                                "Department: ${asset.department}\n"
                            )

                            report.append(
                                "Location: ${asset.location}\n"
                            )

                            report.append(
                                "Condition: ${asset.condition}\n"
                            )

                            report.append(
                                "----------------------------\n\n"
                            )
                        }
                    }

                    currentReport =
                        report.toString()

                    showReportCreatedMessage()
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {

                    showDatabaseError(error)
                }
            }
        )
    }

    // =====================================================
    // EXPORT REPORT
    // =====================================================

    private fun exportReport(
        fileName: String,
        content: String
    ) {

        try {

            // -------------------------------------------------
            // Create report directory
            // -------------------------------------------------

            val reportsDirectory =
                getExternalFilesDir(null)

            if (reportsDirectory == null) {

                Toast.makeText(
                    this,
                    "Unable to access app storage.",
                    Toast.LENGTH_LONG
                ).show()

                return
            }

            if (!reportsDirectory.exists()) {

                reportsDirectory.mkdirs()
            }

            // -------------------------------------------------
            // Create report file
            // -------------------------------------------------

            val file =
                File(
                    reportsDirectory,
                    fileName
                )

            FileOutputStream(file).use { outputStream ->

                outputStream.write(
                    content.toByteArray(
                        Charsets.UTF_8
                    )
                )
            }

            // -------------------------------------------------
            // FILE WAS SUCCESSFULLY CREATED
            // -------------------------------------------------
            // Record export in Firebase
            // -------------------------------------------------

            recordReportExport(fileName)

            // -------------------------------------------------
            // Share report
            // -------------------------------------------------

            shareReport(file)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Export failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =====================================================
    // RECORD REPORT EXPORT
    // =====================================================

    private fun recordReportExport(
        fileName: String
    ) {

        val exportId =
            reportExportsDatabase
                .push()
                .key

        if (exportId == null) {

            Toast.makeText(
                this,
                "Report exported, but export count could not be updated.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val exportData =
            hashMapOf<String, Any>(
                "fileName" to fileName,
                "exportedAt" to System.currentTimeMillis(),
                "exportedBy" to (
                        auth.currentUser?.uid
                            ?: "unknown"
                        )
            )

        reportExportsDatabase
            .child(exportId)
            .setValue(exportData)
            .addOnFailureListener { error ->

                Toast.makeText(
                    this,
                    "Report exported, but Firebase count update failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =====================================================
    // SHARE EXPORTED REPORT
    // =====================================================

    private fun shareReport(
        file: File
    ) {

        try {

            val uri =
                FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    file
                )

            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {

                    type = "text/plain"

                    putExtra(
                        Intent.EXTRA_STREAM,
                        uri
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

            startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Share Asset Report"
                )
            )

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Sharing failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =====================================================
    // SUCCESS MESSAGE
    // =====================================================

    private fun showReportCreatedMessage() {

        Toast.makeText(
            this,
            "Report generated from Firebase successfully.",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =====================================================
    // DATABASE ERROR
    // =====================================================

    private fun showDatabaseError(
        error: DatabaseError
    ) {

        Toast.makeText(
            this,
            "Firebase error: ${error.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}












