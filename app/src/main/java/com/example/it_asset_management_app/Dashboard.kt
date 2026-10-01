package com.example.it_asset_management_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.jvm.java

class Dashboard : AppCompatActivity() {
    // =====================================================
    // QUICK ACTIONS
    // =====================================================

    private lateinit var btnAddAsset: View
    private lateinit var btnTransfer: View
    private lateinit var btnExportReport: View
    private lateinit var btnAllocate: View
    private lateinit var btnAssetPersonnel: View
    private lateinit var btnMaintenance: View


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
    // ON CREATE
    // =====================================================


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        // =================================================
        // FIREBASE INITIALIZATION
        // =================================================

        auth = FirebaseAuth.getInstance()

        database =
            FirebaseDatabase.getInstance().reference

        firestore =
            FirebaseFirestore.getInstance()


        // =================================================
        // FIND VIEWS
        // =================================================

        btnAddAsset =
            findViewById(R.id.btnAddAsset)

        btnTransfer =
            findViewById(R.id.btnTransfer)

        btnExportReport =
            findViewById(R.id.btnExportReport)

        btnAllocate =
            findViewById(R.id.btnAllocate)

        btnAssetPersonnel =
            findViewById(R.id.btnAssetPersonnel)

        btnMaintenance =
            findViewById(R.id.btnMaintenance)


        // =================================================
        // DASHBOARD TEXT
        // =================================================

        txtTotalAssets =
            findViewById(R.id.txtTotalAssets)

        txtReports =
            findViewById(R.id.txtReports)

        txtTransfers =
            findViewById(R.id.txtTransfers)

        txtAllocations =
            findViewById(R.id.txtAllocations)

        tvUsername =
            findViewById(R.id.tvUsername)

        txtUserRole =
            findViewById(R.id.txtUserRole)


        // =================================================
        // PROFILE CARD
        // =================================================

        profileCard =
            findViewById(R.id.profileCard)


        // =================================================
        // BUTTON NAVIGATION
        // =================================================


        // -------------------------------------------------
        // ADD ASSET
        // -------------------------------------------------

        btnAddAsset.setOnClickListener {

            val intent =
                Intent(
                    this,
                    AddAsset::class.java
                )

            startActivity(intent)
        }


        // -------------------------------------------------
        // TRANSFER ASSET
        // -------------------------------------------------

        btnTransfer.setOnClickListener {

            val intent =
                Intent(
                    this,
                    TransferAsset::class.java
                )

            startActivity(intent)
        }


        // -------------------------------------------------
        // ALLOCATE ASSET
        // -------------------------------------------------

        btnAllocate.setOnClickListener {

            val intent =
                Intent(
                    this,
                    Allocate::class.java
                )

            startActivity(intent)
        }


        // -------------------------------------------------
        // REPORTS
        // -------------------------------------------------

        btnExportReport.setOnClickListener {

            val intent =
                Intent(
                    this,
                    AssetReports::class.java
                )

            startActivity(intent)
        }


        // -------------------------------------------------
        // ASSET PERSONNEL
        // -------------------------------------------------

        btnAssetPersonnel.setOnClickListener {

            val intent =
                Intent(
                    this,
                    AssetPersonnel::class.java
                )

            startActivity(intent)
        }


        // -------------------------------------------------
        // MAINTENANCE
        // -------------------------------------------------

        btnMaintenance.setOnClickListener {

            val intent =
                Intent(
                    this,
                    MaintenanceAdd::class.java
                )

            startActivity(intent)
        }


        // -------------------------------------------------
        // PROFILE
        // -------------------------------------------------

        profileCard.setOnClickListener {

            val intent =
                Intent(
                    this,
                    Profile::class.java
                )

            startActivity(intent)
        }


        // =================================================
        // LOAD FIREBASE DATA
        // =================================================

        loadUserProfile()

