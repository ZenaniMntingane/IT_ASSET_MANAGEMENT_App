package com.example.it_asset_management_app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class AssetReports : AppCompatActivity() {
// ==========================================
    // FIREBASE
    // ==========================================

    private lateinit var database: DatabaseReference
    private lateinit var maintenanceDatabase: DatabaseReference
    private lateinit var reportExportsDatabase: DatabaseReference
    private lateinit var auth: FirebaseAuth

    // ==========================================
    // REPORT CARDS
    // ==========================================

    private lateinit var reportAllAssets: CardView
    private lateinit var reportStatus: CardView
    private lateinit var reportCategory: CardView
    private lateinit var reportLocation: CardView
    private lateinit var reportMaintenance: CardView
    private lateinit var reportAllocation: CardView

    // ==========================================
    // REPORT DATA
    // ==========================================

    private var currentReport = ""
    private var currentReportTitle = ""

    // ==========================================
    // ON CREATE
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_asset_reports)

        initializeFirebase()
        initializeViews()
        setupReportListeners()
    }

    // ==========================================
    // INITIALIZE FIREBASE
    // ==========================================

    private fun initializeFirebase() {

        val firebase = FirebaseDatabase.getInstance()

        database = firebase.getReference("assets")

        maintenanceDatabase =
            firebase.getReference("maintenance")

        reportExportsDatabase =
            firebase.getReference("reportExports")

        auth = FirebaseAuth.getInstance()
    }

    // ==========================================
    // INITIALIZE VIEWS
    // ==========================================

    private fun initializeViews() {

        reportAllAssets = findViewById(R.id.reportAllAssets)
        reportStatus = findViewById(R.id.reportStatus)
        reportCategory = findViewById(R.id.reportCategory)
        reportLocation = findViewById(R.id.reportLocation)
        reportMaintenance = findViewById(R.id.reportMaintenance)
        reportAllocation = findViewById(R.id.reportAllocation)
    }

    // ==========================================
    // REPORT CLICK LISTENERS
    // ==========================================

    private fun setupReportListeners() {

        reportAllAssets.setOnClickListener {
            generateAllAssetsReport()
        }

        reportStatus.setOnClickListener {
            generateStatusReport()
        }

        reportCategory.setOnClickListener {
            generateCategoryReport()
        }

        reportLocation.setOnClickListener {
            generateLocationReport()
        }

        reportMaintenance.setOnClickListener {
            generateMaintenanceReport()
        }

        reportAllocation.setOnClickListener {
            generateAllocationReport()
        }
    }

    // ==========================================
    // 1. ALL ASSETS REPORT
    // ==========================================

    private fun generateAllAssetsReport() {

        database.addListenerForSingleValueEvent(
            object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val report = StringBuilder()

                    report.append("IT ASSET MANAGEMENT SYSTEM\n")
                    report.append("============================\n\n")
                    report.append("ALL ASSETS REPORT\n")
                    report.append("============================\n\n")

                    report.append(
                        "Total Assets: ${snapshot.childrenCount}\n\n"
                    )

                    if (!snapshot.exists()) {
                        report.append("No assets found.\n")
                    }

                    for (item in snapshot.children) {

                        val asset = item.getValue(Asset::class.java)

                        if (asset != null) {

                            report.append("ASSET DETAILS\n")
                            report.append("----------------------------\n")

                            report.append("Asset Name: ${asset.assetName}\n")
                            report.append("Asset Number: ${asset.assetNumber}\n")
                            report.append("Serial Number: ${asset.serialNumber}\n")
                            report.append("Asset Type: ${asset.assetType}\n")
                            report.append("Condition: ${asset.condition}\n")
                            report.append("Location: ${asset.location}\n")
                            report.append("Department: ${asset.department}\n")
                            report.append("Assigned User: ${asset.assignedUser}\n")
                            report.append("Purchase Date: ${asset.purchaseDate}\n")

                            report.append("\n============================\n\n")
                        }
                    }

                    displayReport(
                        "All Assets Report",
                        report.toString()
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    showError(error)
                }
            }
        )
    }

    // ==========================================
    // 2. ASSET STATUS REPORT
    // ==========================================

    private fun generateStatusReport() {

        database.addListenerForSingleValueEvent(
            object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val report = StringBuilder()

                    report.append("IT ASSET STATUS REPORT\n")
                    report.append("============================\n\n")

                    report.append(
                        "Total Assets: ${snapshot.childrenCount}\n\n"
                    )

                    val conditions = mutableMapOf<String, Int>()

                    for (item in snapshot.children) {

                        val asset = item.getValue(Asset::class.java)

                        if (asset != null) {

                            val condition = asset.condition.ifBlank {
                                "Unknown"
                            }

                            conditions[condition] =
                                (conditions[condition] ?: 0) + 1
                        }
                    }

                    report.append("STATUS SUMMARY\n")
                    report.append("----------------------------\n")

                    for ((condition, count) in conditions) {
                        report.append("$condition: $count assets\n")
                    }

                    report.append("\nASSET DETAILS\n")
                    report.append("============================\n\n")

                    for (item in snapshot.children) {

                        val asset = item.getValue(Asset::class.java)

                        if (asset != null) {

                            report.append("Asset Name: ${asset.assetName}\n")
                            report.append("Asset Number: ${asset.assetNumber}\n")
                            report.append("Condition: ${asset.condition}\n")
                            report.append("Location: ${asset.location}\n")

                            report.append("----------------------------\n\n")
                        }
                    }

                    displayReport(
                        "Asset Status Report",
                        report.toString()
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    showError(error)
                }
            }
        )
    }

    // ==========================================
    // 3. CATEGORY REPORT
    // ==========================================

    private fun generateCategoryReport() {

        database.addListenerForSingleValueEvent(
            object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val report = StringBuilder()

                    report.append("IT ASSET CATEGORY REPORT\n")
                    report.append("============================\n\n")

                    val categories = mutableMapOf<String, Int>()

                    for (item in snapshot.children) {

                        val asset = item.getValue(Asset::class.java)

                        if (asset != null) {

                            val category = asset.assetType.ifBlank {
                                "Uncategorized"
                            }

                            categories[category] =
                                (categories[category] ?: 0) + 1
                        }
                    }

                    report.append("CATEGORY SUMMARY\n")
                    report.append("----------------------------\n")

                    for ((category, count) in categories) {
                        report.append("$category: $count assets\n")
                    }

                    report.append("\nCATEGORY DETAILS\n")
                    report.append("============================\n\n")

                    for (item in snapshot.children) {

                        val asset = item.getValue(Asset::class.java)

                        if (asset != null) {

                            report.append("Asset Name: ${asset.assetName}\n")
                            report.append("Asset Number: ${asset.assetNumber}\n")
                            report.append("Category: ${asset.assetType}\n")

                            report.append("----------------------------\n\n")
                        }
                    }

                    displayReport(
                        "Category Report",
                        report.toString()
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    showError(error)
                }
            }
        )
    }

    // ==========================================
    // 4. LOCATION REPORT
    // ==========================================

    private fun generateLocationReport() {

        database.addListenerForSingleValueEvent(
            object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val report = StringBuilder()

                    report.append("IT ASSET LOCATION REPORT\n")
                    report.append("============================\n\n")

                    val locations = mutableMapOf<String, Int>()

                    for (item in snapshot.children) {

                        val asset = item.getValue(Asset::class.java)

                        if (asset != null) {

                            val location = asset.location.ifBlank {
                                "Unknown Location"
                            }

                            locations[location] =
                                (locations[location] ?: 0) + 1
                        }
                    }

                    report.append("LOCATION SUMMARY\n")
                    report.append("----------------------------\n")

                    for ((location, count) in locations) {
                        report.append("$location: $count assets\n")
                    }

                    report.append("\nLOCATION DETAILS\n")
                    report.append("============================\n\n")

                    for (item in snapshot.children) {

                        val asset = item.getValue(Asset::class.java)

                        if (asset != null) {

                            report.append("Asset Name: ${asset.assetName}\n")
                            report.append("Asset Number: ${asset.assetNumber}\n")
                            report.append("Location: ${asset.location}\n")
                            report.append("Department: ${asset.department}\n")

                            report.append("----------------------------\n\n")
                        }
                    }

                    displayReport(
                        "Location Report",
                        report.toString()
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    showError(error)
                }
            }
        )
    }

    // ==========================================
    // 5. MAINTENANCE REPORT
    // ==========================================

    private fun generateMaintenanceReport() {

        maintenanceDatabase.addListenerForSingleValueEvent(
            object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val report = StringBuilder()

                    report.append("IT ASSET MANAGEMENT SYSTEM\n")
                    report.append("============================\n\n")

                    report.append("MAINTENANCE REPORT\n")
                    report.append("============================\n\n")

                    report.append(
                        "Total Maintenance Records: ${snapshot.childrenCount}\n\n"
                    )

                    val statuses = mutableMapOf<String, Int>()

                    for (record in snapshot.children) {

                        val status = record.child("status")
                            .getValue(String::class.java)
                            ?.trim()
                            .orEmpty()
                            .ifBlank { "Unknown" }

                        statuses[status] =
                            (statuses[status] ?: 0) + 1
                    }

                    report.append("MAINTENANCE STATUS SUMMARY\n")
                    report.append("----------------------------\n")

                    for ((status, count) in statuses) {
                        report.append("$status: $count records\n")
                    }

                    report.append("\nMAINTENANCE DETAILS\n")
                    report.append("============================\n\n")

                    if (!snapshot.exists()) {
                        report.append("No maintenance records found.\n")
                    }

                    var count = 1

                    for (record in snapshot.children) {

                        val id =
                            record.child("id")
                                .getValue(String::class.java)
                                ?: record.key
                                ?: "N/A"

                        val assetName =
                            record.child("assetName")
                                .getValue(String::class.java)
                                ?: "N/A"

                        val assetNumber =
                            record.child("assetNumber")
                                .getValue(String::class.java)
                                ?: "N/A"

                        val issue =
                            record.child("issue")
                                .getValue(String::class.java)
                                ?: "N/A"

                        val priority =
                            record.child("priority")
                                .getValue(String::class.java)
                                ?: "N/A"

                        val status =
                            record.child("status")
                                .getValue(String::class.java)
                                ?: "N/A"

                        val technician =
                            record.child("technician")
                                .getValue(String::class.java)
                                ?: "N/A"

                        val timestamp =
                            record.child("timestamp")
                                .getValue(Long::class.java)

                        report.append("MAINTENANCE RECORD #$count\n")
                        report.append("----------------------------\n")

                        report.append("Record ID: $id\n")
                        report.append("Asset Name: $assetName\n")
                        report.append("Asset Number: $assetNumber\n")
                        report.append("Issue: $issue\n")
                        report.append("Technician: $technician\n")
                        report.append("Priority: $priority\n")
                        report.append("Status: $status\n")

                        if (timestamp != null) {

                            val date = SimpleDateFormat(
                                "dd MMM yyyy HH:mm",
                                Locale.getDefault()
                            ).format(Date(timestamp))

                            report.append("Reported Date: $date\n")

                        } else {

                            report.append("Reported Date: N/A\n")
                        }

                        report.append("\n============================\n\n")

                        count++
                    }

                    displayReport(
                        "Maintenance Report",
                        report.toString()
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    showError(error)
                }
            }
        )
    }

    // ==========================================
    // 6. ALLOCATION REPORT
    // ==========================================

    private fun generateAllocationReport() {

        database.addListenerForSingleValueEvent(
            object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val report = StringBuilder()

                    report.append("IT ASSET ALLOCATION REPORT\n")
                    report.append("============================\n\n")

                    var allocated = 0
                    var unallocated = 0

                    for (item in snapshot.children) {

                        val asset = item.getValue(Asset::class.java)

                        if (asset != null) {

                            val user = asset.assignedUser.trim()

                            if (
                                user.isNotBlank() &&
                                !user.equals("Not Assigned", true) &&
                                !user.equals("Unassigned", true)
                            ) {
                                allocated++
                            } else {
                                unallocated++
                            }
                        }
                    }

                    report.append(
                        "Total Assets: ${snapshot.childrenCount}\n"
                    )

                    report.append(
                        "Allocated Assets: $allocated\n"
                    )

                    report.append(
                        "Unallocated Assets: $unallocated\n\n"
                    )

                    report.append("ALLOCATION DETAILS\n")
                    report.append("============================\n\n")

                    for (item in snapshot.children) {

                        val asset = item.getValue(Asset::class.java)

                        if (asset != null) {

                            report.append("Asset Name: ${asset.assetName}\n")
                            report.append("Asset Number: ${asset.assetNumber}\n")
                            report.append("Serial Number: ${asset.serialNumber}\n")
                            report.append("Assigned User: ${asset.assignedUser}\n")
                            report.append("Department: ${asset.department}\n")
                            report.append("Location: ${asset.location}\n")
                            report.append("Condition: ${asset.condition}\n")

                            report.append("----------------------------\n\n")
                        }
                    }

                    displayReport(
                        "Allocation Report",
                        report.toString()
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    showError(error)
                }
            }
        )
    }

    // ==========================================
    // DISPLAY REPORT PREVIEW
    // ==========================================

    private fun displayReport(
        title: String,
        content: String
    ) {

        currentReportTitle = title
        currentReport = content

        val scrollView = ScrollView(this)

        val textView = TextView(this).apply {

            text = content
            textSize = 14f

            setTextColor(
                android.graphics.Color.rgb(35, 45, 65)
            )

            setTextIsSelectable(true)

            setPadding(
                20.dp(),
                20.dp(),
                20.dp(),
                20.dp()
            )

            setLineSpacing(5f, 1f)
        }

        scrollView.addView(
            textView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(scrollView)
            .setPositiveButton("Export Report") { _, _ ->
                exportReport(content)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    // ==========================================
    // EXPORT REPORT
    // ==========================================

    private fun exportReport(content: String) {

        try {

            val directory = getExternalFilesDir(null)

            if (directory == null) {

                Toast.makeText(
                    this,
                    "Unable to access storage.",
                    Toast.LENGTH_LONG
                ).show()

                return
            }

            if (!directory.exists()) {
                directory.mkdirs()
            }

            val safeTitle = currentReportTitle
                .replace(" ", "_")
                .replace(Regex("[^A-Za-z0-9_-]"), "")

            val fileName = "${safeTitle}.txt"

            val file = File(directory, fileName)

            FileOutputStream(file).use { output ->

                output.write(
                    content.toByteArray(Charsets.UTF_8)
                )
            }

            recordReportExport(fileName)

            shareReport(file)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Export failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ==========================================
    // SAVE EXPORT RECORD IN FIREBASE
    // ==========================================

    private fun recordReportExport(fileName: String) {

        val exportId = reportExportsDatabase.push().key

        if (exportId == null) return

        val exportData = hashMapOf<String, Any>(
            "fileName" to fileName,
            "reportType" to currentReportTitle,
            "exportedAt" to System.currentTimeMillis(),
            "exportedBy" to (auth.currentUser?.uid ?: "unknown")
        )

        reportExportsDatabase
            .child(exportId)
            .setValue(exportData)
            .addOnFailureListener { error ->

                Toast.makeText(
                    this,
                    "Could not record export: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ==========================================
    // SHARE REPORT
    // ==========================================

    private fun shareReport(file: File) {

        try {

            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {

                type = "text/plain"

                putExtra(Intent.EXTRA_STREAM, uri)

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            startActivity(
                Intent.createChooser(
                    intent,
                    "Share $currentReportTitle"
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

    // ==========================================
    // DATABASE ERROR
    // ==========================================

    private fun showError(error: DatabaseError) {

        Toast.makeText(
            this,
            "Firebase error: ${error.message}",
            Toast.LENGTH_LONG
        ).show()
    }

    // ==========================================
    // DP CONVERSION
    // ==========================================

    private fun Int.dp(): Int {

        return (
                this * resources.displayMetrics.density
                ).toInt()
    }
}