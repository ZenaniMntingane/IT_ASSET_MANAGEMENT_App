package com.example.it_asset_management_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale
import kotlin.jvm.java

class Dashboard : AppCompatActivity() {
    // =====================================================
    // QUICK ACTIONS - ALL 8 MODULES
    // =====================================================

    private lateinit var btnAddAsset: View
    private lateinit var btnAuditTrail: View
    private lateinit var btnTransfer: View
    private lateinit var btnExportReport: View
    private lateinit var btnAllocate: View
    private lateinit var btnAssetPersonnel: View
    private lateinit var btnMaintenance: View
    private lateinit var btnAssetHistory: View

    // =====================================================
    // DASHBOARD TEXT
    // =====================================================

    private lateinit var txtTotalAssets: TextView
    private lateinit var txtReports: TextView
    private lateinit var txtTransfers: TextView
    private lateinit var txtAllocations: TextView

    private lateinit var tvUsername: TextView
    private lateinit var txtUserRole: TextView

    // =====================================================
    // PROFILE CARD
    // =====================================================

    private lateinit var profileCard: View

    // =====================================================
    // FIREBASE
    // =====================================================

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var firestore: FirebaseFirestore

    // =====================================================
    // VERIFIED ROLE
    // =====================================================

    private var currentUserRole: String = ""

    private val allowedRoles = setOf(
        "admin",
        "it manager",
        "it staff",
        "asset personnel",
        "staff"
    )

    // =====================================================
    // FIREBASE LISTENERS
    // =====================================================

    private var assetsListener: ValueEventListener? = null
    private var reportsListener: ValueEventListener? = null
    private var allocationsListener: ValueEventListener? = null

