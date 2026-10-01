package com.example.it_asset_management_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Profile : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var btnBack: ImageView
    private lateinit var btnLogout: Button
    private lateinit var btnEditProfile: Button

    private lateinit var txtName: TextView
    private lateinit var txtFullName: TextView
    private lateinit var txtEmail: TextView
    private lateinit var txtDepartment: TextView
    private lateinit var txtRole: TextView
    private lateinit var txtProfileRole: TextView
    private lateinit var txtRegisteredOn: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)
// Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Connect XML views
        btnBack = findViewById(R.id.btnBack)
        btnLogout = findViewById(R.id.btnLogout)
        btnEditProfile = findViewById(R.id.btnEditProfile)

        txtName = findViewById(R.id.txtName)
        txtFullName = findViewById(R.id.txtFullName)
        txtEmail = findViewById(R.id.txtEmail)
        txtDepartment = findViewById(R.id.txtDepartment)
        txtRole = findViewById(R.id.txtRole)
        txtProfileRole = findViewById(R.id.txtProfileRole)
        txtRegisteredOn = findViewById(R.id.txtRegisteredOn)

        // Back
        btnBack.setOnClickListener {
            finish()
        }

        // Load Firebase user information
        loadUserProfile()

        // Logout
        btnLogout.setOnClickListener {

            auth.signOut()

            val intent = Intent(
                this,
                Login::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
        }

        // Edit Profile
        btnEditProfile.setOnClickListener {

            Toast.makeText(
                this,
                "Edit Profile coming soon",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun loadUserProfile() {

        val user = auth.currentUser

        if (user == null) {

            Toast.makeText(
                this,
                "No user is currently logged in",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        // Email comes directly from Firebase Authentication
        txtEmail.text = user.email ?: "No email"

        // Get matching Firestore document
        firestore
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    // Registered name
                    val name =
                        document.getString("name")
                            ?: "User"

                    // Department
                    val department =
                        document.getString("department")
                            ?: "Not specified"

                    // Role
                    val role =
                        document.getString("role")
                            ?: "Staff"

                    // Registered date
                    val registeredOn =
                        document.getString("registeredOn")
                            ?: "Not available"

                    // Display registered name
                    txtName.text = name
                    txtFullName.text = name

                    // Display department
                    txtDepartment.text = department

                    // Display role
                    txtRole.text = role
                    txtProfileRole.text = role

                    // Display registration date
                    txtRegisteredOn.text = registeredOn

                } else {

                    txtName.text = "User"
                    txtFullName.text = "User"
                    txtDepartment.text = "Not specified"
                    txtRole.text = "Staff"
                    txtProfileRole.text = "Staff"
                    txtRegisteredOn.text = "Not available"
                }
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Failed to load profile: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onResume() {
        super.onResume()

        // Reload information whenever Profile becomes visible
        loadUserProfile()
    }
}
