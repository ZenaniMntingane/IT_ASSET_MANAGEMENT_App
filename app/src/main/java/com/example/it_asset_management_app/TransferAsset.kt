package com.example.it_asset_management_app

import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Signature
import android.util.Base64
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.android.identity.util.UUID
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.FirebaseFirestore
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.util.Locale


class TransferAsset : AppCompatActivity() {
    // ---------------------------------------------------------
    // FIREBASE
    // ---------------------------------------------------------

    private lateinit var auth: FirebaseAuth
    private lateinit var assetsRef: DatabaseReference
    private lateinit var firestore: FirebaseFirestore

    // ---------------------------------------------------------
    // INPUT FIELDS
    // ---------------------------------------------------------

    private lateinit var assetIdInput: MaterialAutoCompleteTextView
    private lateinit var assetNameInput: TextInputEditText

    private lateinit var oldUserInput: TextInputEditText
    private lateinit var oldDepartmentInput: TextInputEditText
    private lateinit var oldLocationInput: TextInputEditText

    // New employee dropdown
    private lateinit var newUserInput: MaterialAutoCompleteTextView
    private lateinit var newDepartmentInput: TextInputEditText
    private lateinit var newLocationInput: TextInputEditText

    private lateinit var hashValueInput: TextInputEditText
    private lateinit var digitalSignatureInput: TextInputEditText

    // ---------------------------------------------------------
    // BUTTONS AND STATUS
    // ---------------------------------------------------------

    private lateinit var txtSignatureStatus: TextView

    private lateinit var btnSignTransfer: MaterialButton
    private lateinit var btnVerifySignature: MaterialButton
    private lateinit var transferButton: MaterialButton

    // ---------------------------------------------------------
    // SECURITY
    // ---------------------------------------------------------

    private val keyAlias = "IT_ASSET_TRANSFER_RSA_KEY_V2"

    private val allowedRoles = setOf(
        "Admin",
        "IT Manager",
        "IT Staff",
        "Asset Personnel",
        "Staff"
    )

    private var signedInRole: String = ""

    // ---------------------------------------------------------
    // ASSETS
    // ---------------------------------------------------------

    private val assetSnapshots = mutableListOf<DataSnapshot>()

    private var selectedAsset: DataSnapshot? = null

    // ---------------------------------------------------------
    // EMPLOYEES
    // ---------------------------------------------------------

    private val employeesList = mutableListOf<Employee>()

    private var selectedEmployee: Employee? = null

    // ---------------------------------------------------------
    // DIGITAL SIGNATURE STATE
    // ---------------------------------------------------------

    private var signedPayload: String? = null
    private var signedHash: String? = null
    private var signatureBase64: String? = null

    private var verificationPassed = false
    private var transferInProgress = false

    // ---------------------------------------------------------
    // ON CREATE
    // ---------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_transfer_asset)
        auth = FirebaseAuth.getInstance()

        assetsRef = FirebaseDatabase.getInstance()
            .reference
            .child("assets")

        firestore = FirebaseFirestore.getInstance()

        bindViews()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        transferButton.isEnabled = false

        assetIdInput.setOnItemClickListener { _, _, position, _ ->

            if (position in assetSnapshots.indices) {
                selectAsset(assetSnapshots[position])
            }
        }

        // Open employee dropdown when clicked.
        newUserInput.setOnClickListener {
            newUserInput.showDropDown()
        }

