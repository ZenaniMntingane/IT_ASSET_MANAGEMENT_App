package com.example.it_asset_management_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.jvm.java

class Login : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Find views
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        registerButton = findViewById(R.id.tvRegister)

        val forgotPassword =
            findViewById<TextView>(R.id.ForgotPassword)

        // Pre-fill email if coming from registration
        val registeredEmail = intent.getStringExtra("email")

        if (!registeredEmail.isNullOrEmpty()) {
            emailInput.setText(registeredEmail)
        }

        // Login
        loginButton.setOnClickListener {
            loginUser()
        }

        // Register
        registerButton.setOnClickListener {

            startActivity(
                Intent(this, Register::class.java)
            )

            finish()
        }

        // Forgot password
        forgotPassword.setOnClickListener {

            val email = emailInput.text.toString().trim()

            if (email.isEmpty()) {

                emailInput.error = "Please enter your email"
                emailInput.requestFocus()

                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        Toast.makeText(
                            this,
                            "Password reset email sent to $email",
                            Toast.LENGTH_LONG
                        ).show()

                    } else {

                        Toast.makeText(
                            this,
                            "Error: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    // ==============================================
    // LOGIN USER
    // ==============================================

    private fun loginUser() {

        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        if (email.isEmpty()) {

            emailInput.error = "Please enter your email"
            emailInput.requestFocus()

            return
        }

        if (password.isEmpty()) {

            passwordInput.error = "Please enter your password"
            passwordInput.requestFocus()

            return
        }

        loginButton.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    val firebaseUser = auth.currentUser

                    if (firebaseUser == null) {

                        loginButton.isEnabled = true

                        Toast.makeText(
                            this,
                            "Unable to retrieve signed-in user.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@addOnCompleteListener
                    }

                    // Get authenticated UID
                    val uid = firebaseUser.uid

                    // Verify profile
                    checkUserProfile(uid, email)

                } else {

                    loginButton.isEnabled = true

                    Toast.makeText(
                        this,
                        "Login failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // ==============================================
    // CHECK USER PROFILE USING UID
    // ==============================================

    private fun checkUserProfile(
        uid: String,
        email: String
    ) {

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    // UID profile already exists
                    openDashboard(uid, document)

                } else {

                    // Try finding old automatically generated ID
                    findOldUserProfile(uid, email)
                }
            }
            .addOnFailureListener { error ->

                loginButton.isEnabled = true

                Toast.makeText(
                    this,
                    "Profile lookup failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ==============================================
    // FIND EXISTING PROFILE USING EMAIL
    // ==============================================

    private fun findOldUserProfile(
        uid: String,
        email: String
    ) {

        db.collection("users")
            .whereEqualTo("email", email)
            .limit(2)
            .get()
            .addOnSuccessListener { snapshot ->

                if (snapshot.isEmpty) {

                    loginButton.isEnabled = true

                    auth.signOut()

                    Toast.makeText(
                        this,
                        "User profile not found. Contact the administrator.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                // Avoid selecting the wrong profile
                if (snapshot.size() > 1) {

                    loginButton.isEnabled = true

                    auth.signOut()

                    Toast.makeText(
                        this,
                        "Multiple profiles found for this email. Contact the administrator.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                val oldDocument = snapshot.documents.first()

                val userData = oldDocument.data

                if (userData == null) {

                    loginButton.isEnabled = true

                    Toast.makeText(
                        this,
                        "User profile contains no data.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                // Add UID to existing profile data
                val migratedData = HashMap(userData)

                migratedData["uid"] = uid

                // Create new UID-based document
                db.collection("users")
                    .document(uid)
                    .set(migratedData)
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "Existing user profile successfully migrated.",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Use the migrated profile
                        db.collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener { newDocument ->

                                if (newDocument.exists()) {
                                    openDashboard(uid, newDocument)
                                } else {

                                    loginButton.isEnabled = true

                                    Toast.makeText(
                                        this,
                                        "Unable to verify migrated profile.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            .addOnFailureListener { error ->

                                loginButton.isEnabled = true

                                Toast.makeText(
                                    this,
                                    "Profile verification failed: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                    .addOnFailureListener { error ->

                        loginButton.isEnabled = true

                        Toast.makeText(
                            this,
                            "Profile migration failed: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { error ->

                loginButton.isEnabled = true

                Toast.makeText(
                    this,
                    "Unable to find existing profile: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ==============================================
    // VERIFY PROFILE AND OPEN DASHBOARD
    // ==============================================

    private fun openDashboard(
        uid: String,
        document: com.google.firebase.firestore.DocumentSnapshot
    ) {

        val name = document.getString("name")
            ?: document.getString("fullName")
            ?: "User"

        val email = document.getString("email")
            ?: auth.currentUser?.email
            ?: ""

        val department = document.getString("department")
            ?: ""

        val role = document.getString("role")
            ?.trim()
            .orEmpty()

        val allowedRoles = setOf(
            "admin",
            "it manager",
            "it staff",
            "asset personnel",
            "staff"
        )

        if (role.isBlank()) {

            loginButton.isEnabled = true

            Toast.makeText(
                this,
                "Your account has no assigned role. Contact the administrator.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if (role.lowercase() !in allowedRoles) {

            loginButton.isEnabled = true

            Toast.makeText(
                this,
                "Invalid account role. Contact the administrator.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        // Open Dashboard
        val intent = Intent(
            this,
            Dashboard::class.java
        )

        intent.putExtra("USER_UID", uid)
        intent.putExtra("USER_NAME", name)
        intent.putExtra("USER_EMAIL", email)
        intent.putExtra("USER_ROLE", role)
        intent.putExtra("USER_DEPARTMENT", department)

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        Toast.makeText(
            this,
            "Welcome, $name!",
            Toast.LENGTH_SHORT
        ).show()

        startActivity(intent)
        finish()
    }
}