    private var transfersListener: ListenerRegistration? = null


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)
        // =================================================
        // FIREBASE INITIALIZATION
        // =================================================

        auth = FirebaseAuth.getInstance()

        database = FirebaseDatabase.getInstance()
            .reference

        firestore = FirebaseFirestore.getInstance()

        // =================================================
        // FIND QUICK ACTION VIEWS
        // =================================================

        btnAddAsset = findViewById(R.id.cardAddAsset)

        btnAuditTrail = findViewById(R.id.cardAuditTrail)

        btnTransfer = findViewById(R.id.cardTransferAsset)

        btnExportReport = findViewById(R.id.cardReports)

        btnAllocate = findViewById(R.id.cardAllocateAsset)

        btnAssetPersonnel = findViewById(R.id.cardAssetPersonnel)

        btnMaintenance = findViewById(R.id.cardMaintenance)

        btnAssetHistory = findViewById(R.id.cardAssetHistory)

        // =================================================
        // DASHBOARD STATISTICS
        // =================================================

        txtTotalAssets = findViewById(R.id.txtTotalAssets)

        txtReports = findViewById(R.id.txtTotalReports)

        txtTransfers = findViewById(R.id.txtTotalTransfers)

        txtAllocations = findViewById(R.id.txtTotalAllocations)

        // =================================================
        // PROFILE
        // =================================================

        tvUsername = findViewById(R.id.txtUserName)

        txtUserRole = findViewById(R.id.txtUserRole)

        profileCard = findViewById(R.id.cardProfile)

        // =================================================
        // HIDE ALL QUICK ACTIONS UNTIL ROLE IS VERIFIED
        // =================================================

        applyRoleBasedAccess("")

        // =================================================
        // CHECK AUTHENTICATION
        // =================================================

        if (auth.currentUser == null) {

            Toast.makeText(
                this,
                "Please sign in to access the dashboard.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        // =================================================
        // NAVIGATION
        // =================================================

        setupNavigation()

        // =================================================
        // LOAD USER PROFILE AND STATISTICS
        // =================================================

        loadUserProfile()

        loadDashboardData()
    }

    // =====================================================
    // QUICK ACTION NAVIGATION
    // =====================================================

    private fun setupNavigation() {

        // -------------------------------------------------
        // 1. ADD ASSET
        // -------------------------------------------------

        btnAddAsset.setOnClickListener {

            if (!hasAccess("add asset")) {
                return@setOnClickListener
            }

            startActivity(
                Intent(this, AddAsset::class.java)
            )
        }

        // -------------------------------------------------
        // 2. TRANSFER ASSET
        // -------------------------------------------------

        btnTransfer.setOnClickListener {

            if (!hasAccess("transfer asset")) {
                return@setOnClickListener
            }

            startActivity(
                Intent(this, TransferAsset::class.java)
            )
        }

        // -------------------------------------------------
        // 3. ALLOCATE ASSET
        // -------------------------------------------------

        btnAllocate.setOnClickListener {

            if (!hasAccess("allocate asset")) {
                return@setOnClickListener
            }

            startActivity(
                Intent(this, Allocate::class.java)
            )
        }

        // -------------------------------------------------
        // 4. ASSET PERSONNEL
        // -------------------------------------------------

        btnAssetPersonnel.setOnClickListener {

            if (!hasAccess("asset personnel management")) {
                return@setOnClickListener
            }

            startActivity(
                Intent(this, AssetPersonnel::class.java)
            )
        }

        // -------------------------------------------------
        // 5. MAINTENANCE
        // -------------------------------------------------

        btnMaintenance.setOnClickListener {

            if (!hasAccess("maintenance")) {
                return@setOnClickListener
            }

            startActivity(
                Intent(this, Maintenance::class.java)
            )
        }

        // -------------------------------------------------
        // 6. REPORTS
        // -------------------------------------------------

        btnExportReport.setOnClickListener {

            if (!hasAccess("reports")) {
                return@setOnClickListener
            }

            startActivity(
                Intent(this, AssetReports::class.java)
            )
        }

        // -------------------------------------------------
        // 7. AUDIT TRAIL
        // -------------------------------------------------

        btnAuditTrail.setOnClickListener {

            if (!hasAccess("audit trail")) {
                return@setOnClickListener
            }

            startActivity(
                Intent(this, AuditTrail::class.java)
            )
        }

        // -------------------------------------------------
        // 8. ASSET HISTORY
        // -------------------------------------------------

        btnAssetHistory.setOnClickListener {

            if (!hasAccess("asset history")) {
                return@setOnClickListener
            }

            startActivity(
                Intent(this, AssetHistory::class.java)
            )
        }

        // -------------------------------------------------
        // PROFILE
        // -------------------------------------------------

        profileCard.setOnClickListener {

            startActivity(
                Intent(this, Profile::class.java)
            )
        }
    }

    // =====================================================
    // LOAD USER PROFILE
    // FIRESTORE: users/{UID}
    // =====================================================

    private fun loadUserProfile() {

        val currentUser = auth.currentUser

        if (currentUser == null) {

            tvUsername.text = "User"
            txtUserRole.text = "Not signed in"

            applyRoleBasedAccess("")

            finish()
            return
        }

        val uid = currentUser.uid

        Log.d(
            "Dashboard",
            "Firebase Authentication UID: $uid"
        )

        // The Firestore document ID is the Firebase UID.
        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {

                    tvUsername.text = "User"
                    txtUserRole.text = "Profile not found"

                    applyRoleBasedAccess("")

                    Log.e(
                        "Dashboard",
                        "No profile found for UID: $uid"
                    )

                    Toast.makeText(
                        this,
                        "User profile not found.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                // -----------------------------------------
                // USER NAME
                // -----------------------------------------

                val name = document.getString("name")
                    ?: document.getString("fullName")
                    ?: "User"

                // -----------------------------------------
                // USER ROLE
                // -----------------------------------------

                val role = document.getString("role")
                    .orEmpty()
                    .trim()

                val normalizedRole = role.lowercase(Locale.ROOT)

                tvUsername.text = name

                txtUserRole.text = role.ifBlank {
                    "Unknown role"
                }

                Log.d(
                    "Dashboard",
                    "User: $name"
                )

                Log.d(
                    "Dashboard",
                    "Role: $role"
                )

                // -----------------------------------------
                // VALIDATE ROLE
                // -----------------------------------------

                if (normalizedRole !in allowedRoles) {

                    currentUserRole = ""

                    applyRoleBasedAccess("")

                    Toast.makeText(
                        this,
                        "Your account has an invalid role.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                // Apply permissions.
                applyRoleBasedAccess(role)
            }
            .addOnFailureListener { error ->

                Log.e(
                    "Dashboard",
                    "Failed to retrieve Firestore user profile",
                    error
                )

                tvUsername.text = "User"

                txtUserRole.text = "Unable to verify role"

                currentUserRole = ""

                applyRoleBasedAccess("")

                Toast.makeText(
                    this,
                    "Unable to verify your permissions.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =====================================================
    // APPLY ROLE-BASED ACCESS CONTROL
    // =====================================================

    private fun applyRoleBasedAccess(role: String) {

        // =================================================
        // HIDE ALL 8 QUICK ACTIONS FIRST
        // =================================================

        btnAddAsset.visibility = View.GONE

        btnTransfer.visibility = View.GONE

        btnAllocate.visibility = View.GONE

        btnAssetPersonnel.visibility = View.GONE

        btnMaintenance.visibility = View.GONE

        btnExportReport.visibility = View.GONE

        btnAuditTrail.visibility = View.GONE

        btnAssetHistory.visibility = View.GONE

        // =================================================
        // NORMALIZE ROLE
        // =================================================

        val userRole = role.trim()
            .lowercase(Locale.ROOT)

        currentUserRole = userRole

        // =================================================
        // ADMIN - ALL 8 MODULES
        // =================================================

        when (userRole) {

            "admin" -> {

                showQuickActions(
                    addAsset = true,
                    transfer = true,
                    allocate = true,
                    assetPersonnel = true,
                    maintenance = true,
                    reports = true,
                    auditTrail = true,
                    assetHistory = true
                )
            }

            // =============================================
            // IT MANAGER
            // ALL EXCEPT AUDIT TRAIL AND ASSET HISTORY
            // =============================================

            "it manager" -> {

                showQuickActions(
                    addAsset = true,
                    transfer = true,
                    allocate = true,
                    assetPersonnel = true,
                    maintenance = true,
                    reports = true,
                    auditTrail = false,
                    assetHistory = false
                )
            }

            // =============================================
            // IT STAFF
            // =============================================

            "it staff" -> {

                showQuickActions(
                    addAsset = true,
                    transfer = true,
                    allocate = false,
                    assetPersonnel = false,
                    maintenance = true,
                    reports = true,
                    auditTrail = false,
                    assetHistory = true
                )
            }

            // =============================================
            // ASSET PERSONNEL
            // =============================================

            "asset personnel" -> {

                showQuickActions(
                    addAsset = true,
                    transfer = true,
                    allocate = true,
                    assetPersonnel = false,
                    maintenance = false,
                    reports = false,
                    auditTrail = true,
                    assetHistory = true
                )
            }

            // =============================================
            // STAFF
            // TRANSFER ASSET ONLY
            // =============================================

            "staff" -> {

                showQuickActions(
                    addAsset = false,
                    transfer = true,
                    allocate = false,
                    assetPersonnel = false,
                    maintenance = false,
                    reports = false,
                    auditTrail = false,
                    assetHistory = false
                )
            }

            // =============================================
            // UNKNOWN ROLE
            // NO QUICK ACTION ACCESS
            // =============================================

            else -> {

                currentUserRole = ""

                Log.e(
                    "Dashboard",
                    "No dashboard permissions for: $role"
                )
            }
        }

        Log.d(
            "Dashboard",
            "RBAC permissions applied: $userRole"
        )
    }

    // =====================================================
    // SHOW/HIDE QUICK ACTIONS
    // =====================================================

    private fun showQuickActions(
        addAsset: Boolean,
        transfer: Boolean,
        allocate: Boolean,
        assetPersonnel: Boolean,
        maintenance: Boolean,
        reports: Boolean,
        auditTrail: Boolean,
        assetHistory: Boolean
    ) {

        btnAddAsset.visibility =
            if (addAsset) View.VISIBLE else View.GONE

        btnTransfer.visibility =
            if (transfer) View.VISIBLE else View.GONE

        btnAllocate.visibility =
            if (allocate) View.VISIBLE else View.GONE

        btnAssetPersonnel.visibility =
            if (assetPersonnel) View.VISIBLE else View.GONE

        btnMaintenance.visibility =
            if (maintenance) View.VISIBLE else View.GONE

        btnExportReport.visibility =
            if (reports) View.VISIBLE else View.GONE

        btnAuditTrail.visibility =
            if (auditTrail) View.VISIBLE else View.GONE

        btnAssetHistory.visibility =
            if (assetHistory) View.VISIBLE else View.GONE
    }

    // =====================================================
    // CHECK USER PERMISSION BEFORE NAVIGATION
    // =====================================================

    private fun hasAccess(feature: String): Boolean {

        // Require a valid signed-in account.
        if (auth.currentUser == null || currentUserRole.isBlank()) {

            Toast.makeText(
                this,
                "Your account permissions have not been verified.",
                Toast.LENGTH_SHORT
            ).show()

            return false
        }

        val permitted = when (currentUserRole) {

            // Admin can access every module.
            "admin" -> true

            // IT Manager.
            "it manager" -> feature in setOf(
                "add asset",
                "transfer asset",
                "allocate asset",
                "asset personnel management",
                "maintenance",
                "reports"
            )

            // IT Staff.
            "it staff" -> feature in setOf(
                "add asset",
                "transfer asset",
                "maintenance",
                "reports",
                "asset history"
            )

            // Asset Personnel.
            "asset personnel" -> feature in setOf(
                "add asset",
                "transfer asset",
                "allocate asset",
                "audit trail",
                "asset history"
            )

            // Staff.
            "staff" -> feature == "transfer asset"

            else -> false
        }

        if (!permitted) {

            Toast.makeText(
                this,
                "Access denied: $feature",
                Toast.LENGTH_SHORT
            ).show()
        }

        return permitted
    }

    // =====================================================
    // LOAD DASHBOARD STATISTICS
    // =====================================================

    private fun loadDashboardData() {

        loadTotalAssets()

        loadReports()

        loadTransfers()

        loadAllocations()
    }

    // =====================================================
    // TOTAL ASSETS
    // REALTIME DATABASE: assets
    // =====================================================

    private fun loadTotalAssets() {

        assetsListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                txtTotalAssets.text =
                    snapshot.childrenCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {

                Log.e(
                    "Dashboard",
                    "Failed to retrieve asset count",
                    error.toException()
                )

                txtTotalAssets.text = "0"
            }
        }

        database.child("assets")
            .addValueEventListener(assetsListener!!)
    }

    // =====================================================
    // REPORT EXPORTS
    // REALTIME DATABASE: reportExports
    // =====================================================

    private fun loadReports() {

        reportsListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                txtReports.text =
                    snapshot.childrenCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {

                Log.e(
                    "Dashboard",
                    "Failed to retrieve report count",
                    error.toException()
                )

                txtReports.text = "0"
            }
        }

        database.child("reportExports")
            .addValueEventListener(reportsListener!!)
    }

    // =====================================================
    // TRANSFERS
    // FIRESTORE: transfers
    // =====================================================

    private fun loadTransfers() {

        transfersListener = firestore
            .collection("transfers")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {

                    Log.e(
                        "Dashboard",
                        "Failed to retrieve transfer count",
                        error
                    )

                    txtTransfers.text = "—"

                    return@addSnapshotListener
                }

                if (snapshot != null) {

                    txtTransfers.text =
                        snapshot.size().toString()
                }
            }
    }

    // =====================================================
    // ALLOCATIONS
    // REALTIME DATABASE: allocations
    // =====================================================

    private fun loadAllocations() {

        allocationsListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                txtAllocations.text =
                    snapshot.childrenCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {

                Log.e(
                    "Dashboard",
                    "Failed to retrieve allocation count",
                    error.toException()
                )

                txtAllocations.text = "0"
            }
        }

        database.child("allocations")
            .addValueEventListener(allocationsListener!!)
    }

    // =====================================================
    // REMOVE FIREBASE LISTENERS
    // =====================================================

    override fun onDestroy() {

        assetsListener?.let {
            database.child("assets").removeEventListener(it)
        }

        reportsListener?.let {
            database.child("reportExports").removeEventListener(it)
        }

        allocationsListener?.let {
            database.child("allocations").removeEventListener(it)
        }

        transfersListener?.remove()

        assetsListener = null
        reportsListener = null
        allocationsListener = null
        transfersListener = null

        super.onDestroy()
    }
}