        loadDashboardData()
    }


    // =====================================================
    // LOAD USER PROFILE
    // =====================================================

    private fun loadUserProfile() {

        val currentUser =
            auth.currentUser


        // -------------------------------------------------
        // NO LOGGED-IN USER
        // -------------------------------------------------

        if (currentUser == null) {

            tvUsername.text =
                "User"

            txtUserRole.text =
                "Staff"

            applyRoleBasedAccess(
                "Staff"
            )

            return
        }


        val uid =
            currentUser.uid


        // =================================================
        // GET USER FROM FIRESTORE
        // =================================================

        firestore
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->


                // -------------------------------------------------
                // DOCUMENT EXISTS
                // -------------------------------------------------

                if (document.exists()) {


                    // ---------------------------------------------
                    // USER NAME
                    // ---------------------------------------------

                    val name =
                        document.getString("name")
                            ?: "User"


                    tvUsername.text =
                        name


                    // ---------------------------------------------
                    // USER ROLE
                    // ---------------------------------------------

                    val role =
                        document.getString("role")
                            ?: "Staff"


                    txtUserRole.text =
                        role


                    // ---------------------------------------------
                    // APPLY ROLE
                    // ---------------------------------------------

                    applyRoleBasedAccess(
                        role
                    )


                } else {


                    // ---------------------------------------------
                    // USER DOCUMENT DOES NOT EXIST
                    // ---------------------------------------------

                    tvUsername.text =
                        "User"

                    txtUserRole.text =
                        "Staff"

                    applyRoleBasedAccess(
                        "Staff"
                    )
                }
            }
            .addOnFailureListener {


                // -------------------------------------------------
                // FIRESTORE ERROR
                // -------------------------------------------------

                tvUsername.text =
                    "User"

                txtUserRole.text =
                    "Staff"

                applyRoleBasedAccess(
                    "Staff"
                )
            }
    }


    // =====================================================
    // ROLE-BASED DASHBOARD ACCESS
    // =====================================================

    private fun applyRoleBasedAccess(
        role: String
    ) {


        // =================================================
        // HIDE EVERYTHING FIRST
        // =================================================

        btnAddAsset.visibility =
            View.GONE

        btnTransfer.visibility =
            View.GONE

        btnExportReport.visibility =
            View.GONE

        btnAllocate.visibility =
            View.GONE

        btnAssetPersonnel.visibility =
            View.GONE

        btnMaintenance.visibility =
            View.GONE


        // =================================================
        // NORMALIZE ROLE
        // =================================================

        val userRole =
            role
                .trim()
                .lowercase()


        // =================================================
        // ADMIN
        // =================================================

        if (userRole == "admin") {

            btnAddAsset.visibility =
                View.VISIBLE

            btnTransfer.visibility =
                View.VISIBLE

            btnExportReport.visibility =
                View.VISIBLE

            btnAllocate.visibility =
                View.VISIBLE

            btnAssetPersonnel.visibility =
                View.VISIBLE

            btnMaintenance.visibility =
                View.VISIBLE

            return
        }


        // =================================================
        // IT MANAGER
        // =================================================

        if (userRole == "it manager") {

            btnAddAsset.visibility =
                View.VISIBLE

            btnTransfer.visibility =
                View.VISIBLE

            btnExportReport.visibility =
                View.VISIBLE

            btnAllocate.visibility =
                View.VISIBLE

            btnAssetPersonnel.visibility =
                View.VISIBLE

            btnMaintenance.visibility =
                View.VISIBLE

            return
        }


        // =================================================
        // ASSET PERSONNEL
        // =================================================

        if (userRole == "asset personnel") {

            btnAddAsset.visibility =
                View.VISIBLE

            btnTransfer.visibility =
                View.VISIBLE

            btnExportReport.visibility =
                View.VISIBLE

            btnAllocate.visibility =
                View.VISIBLE

            btnMaintenance.visibility =
                View.VISIBLE

            return
        }


        // =================================================
        // IT STAFF
        // =================================================

        if (userRole == "it staff") {

            btnTransfer.visibility =
                View.VISIBLE

            btnExportReport.visibility =
                View.VISIBLE

            btnAllocate.visibility =
                View.VISIBLE

            btnMaintenance.visibility =
                View.VISIBLE

            return
        }


        // =================================================
        // STAFF
        // =================================================

        // =================================================


        if (userRole == "staff") {
            btnTransfer.visibility = View.VISIBLE
            btnAllocate.visibility = View.GONE
            btnMaintenance.visibility = View.GONE
            return
        }

        // =================================================
        // UNKNOWN ROLE
        // =================================================

        // If the role is unknown, keep all management
        // actions hidden for safety.
    }


    // =====================================================
    // LOAD DASHBOARD DATA
    // =====================================================

    private fun loadDashboardData() {

        loadTotalAssets()

        loadReports()

        loadTransfers()

        loadAllocations()
    }


    // =====================================================
    // TOTAL ASSETS
    // =====================================================

    private fun loadTotalAssets() {

        database
            .child("assets")
            .addValueEventListener(

                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        val count =
                            snapshot.childrenCount


                        txtTotalAssets.text =
                            count.toString()
                    }


                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        txtTotalAssets.text =
                            "0"
                    }
                }
            )
    }


    // =====================================================
    // REPORT EXPORTS
    // =====================================================

    private fun loadReports() {

        database
            .child("reportExports")
            .addValueEventListener(

                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        val exportCount =
                            snapshot.childrenCount


                        txtReports.text =
                            exportCount.toString()
                    }


                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        txtReports.text =
                            "0"
                    }
                }
            )
    }


    // =====================================================
    // TRANSFERS
    // =====================================================

    private fun loadTransfers() {

        database
            .child("transfers")
            .addValueEventListener(

                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        val count =
                            snapshot.childrenCount


                        txtTransfers.text =
                            count.toString()
                    }


                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        txtTransfers.text =
                            "0"
                    }
                }
            )
    }


    // =====================================================
    // ALLOCATIONS
    // =====================================================

    private fun loadAllocations() {

        database
            .child("allocations")
            .addValueEventListener(

                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        val count =
                            snapshot.childrenCount


                        txtAllocations.text =
                            count.toString()
                    }


                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                        txtAllocations.text =
                            "0"
                    }
                }
            )
    }
}