package com.example.it_asset_management_app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Register : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var departmentInput: EditText

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)


        // Firebase initialization
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Find views
        val txtDate = findViewById<TextView>(R.id.txtDate)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)
        val registerButton = findViewById<Button>(R.id.btnRegister)

        emailInput = findViewById(R.id.registerEmailInput)
        passwordInput = findViewById(R.id.registerPasswordInput)
        nameInput = findViewById(R.id.registerNameInput)
        departmentInput = findViewById(R.id.registerDepartmentInput)

        // Show registration date
        val currentDate = SimpleDateFormat(
            "dd MMM yyyy HH:mm",
            Locale.getDefault()
        ).format(Date())

        txtDate.text = "Registered On: $currentDate"

        // Navigate to Login
        tvLogin.setOnClickListener {

            startActivity(
                Intent(this, Login::class.java)
            )

            finish()
        }

        // Register user
        registerButton.setOnClickListener {

            val emailText = emailInput.text.toString().trim()
            val passwordText = passwordInput.text.toString()
            val nameText = nameInput.text.toString().trim()
            val departmentText = departmentInput.text.toString().trim()

            // Validate email
            if (emailText.isEmpty()) {
                emailInput.error = "Enter your email"
                emailInput.requestFocus()
                return@setOnClickListener
            }

            // Validate password
            if (passwordText.isEmpty()) {
                passwordInput.error = "Create a password"
                passwordInput.requestFocus()
                return@setOnClickListener
            }

            if (passwordText.length < 6) {
                passwordInput.error = "Password must contain at least 6 characters"
                passwordInput.requestFocus()
                return@setOnClickListener
            }

            // Validate name
            if (nameText.isEmpty()) {
                nameInput.error = "Enter your full name"
                nameInput.requestFocus()
                return@setOnClickListener
            }

            // Validate department
            if (departmentText.isEmpty()) {
                departmentInput.error = "Enter your department"
                departmentInput.requestFocus()
                return@setOnClickListener
            }

            registerButton.isEnabled = false

            // Create Firebase Authentication account
            auth.createUserWithEmailAndPassword(
                emailText,
                passwordText
            ).addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val firebaseUser = task.result?.user

                    if (firebaseUser == null) {

                        registerButton.isEnabled = true

                        Toast.makeText(
                            this,
                            "Unable to retrieve registered user.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@addOnCompleteListener
                    }

                    // Retrieve Firebase Authentication UID
                    val userId = firebaseUser.uid

                    // Every new user receives Staff role
                    val userMap = hashMapOf(
                        "uid" to userId,
                        "name" to nameText,
                        "email" to emailText,
                        "department" to departmentText,
                        "role" to "Staff",
                        "registeredOn" to currentDate
                    )

                    // Save Firestore profile using Firebase UID
                    db.collection("users")
                        .document(userId)
                        .set(userMap)
                        .addOnSuccessListener {

                            Toast.makeText(
                                this,
                                "Registration successful! Please log in.",
                                Toast.LENGTH_LONG
                            ).show()

                            // Return to Login
                            val intent = Intent(
                                this,
                                Login::class.java
                            )

                            intent.putExtra("email", emailText)

                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { error ->

                            registerButton.isEnabled = true

                            Toast.makeText(
                                this,
                                "Firestore error: ${error.message}",
                                Toast.LENGTH_LONG
                            ).show()

                        }

                } else {

                    registerButton.isEnabled = true

                    Toast.makeText(
                        this,
                        "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}