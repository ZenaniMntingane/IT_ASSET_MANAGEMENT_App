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
        setContentView(R.layout.activity_register)
        enableEdgeToEdge()
        val txtDate = findViewById<TextView>(R.id.txtDate)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)
        val loginButton = findViewById<Button>(R.id.btnRegister)

        emailInput = findViewById(R.id.registerEmailInput)
        passwordInput = findViewById(R.id.registerPasswordInput)
        nameInput = findViewById(R.id.registerNameInput)
        departmentInput = findViewById(R.id.registerDepartmentInput)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Show current date
        val currentDate = SimpleDateFormat(
            "dd MMM yyyy HH:mm",
            Locale.getDefault()
        ).format(Date())

        txtDate.text = "Registered On: $currentDate"

        // Go to Login
        tvLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

        // Register account
        loginButton.setOnClickListener {

            val emailText = emailInput.text.toString().trim()
            val passwordText = passwordInput.text.toString().trim()
            val nameText = nameInput.text.toString().trim()
            val departmentText = departmentInput.text.toString().trim()
            // Validate fields
            if (emailText.isEmpty()) {
                emailInput.error = "Enter your email"
                emailInput.requestFocus()
                return@setOnClickListener
            }

            if (passwordText.isEmpty()) {
                passwordInput.error = "Create a password"
                passwordInput.requestFocus()
                return@setOnClickListener
            }

            if (nameText.isEmpty()) {
                nameInput.error = "Enter your full name"
                nameInput.requestFocus()
                return@setOnClickListener
            }

            if (departmentText.isEmpty()) {
                departmentInput.error = "Enter your department"
                departmentInput.requestFocus()
                return@setOnClickListener
            }

            // Create Firebase Authentication account
            auth.createUserWithEmailAndPassword(
                emailText,
                passwordText
            ).addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    // Get newly created Firebase user
                    val firebaseUser = task.result?.user

                    if (firebaseUser == null) {
                        Toast.makeText(
                            this,
                            "Registration failed. User could not be created.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addOnCompleteListener
                    }

                    val userId = firebaseUser.uid

                    /*
                     * ROLE-BASED ACCESS CONTROL
                     *
                     * Every newly registered account is automatically
                     * assigned the Staff role.
                     *
                     * Users cannot choose their own role during registration.
                     */
                    val userMap = hashMapOf(
                        "name" to nameText,
                        "email" to emailText,
                        "department" to departmentText,
                        "role" to "Staff",
                        "registeredOn" to currentDate
                    )
                    // Save user information in Firestore
                    db.collection("users")
                        .document(userId)
                        .set(userMap)
                        .addOnSuccessListener {

                            Toast.makeText(
                                this,
                                "Registration successful!",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Go to Login
                            val intent = Intent(
                                this,
                                Login::class.java
                            )

                            intent.putExtra(
                                "email",
                                emailText
                            )

                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->

                            Toast.makeText(
                                this,
                                "Firestore error: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
        }
    }
}
