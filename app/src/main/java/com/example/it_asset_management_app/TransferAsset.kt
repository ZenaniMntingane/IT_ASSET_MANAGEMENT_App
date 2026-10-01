package com.example.it_asset_management_app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransferAsset : AppCompatActivity() {
    // ================= FIREBASE =================

    private lateinit var transfersDatabase: DatabaseReference
    private val storage = FirebaseStorage.getInstance()


    // ================= ASSET INFORMATION =================

    private lateinit var assetIdInput: EditText
    private lateinit var assetNameInput: EditText


    // ================= OLD INFORMATION =================

    private lateinit var oldUserInput: EditText
    private lateinit var oldDepartmentInput: EditText
    private lateinit var oldLocationInput: EditText


    // ================= NEW INFORMATION =================

    private lateinit var newUserInput: EditText
    private lateinit var newDepartmentInput: EditText
    private lateinit var newLocationInput: EditText


    // ================= SIGNATURES =================

    private lateinit var oldEmployeeSignature: SignatureView
    private lateinit var newEmployeeSignature: SignatureView

    private lateinit var btnClearOldSignature: MaterialButton
    private lateinit var btnClearNewSignature: MaterialButton


    // ================= BUTTON =================

    private lateinit var transferButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_transfer_asset)

        // ================= FIREBASE DATABASE =================

        transfersDatabase =
            FirebaseDatabase.getInstance()
                .getReference("transfers")


        // ================= ASSET INFORMATION =================

        assetIdInput = findViewById(R.id.assetIdInput)
        assetNameInput = findViewById(R.id.assetNameInput)


        // ================= OLD INFORMATION =================

        oldUserInput = findViewById(R.id.oldUserInput)
        oldDepartmentInput = findViewById(R.id.oldDepartmentInput)
        oldLocationInput = findViewById(R.id.oldLocationInput)


        // ================= NEW INFORMATION =================

        newUserInput = findViewById(R.id.newUserInput)
        newDepartmentInput = findViewById(R.id.newDepartmentInput)
        newLocationInput = findViewById(R.id.newLocationInput)


        // ================= SIGNATURE VIEWS =================

        oldEmployeeSignature =
            findViewById(R.id.oldEmployeeSignature)

        newEmployeeSignature =
            findViewById(R.id.newEmployeeSignature)


        // ================= CLEAR BUTTONS =================

        btnClearOldSignature =
            findViewById(R.id.btnClearOldSignature)

        btnClearNewSignature =
            findViewById(R.id.btnClearNewSignature)


        btnClearOldSignature.setOnClickListener {

            oldEmployeeSignature.clearSignature()

        }


        btnClearNewSignature.setOnClickListener {

            newEmployeeSignature.clearSignature()

        }


        // ================= TRANSFER BUTTON =================

        transferButton =
            findViewById(R.id.transferButton)


        transferButton.setOnClickListener {

            transferAsset()

        }
    }


    // ============================================================
    // TRANSFER ASSET
    // ============================================================

    private fun transferAsset() {

        // ================= GET INPUT VALUES =================

        val assetId =
            assetIdInput.text.toString().trim()

        val assetName =
            assetNameInput.text.toString().trim()


        val oldUser =
            oldUserInput.text.toString().trim()

        val oldDepartment =
            oldDepartmentInput.text.toString().trim()

        val oldLocation =
            oldLocationInput.text.toString().trim()


        val newUser =
            newUserInput.text.toString().trim()

        val newDepartment =
            newDepartmentInput.text.toString().trim()

        val newLocation =
            newLocationInput.text.toString().trim()


        // ============================================================
        // VALIDATE ASSET INFORMATION
        // ============================================================

        if (assetId.isEmpty()) {

            assetIdInput.error = "Enter Asset ID"

            assetIdInput.requestFocus()

            return
        }


        if (assetName.isEmpty()) {

            assetNameInput.error = "Enter Asset Name"

            assetNameInput.requestFocus()

            return
        }


        // ============================================================
        // VALIDATE OLD INFORMATION
        // ============================================================

        if (oldUser.isEmpty()) {

            oldUserInput.error = "Enter current user"

            oldUserInput.requestFocus()

            return
        }


        if (oldDepartment.isEmpty()) {

            oldDepartmentInput.error =
                "Enter current department"

            oldDepartmentInput.requestFocus()

            return
        }


        if (oldLocation.isEmpty()) {

            oldLocationInput.error =
                "Enter current location"

            oldLocationInput.requestFocus()

            return
        }


        // ============================================================
        // VALIDATE NEW INFORMATION
        // ============================================================

        if (newUser.isEmpty()) {

            newUserInput.error = "Enter new user"

            newUserInput.requestFocus()

            return
        }


        if (newDepartment.isEmpty()) {

            newDepartmentInput.error =
                "Enter new department"

            newDepartmentInput.requestFocus()

            return
        }


        if (newLocation.isEmpty()) {

            newLocationInput.error =
                "Enter new location"

            newLocationInput.requestFocus()

            return
        }


        // ============================================================
        // VALIDATE OLD EMPLOYEE SIGNATURE
        // ============================================================

        if (!oldEmployeeSignature.hasSignature()) {

            Toast.makeText(
                this,
                "Please ask the current employee to sign.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        // ============================================================
        // VALIDATE NEW EMPLOYEE SIGNATURE
        // ============================================================

        if (!newEmployeeSignature.hasSignature()) {

            Toast.makeText(
                this,
                "Please ask the new employee to sign.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        // ============================================================
        // DISABLE BUTTON WHILE PROCESSING
        // ============================================================

        transferButton.isEnabled = false

        transferButton.text = "TRANSFERRING..."


        // ============================================================
        // CREATE TRANSFER ID
        // ============================================================

        val transferId =
            transfersDatabase.push().key


        if (transferId == null) {

            transferButton.isEnabled = true

            transferButton.text = "TRANSFER"

            Toast.makeText(
                this,
                "Could not create transfer record",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        // ============================================================
        // UPLOAD OLD EMPLOYEE SIGNATURE
        // ============================================================

        uploadSignature(
            oldEmployeeSignature,
            "old_employee",
            transferId
        ) { oldSignatureUrl ->


            // ========================================================
            // UPLOAD NEW EMPLOYEE SIGNATURE
            // ========================================================

            uploadSignature(
                newEmployeeSignature,
                "new_employee",
                transferId
            ) { newSignatureUrl ->


                // ====================================================
                // DATE AND TIME
                // ====================================================

                val dateFormat =
                    SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                    )

                val transferDate =
                    dateFormat.format(Date())


                // ====================================================
                // CREATE TRANSFER RECORD
                // ====================================================

                val transfer =
                    hashMapOf<String, Any>(

                        "transferId" to transferId,

                        "assetId" to assetId,

                        "assetName" to assetName,


                        // OLD INFORMATION

                        "oldInformation" to hashMapOf(

                            "user" to oldUser,

                            "department" to oldDepartment,

                            "location" to oldLocation,

                            "signatureUrl" to oldSignatureUrl
                        ),


                        // NEW INFORMATION

                        "newInformation" to hashMapOf(

                            "user" to newUser,

                            "department" to newDepartment,

                            "location" to newLocation,

                            "signatureUrl" to newSignatureUrl
                        ),


                        // TRANSFER INFORMATION

                        "transferDate" to transferDate,

                        "status" to "Transferred"
                    )


                // ====================================================
                // SAVE TO REALTIME DATABASE
                // ====================================================

                transfersDatabase
                    .child(transferId)
                    .setValue(transfer)

                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "Asset transferred successfully",
                            Toast.LENGTH_LONG
                        ).show()


                        // Clear everything

                        clearFields()


                        // Enable button

                        transferButton.isEnabled = true

                        transferButton.text = "TRANSFER"
                    }

                    .addOnFailureListener { exception ->

                        transferButton.isEnabled = true

                        transferButton.text = "TRANSFER"

                        Toast.makeText(
                            this,
                            "Transfer failed: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }
    }


    // ============================================================
    // UPLOAD SIGNATURE
    // ============================================================

    private fun uploadSignature(
        signatureView: SignatureView,
        employeeType: String,
        transferId: String,
        onSuccess: (String) -> Unit
    ) {

        // Get signature bitmap

        val bitmap =
            signatureView.getSignatureBitmap()


        // Convert bitmap to PNG

        val outputStream =
            ByteArrayOutputStream()


        bitmap.compress(
            android.graphics.Bitmap.CompressFormat.PNG,
            100,
            outputStream
        )


        val signatureBytes =
            outputStream.toByteArray()


        // Firebase Storage path

        val signatureReference =
            storage
                .reference
                .child("transfer_signatures")
                .child(transferId)
                .child("$employeeType.png")


        // Upload

        signatureReference
            .putBytes(signatureBytes)

            .addOnSuccessListener {

                // Get download URL

                signatureReference
                    .downloadUrl

                    .addOnSuccessListener { uri ->

                        onSuccess(uri.toString())
                    }

                    .addOnFailureListener { exception ->

                        transferButton.isEnabled = true

                        transferButton.text = "TRANSFER"

                        Toast.makeText(
                            this,
                            "Could not get signature URL: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }

            .addOnFailureListener { exception ->

                transferButton.isEnabled = true

                transferButton.text = "TRANSFER"

                Toast.makeText(
                    this,
                    "Signature upload failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // ============================================================
    // CLEAR FIELDS
    // ============================================================

    private fun clearFields() {

        assetIdInput.text.clear()

        assetNameInput.text.clear()


        oldUserInput.text.clear()

        oldDepartmentInput.text.clear()

        oldLocationInput.text.clear()


        newUserInput.text.clear()

        newDepartmentInput.text.clear()

        newLocationInput.text.clear()


        // Clear signatures

        oldEmployeeSignature.clearSignature()

        newEmployeeSignature.clearSignature()
    }
}