        // Open dropdown when the field receives focus.
        newUserInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                newUserInput.showDropDown()
            }
        }

        // Sign transfer
        btnSignTransfer.setOnClickListener {
            signTransfer()
        }

        // Verify signature
        btnVerifySignature.setOnClickListener {
            verifySignature()
        }

        // Save transfer
        transferButton.setOnClickListener {
            saveTransfer()
        }

        checkUserRole()
    }

    // ---------------------------------------------------------
    // BIND VIEWS
    // ---------------------------------------------------------

    private fun bindViews() {

        assetIdInput = findViewById(R.id.assetIdInput)
        assetNameInput = findViewById(R.id.assetNameInput)

        oldUserInput = findViewById(R.id.oldUserInput)
        oldDepartmentInput = findViewById(R.id.oldDepartmentInput)
        oldLocationInput = findViewById(R.id.oldLocationInput)

        newUserInput = findViewById(R.id.newUserInput)
        newDepartmentInput = findViewById(R.id.newDepartmentInput)
        newLocationInput = findViewById(R.id.newLocationInput)


        hashValueInput = findViewById(R.id.hashValueInput)
        digitalSignatureInput = findViewById(R.id.digitalSignatureInput)

        txtSignatureStatus = findViewById(R.id.txtSignatureStatus)

        btnSignTransfer = findViewById(R.id.btnSignTransfer)
        btnVerifySignature = findViewById(R.id.btnVerifySignature)
        transferButton = findViewById(R.id.transferButton)
    }

    // ---------------------------------------------------------
    // USER ROLE VERIFICATION
    // FIRESTORE: users/{FIREBASE_AUTH_UID}
    // ---------------------------------------------------------

    private fun checkUserRole() {

        val currentUser = auth.currentUser

        if (currentUser == null) {
            toast("Please sign in first.")
            finish()
            return
        }

        val uid = currentUser.uid

        Log.d("TransferAsset", "AUTH UID: $uid")
        Log.d("TransferAsset", "AUTH EMAIL: ${currentUser.email}")

        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {

                    Log.e(
                        "TransferAsset",
                        "No profile found for UID: $uid"
                    )

                    toast("User profile not found for your signed-in UID.")
                    return@addOnSuccessListener
                }

                val role = document.getString("role")
                    .orEmpty()
                    .trim()

                Log.d(
                    "TransferAsset",
                    "USER PROFILE: ${document.data}"
                )

                Log.d(
                    "TransferAsset",
                    "USER ROLE: $role"
                )

                if (role !in allowedRoles) {
                    toast("Unauthorized role: $role")
                    return@addOnSuccessListener
                }

                signedInRole = role

                toast("Access granted: $role")

                // Load allocated assets.
                loadAllocatedAssets()

                // Load all registered employees from Firestore.
                loadEmployees()
            }
            .addOnFailureListener { error ->

                Log.e(
                    "TransferAsset",
                    "Firestore profile lookup failed",
                    error
                )

                toast(
                    "Could not verify your role: ${error.message}"
                )
            }
    }

    // ---------------------------------------------------------
    // LOAD EMPLOYEES
    // FIRESTORE: users COLLECTION
    // LOAD ALL REGISTERED USERS
    // ---------------------------------------------------------

    private fun loadEmployees() {

        firestore.collection("users")
            .get()
            .addOnSuccessListener { documents ->

                employeesList.clear()

                val employeeLabels = mutableListOf<String>()

                for (document in documents) {

                    val employee = Employee(

                        // Firestore document ID is the Firebase Auth UID.
                        uid = document.id,

                        name = document.getString("name")
                            ?.trim()
                            .takeUnless { it.isNullOrBlank() }
                            ?: document.getString("fullName")
                                ?.trim()
                                .orEmpty(),

                        email = document.getString("email")
                            .orEmpty()
                            .trim(),

                        department = document.getString("department")
                            .orEmpty()
                            .trim(),

                        role = document.getString("role")
                            .orEmpty()
                            .trim()
                    )

                    // Include registered profiles with a valid name.
                    if (
                        employee.name.isNotBlank() &&
                        employee.uid.isNotBlank()
                    ) {

                        employeesList.add(employee)

                        val label = if (employee.email.isNotBlank()) {
                            "${employee.name} (${employee.email})"
                        } else {
                            employee.name
                        }

                        employeeLabels.add(label)
                    }
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    employeeLabels
                )

                newUserInput.setAdapter(adapter)

                // Select an employee from the dropdown.
                newUserInput.setOnItemClickListener { _, _, position, _ ->

                    if (position in employeesList.indices) {

                        selectedEmployee = employeesList[position]

                        val employee = selectedEmployee!!

                        // Display employee name in the field.
                        newUserInput.setText(
                            employee.name,
                            false
                        )

                        // Automatically populate department.
                        newDepartmentInput.setText(
                            employee.department
                        )

                        // Clear previous signature because details changed.
                        clearSignatureState()

                        Log.d(
                            "TransferAsset",
                            "Selected employee: ${employee.name}, UID: ${employee.uid}"
                        )
                    }
                }

                Log.d(
                    "TransferAsset",
                    "Registered employees loaded: ${employeesList.size}"
                )

                if (employeesList.isEmpty()) {

                    toast("No registered employees found in Firestore.")

                } else {

                    toast(
                        "${employeesList.size} registered employees loaded."
                    )
                }
            }
            .addOnFailureListener { error ->

                Log.e(
                    "TransferAsset",
                    "Loading employees failed",
                    error
                )

                toast(
                    "Could not load employees: ${error.message}"
                )
            }
    }

    // ---------------------------------------------------------
    // LOAD ALLOCATED ASSETS
    // DISPLAY ASSET NUMBER + CURRENT ALLOCATED USER
    // ---------------------------------------------------------

    private fun loadAllocatedAssets() {

        assetsRef.get()
            .addOnSuccessListener { snapshot ->

                assetSnapshots.clear()

                val labels = mutableListOf<String>()

                for (asset in snapshot.children) {

                    val assignedUser = value(
                        asset,
                        "assignedUser"
                    ).trim()

                    // Only load currently allocated assets.
                    if (
                        assignedUser.isNotBlank() &&
                        !assignedUser.equals("Not Assigned", true) &&
                        !assignedUser.equals("Unassigned", true)
                    ) {

                        val number = assetNumber(asset)

                        if (number.isNotBlank()) {

                            assetSnapshots.add(asset)

                            labels.add(
                                "$number — Allocated to: $assignedUser"
                            )
                        }
                    }
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    labels
                )

                assetIdInput.setAdapter(adapter)

                if (labels.isEmpty()) {

                    setStatus(
                        "No allocated assets found.",
                        false
                    )

                } else {

                    Log.d(
                        "TransferAsset",
                        "Allocated assets loaded: ${labels.size}"
                    )
                }
            }
            .addOnFailureListener { error ->

                Log.e(
                    "TransferAsset",
                    "Loading assets failed",
                    error
                )

                toast(
                    "Could not load assets: ${error.message}"
                )
            }
    }

    // ---------------------------------------------------------
    // SELECT ASSET
    // ---------------------------------------------------------

    private fun selectAsset(asset: DataSnapshot) {

        selectedAsset = asset

        assetNameInput.setText(
            value(asset, "assetName", "name")
        )

        oldUserInput.setText(
            value(asset, "assignedUser")
        )

        oldDepartmentInput.setText(
            value(asset, "department")
        )

        oldLocationInput.setText(
            value(asset, "location")
        )

        clearSignatureState()

        Log.d(
            "TransferAsset",
            "Selected asset: ${asset.key}"
        )
    }

    // ---------------------------------------------------------
    // GET FIREBASE VALUE
    // ---------------------------------------------------------

    private fun value(
        snapshot: DataSnapshot,
        vararg keys: String
    ): String {

        for (key in keys) {

            val result = snapshot.child(key)
                .getValue(String::class.java)

            if (!result.isNullOrBlank()) {
                return result
            }
        }

        return ""
    }

    // ---------------------------------------------------------
    // GET ASSET NUMBER
    // ---------------------------------------------------------

    private fun assetNumber(asset: DataSnapshot): String {

        return value(
            asset,
            "assetNumber",
            "assetId",
            "assetTag"
        ).ifBlank {
            asset.key.orEmpty()
        }
    }

    // ---------------------------------------------------------
    // VALIDATE TRANSFER INPUTS
    // ---------------------------------------------------------

    private fun validateInputs(): Boolean {

        if (auth.currentUser == null) {

            toast(
                "Please sign in before transferring an asset."
            )

            return false
        }

        if (signedInRole !in allowedRoles) {

            toast(
                "Your account is not authorised to transfer assets."
            )

            return false
        }

        if (selectedAsset == null) {

            toast("Please select an allocated asset.")

            return false
        }

        val employee = selectedEmployee

        if (
            employee == null ||
            employee.uid.isBlank() ||
            newUserInput.text.toString().trim() != employee.name
        ) {

            newUserInput.error = "Please select an employee from the list."
            newUserInput.requestFocus()

            return false
        }

        if (newDepartmentInput.text.toString().trim().isBlank()) {

            newDepartmentInput.error = "Enter the new department"
            newDepartmentInput.requestFocus()

            return false
        }

        if (newLocationInput.text.toString().trim().isBlank()) {

            newLocationInput.error = "Enter the new location"
            newLocationInput.requestFocus()

            return false
        }

        return true
    }

    // ---------------------------------------------------------
    // CREATE CANONICAL PAYLOAD
    // ---------------------------------------------------------

    private fun canonicalPayload(): String {

        val asset = selectedAsset ?: return ""
        val employee = selectedEmployee ?: return ""

        return listOf(

            "assetKey=${asset.key.orEmpty()}",

            "assetNumber=${assetNumber(asset)}",

            "assetName=${assetNameInput.text.toString().trim()}",

            "oldUser=${oldUserInput.text.toString().trim()}",

            "oldDepartment=${oldDepartmentInput.text.toString().trim()}",

            "oldLocation=${oldLocationInput.text.toString().trim()}",

            "newUser=${employee.name}",

            "newUserUid=${employee.uid}",

            "newUserEmail=${employee.email}",

            "newDepartment=${newDepartmentInput.text.toString().trim()}",

            "newLocation=${newLocationInput.text.toString().trim()}",

            "performedBy=${auth.currentUser?.uid.orEmpty()}"

        ).joinToString("\n")
    }

    // ---------------------------------------------------------
    // SHA-256 HASHING
    // ---------------------------------------------------------

    private fun sha256Hex(text: String): String {

        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(
                text.toByteArray(StandardCharsets.UTF_8)
            )

        return bytes.joinToString("") {

            String.format(
                Locale.ROOT,
                "%02x",
                it.toInt() and 0xff
            )
        }
    }

    // ---------------------------------------------------------
    // RSA SIGNING KEY
    // ---------------------------------------------------------

    private fun getSigningKey(): PrivateKey {

        val keyStore = KeyStore.getInstance("AndroidKeyStore")

        keyStore.load(null)

        val existing = keyStore.getKey(keyAlias, null)

        if (existing is PrivateKey) {

            val certificate = keyStore.getCertificate(keyAlias)
                ?: throw IllegalStateException(
                    "Signing key certificate is missing."
                )

            if (!certificate.publicKey.algorithm.equals("RSA", true)) {

                throw IllegalStateException(
                    "The existing signing key is not RSA."
                )
            }

            return existing
        }

        if (keyStore.containsAlias(keyAlias)) {

            throw IllegalStateException(
                "The key alias exists but is not a usable private key."
            )
        }

        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            "AndroidKeyStore"
        )

        generator.initialize(

            KeyGenParameterSpec.Builder(
                keyAlias,

                KeyProperties.PURPOSE_SIGN or
                        KeyProperties.PURPOSE_VERIFY
            )
                .setKeySize(2048)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setSignaturePaddings(
                    KeyProperties.SIGNATURE_PADDING_RSA_PKCS1
                )
                .build()
        )

        return generator.generateKeyPair().private
    }

    // ---------------------------------------------------------
    // SIGN TRANSFER
    // ---------------------------------------------------------

    private fun signTransfer() {

        if (!validateInputs()) return

        try {

            val payload = canonicalPayload()

            val privateKey = getSigningKey()

            val signer = Signature.getInstance("SHA256withRSA")

            signer.initSign(privateKey)

            signer.update(
                payload.toByteArray(StandardCharsets.UTF_8)
            )

            val signature = signer.sign()

            signedPayload = payload

            signedHash = sha256Hex(payload)

            signatureBase64 = Base64.encodeToString(
                signature,
                Base64.NO_WRAP
            )

            hashValueInput.setText(signedHash)

            digitalSignatureInput.setText(signatureBase64)

            verificationPassed = false

            transferButton.isEnabled = false

            setStatus(
                "Signature generated. Verify before saving.",
                false
            )

            toast(
                "Transfer signed. Tap VERIFY DIGITAL SIGNATURE."
            )

        } catch (e: Exception) {

            verificationPassed = false

            transferButton.isEnabled = false

            Log.e(
                "TransferAsset",
                "Signing failed",
                e
            )

            val causeText = generateSequence(e as Throwable?) {
                it.cause
            }
                .mapNotNull { it.message }
                .joinToString(" -> ")

            setStatus(
                "Signing failed. See Logcat for details.",
                false
            )

            toast(
                "Signing failed: " +
                        (causeText.ifBlank {
                            e.javaClass.simpleName
                        })
            )
        }
    }

    // ---------------------------------------------------------
    // VERIFY DIGITAL SIGNATURE
    // ---------------------------------------------------------

    private fun verifySignature() {

        if (!validateInputs()) return

        val payload = signedPayload
        val hash = signedHash
        val signatureText = signatureBase64

        if (
            payload.isNullOrBlank() ||
            hash.isNullOrBlank() ||
            signatureText.isNullOrBlank()
        ) {

            setStatus(
                "Sign the transfer first.",
                false
            )

            toast("Tap SIGN TRANSFER first.")

            return
        }

        try {

            val currentPayload = canonicalPayload()

            if (
                currentPayload != payload ||
                sha256Hex(currentPayload) != hash
            ) {

                clearSignatureState()

                setStatus(
                    "Transfer details changed. Sign again.",
                    false
                )

                toast(
                    "Details changed. Please sign the transfer again."
                )

                return
            }

            val keyStore = KeyStore.getInstance("AndroidKeyStore")

            keyStore.load(null)

            val certificate = keyStore.getCertificate(keyAlias)
                ?: throw IllegalStateException(
                    "Signing certificate not found. Sign again."
                )

            val verifier = Signature.getInstance("SHA256withRSA")

            verifier.initVerify(certificate.publicKey)

            verifier.update(
                currentPayload.toByteArray(StandardCharsets.UTF_8)
            )

            val verified = verifier.verify(
                Base64.decode(
                    signatureText,
                    Base64.NO_WRAP
                )
            )

            verificationPassed = verified

            transferButton.isEnabled = verified

            if (verified) {

                setStatus(
                    "Signature verified. Ready to transfer.",
                    true
                )

                toast(
                    "Signature verified successfully."
                )

            } else {

                setStatus(
                    "Signature invalid. Sign the transfer again.",
                    false
                )
            }

        } catch (e: Exception) {

            verificationPassed = false

            transferButton.isEnabled = false

            Log.e(
                "TransferAsset",
                "Verification failed",
                e
            )

            setStatus(
                "Verification failed.",
                false
            )

            toast(
                "Verification failed: ${e.message}"
            )
        }
    }

    // ---------------------------------------------------------
    // SAVE TRANSFER
    // FIRESTORE TRANSFER RECORD
    // ---------------------------------------------------------

    private fun saveTransfer() {

        if (transferInProgress) return

        if (!verificationPassed || signedPayload == null) {

            transferButton.isEnabled = false

            toast(
                "Sign and verify the transfer before saving."
            )

            return
        }

        if (!validateInputs()) return

        if (canonicalPayload() != signedPayload) {

            clearSignatureState()

            setStatus(
                "Details changed. Sign and verify again.",
                false
            )

            toast(
                "Transfer details changed. Please sign again."
            )

            return
        }

        val asset = selectedAsset ?: return
        val employee = selectedEmployee ?: return

        val uid = auth.currentUser?.uid ?: return
        val assetKey = asset.key ?: return

        transferInProgress = true

        transferButton.isEnabled = false

        btnSignTransfer.isEnabled = false
        btnVerifySignature.isEnabled = false

        val transferData = hashMapOf<String, Any>(

            "assetKey" to assetKey,

            "assetNumber" to assetNumber(asset),

            "assetName" to assetNameInput.text.toString().trim(),

            "oldUser" to oldUserInput.text.toString().trim(),

            "oldDepartment" to oldDepartmentInput.text.toString().trim(),

            "oldLocation" to oldLocationInput.text.toString().trim(),

            "newUser" to employee.name,

            "newUserUid" to employee.uid,

            "newUserEmail" to employee.email,

            "newUserRole" to employee.role,

            "newDepartment" to newDepartmentInput.text.toString().trim(),

            "newLocation" to newLocationInput.text.toString().trim(),

            "performedBy" to uid,

            "transferredByUid" to uid,

            "hashAlgorithm" to "SHA-256",

            "signatureAlgorithm" to "SHA256withRSA",

            "hash" to signedHash.orEmpty(),

            "digitalSignature" to signatureBase64.orEmpty(),

            "signedPayload" to signedPayload.orEmpty(),

            "verified" to true,

            "verificationStatus" to "Verified",

            "transferredAt" to System.currentTimeMillis()
        )

        val transferDocument =
            firestore.collection("transfers").document()

        transferDocument.set(transferData)

            .addOnSuccessListener {

                Log.d(
                    "TransferAsset",
                    "Transfer saved to Firestore: ${transferDocument.id}"
                )

                updateAssetAllocation(
                    assetKey = assetKey,
                    transferId = transferDocument.id,
                    uid = uid,
                    asset = asset
                )
            }

            .addOnFailureListener { error ->

                Log.e(
                    "TransferAsset",
                    "Saving transfer failed",
                    error
                )

                transferInProgress = false

                btnSignTransfer.isEnabled = true
                btnVerifySignature.isEnabled = true

                transferButton.isEnabled = verificationPassed

                toast(
                    "Could not save transfer: ${error.message}"
                )
            }
    }

    // ---------------------------------------------------------
    // UPDATE REALTIME DATABASE ALLOCATION
    // ---------------------------------------------------------

    private fun updateAssetAllocation(
        assetKey: String,
        transferId: String,
        uid: String,
        asset: DataSnapshot
    ) {

        val employee = selectedEmployee ?: return

        val assetUpdates = mapOf<String, Any>(

            "assignedUser" to employee.name,

            "assignedUserUid" to employee.uid,

            "department" to newDepartmentInput.text.toString().trim(),

            "location" to newLocationInput.text.toString().trim()
        )

        assetsRef.child(assetKey)
            .updateChildren(assetUpdates)

            .addOnSuccessListener {

                Log.d(
                    "TransferAsset",
                    "Asset allocation updated successfully."
                )

                createAuditRecord(
                    asset = asset,
                    assetKey = assetKey,
                    transferId = transferId,
                    uid = uid
                )
            }

            .addOnFailureListener { error ->

                Log.e(
                    "TransferAsset",
                    "Asset update failed",
                    error
                )

                transferInProgress = false

                btnSignTransfer.isEnabled = true
                btnVerifySignature.isEnabled = true

                transferButton.isEnabled = verificationPassed

                setStatus(
                    "Transfer record saved, but allocation update failed.",
                    false
                )

                toast(
                    "Allocation update failed: ${error.message}. " +
                            "The Firestore transfer record was preserved."
                )
            }
    }

    // ---------------------------------------------------------
    // CREATE AUDIT TRAIL RECORD
    // REALTIME DATABASE: auditTrail
    // ---------------------------------------------------------

    private fun createAuditRecord(
        asset: DataSnapshot,
        assetKey: String,
        transferId: String,
        uid: String
    ) {

        val employee = selectedEmployee ?: return

        val auditId = UUID.randomUUID().toString()

        val auditData = hashMapOf<String, Any>(

            "auditId" to auditId,

            "action" to "ASSET_TRANSFER",

            "assetKey" to assetKey,

            "assetNumber" to assetNumber(asset),

            "assetName" to assetNameInput.text.toString().trim(),

            "oldUser" to oldUserInput.text.toString().trim(),

            "oldDepartment" to oldDepartmentInput.text.toString().trim(),

            "oldLocation" to oldLocationInput.text.toString().trim(),

            "newUser" to employee.name,

            "newUserUid" to employee.uid,

            "newUserEmail" to employee.email,

            "newUserRole" to employee.role,

            "newDepartment" to newDepartmentInput.text.toString().trim(),

            "newLocation" to newLocationInput.text.toString().trim(),

            "performedBy" to uid,

            "performedByRole" to signedInRole,

            "transferId" to transferId,

            "hashAlgorithm" to "SHA-256",

            "signatureAlgorithm" to "SHA256withRSA",

            "hash" to signedHash.orEmpty(),

            "digitalSignature" to signatureBase64.orEmpty(),

            "verificationStatus" to "Verified",

            "timestamp" to System.currentTimeMillis(),

            "status" to "Completed"
        )

        FirebaseDatabase.getInstance()
            .reference
            .child("auditTrail")
            .child(auditId)
            .setValue(auditData)

            .addOnSuccessListener {

                Log.d(
                    "TransferAsset",
                    "Audit record created: $auditId"
                )

                transferInProgress = false

                setStatus(
                    "Transfer, allocation and audit completed successfully.",
                    true
                )

                toast(
                    "Asset transferred successfully."
                )

                finish()
            }

            .addOnFailureListener { error ->

                Log.e(
                    "TransferAsset",
                    "Audit record creation failed",
                    error
                )

                transferInProgress = false

                setStatus(
                    "Asset transferred, but audit record failed.",
                    false
                )

                toast(
                    "Asset allocation updated, but audit record failed: " +
                            error.message
                )
            }
    }

    // ---------------------------------------------------------
    // CLEAR SIGNATURE DATA
    // ---------------------------------------------------------

    private fun clearSignatureState() {

        signedPayload = null
        signedHash = null
        signatureBase64 = null

        verificationPassed = false

        hashValueInput.setText("")
        digitalSignatureInput.setText("")

        transferButton.isEnabled = false

        if (::txtSignatureStatus.isInitialized) {

            setStatus(
                "Awaiting signature.",
                false
            )
        }
    }

    // ---------------------------------------------------------
    // SIGNATURE STATUS
    // ---------------------------------------------------------

    private fun setStatus(
        message: String,
        success: Boolean
    ) {

        txtSignatureStatus.text = "Status: $message"

        if (success) {

            txtSignatureStatus.setBackgroundColor(
                android.graphics.Color.parseColor("#D1FAE5")
            )

            txtSignatureStatus.setTextColor(
                android.graphics.Color.parseColor("#065F46")
            )

        } else {

            txtSignatureStatus.setBackgroundColor(
                android.graphics.Color.parseColor("#FFF7E6")
            )

            txtSignatureStatus.setTextColor(
                android.graphics.Color.parseColor("#B45309")
            )
        }
    }

    // ---------------------------------------------------------
    // TOAST MESSAGE
    // ---------------------------------------------------------

    private fun toast(message: String) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}