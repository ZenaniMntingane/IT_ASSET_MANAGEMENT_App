package com.example.it_asset_management_app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.text.TextWatcher
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class AssetPersonnel : AppCompatActivity() {

    // =====================================================
    // VIEWS
    // =====================================================

    private lateinit var btnBack: ImageView

    private lateinit var etSearchPersonnel: TextInputEditText

    private lateinit var personnelContainer: LinearLayout

    private lateinit var txtNoPersonnel: TextView

    private lateinit var txtTotalPersonnel: TextView

    private lateinit var txtITStaff: TextView

    private lateinit var txtStaff: TextView


    // =====================================================
    // FIREBASE
    // =====================================================

    private lateinit var firestore: FirebaseFirestore


    // =====================================================
    // PERSONNEL LIST
    // =====================================================

    private val personnelList =
        ArrayList<Personnel>()


    // =====================================================
    // ON CREATE
    // =====================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_asset_personnel)

        // =================================================
        // FIREBASE
        // =================================================

        firestore =
            FirebaseFirestore.getInstance()


        // =================================================
        // FIND VIEWS
        // =================================================

        btnBack =
            findViewById(R.id.btnBack)

        etSearchPersonnel =
            findViewById(R.id.etSearchPersonnel)

        personnelContainer =
            findViewById(R.id.personnelContainer)

        txtNoPersonnel =
            findViewById(R.id.txtNoPersonnel)

        txtTotalPersonnel =
            findViewById(R.id.txtTotalPersonnel)

        txtITStaff =
            findViewById(R.id.txtITStaff)

        txtStaff =
            findViewById(R.id.txtStaff)


        // =================================================
        // BACK BUTTON
        // =================================================

        btnBack.setOnClickListener {

            finish()
        }


        // =================================================
        // LOAD PERSONNEL
        // =================================================

        loadPersonnel()


        // =================================================
        // SEARCH PERSONNEL
        // =================================================

        etSearchPersonnel.addTextChangedListener(

            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing required
                }


                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    filterPersonnel(
                        s?.toString() ?: ""
                    )
                }


                override fun afterTextChanged(
                    s: Editable?
                ) {
                    // Nothing required
                }
            }
        )
    }


    // =====================================================
    // LOAD PERSONNEL FROM FIRESTORE
    // =====================================================

    private fun loadPersonnel() {

        firestore
            .collection("users")
            .get()
            .addOnSuccessListener { result ->

                personnelList.clear()


                for (document in result.documents) {

                    val name =
                        document.getString("name")
                            ?: "Unknown User"

                    val email =
                        document.getString("email")
                            ?: ""

                    val department =
                        document.getString("department")
                            ?: "Not specified"

                    val role =
                        document.getString("role")
                            ?: "Staff"

                    val uid =
                        document.id


                    val personnel =
                        Personnel(
                            uid = uid,
                            name = name,
                            email = email,
                            department = department,
                            role = role
                        )


                    personnelList.add(
                        personnel
                    )
                }


                updateStatistics()

                displayPersonnel(
                    personnelList
                )
            }

            .addOnFailureListener {

                personnelList.clear()

                updateStatistics()

                displayPersonnel(
                    emptyList()
                )
            }
    }


    // =====================================================
    // UPDATE STATISTICS
    // =====================================================

    private fun updateStatistics() {

        val total =
            personnelList.size


        val itStaffCount =
            personnelList.count {

                it.role.equals(
                    "IT Staff",
                    ignoreCase = true
                )
            }


        val staffCount =
            personnelList.count {

                it.role.equals(
                    "Staff",
                    ignoreCase = true
                )
            }


        txtTotalPersonnel.text =
            total.toString()

        txtITStaff.text =
            itStaffCount.toString()

        txtStaff.text =
            staffCount.toString()
    }


    // =====================================================
    // FILTER PERSONNEL
    // =====================================================

    private fun filterPersonnel(
        searchText: String
    ) {

        val query =
            searchText
                .trim()
                .lowercase()


        // -------------------------------------------------
        // SHOW EVERYTHING
        // -------------------------------------------------

        if (query.isEmpty()) {

            displayPersonnel(
                personnelList
            )

            return
        }


        // -------------------------------------------------
        // FILTER
        // -------------------------------------------------

        val filteredList =
            personnelList.filter { person ->

                person.name
                    .lowercase()
                    .contains(query) ||

                        person.email
                            .lowercase()
                            .contains(query) ||

                        person.department
                            .lowercase()
                            .contains(query) ||

                        person.role
                            .lowercase()
                            .contains(query)
            }


        displayPersonnel(
            filteredList
        )
    }


    // =====================================================
    // DISPLAY PERSONNEL
    // =====================================================

    private fun displayPersonnel(
        list: List<Personnel>
    ) {

        personnelContainer.removeAllViews()


        // -------------------------------------------------
        // NO PERSONNEL
        // -------------------------------------------------

        if (list.isEmpty()) {

            txtNoPersonnel.visibility =
                View.VISIBLE

            return
        }


        txtNoPersonnel.visibility =
            View.GONE


        // -------------------------------------------------
        // DISPLAY CARDS
        // -------------------------------------------------

        for (person in list) {

            addPersonnelCard(
                person
            )
        }
    }


    // =====================================================
    // CREATE PERSONNEL CARD
    // =====================================================

    private fun addPersonnelCard(
        person: Personnel
    ) {

        // -------------------------------------------------
        // CARD
        // -------------------------------------------------

        val card =
            MaterialCardView(this)


        val cardParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )


        cardParams.bottomMargin =
            dpToPx(12)


        card.layoutParams =
            cardParams


        card.radius =
            dpToPx(18).toFloat()

        card.cardElevation =
            dpToPx(1).toFloat()

        card.strokeWidth =
            dpToPx(1)


        card.setStrokeColor(
            Color.parseColor(
                "#E3E7F5"
            )
        )


        card.setCardBackgroundColor(
            Color.WHITE
        )


        // =================================================
        // CARD CONTENT
        // =================================================

        val content =
            LinearLayout(this)


        content.orientation =
            LinearLayout.HORIZONTAL


        content.gravity =
            Gravity.CENTER_VERTICAL


        content.setPadding(
            dpToPx(16),
            dpToPx(16),
            dpToPx(16),
            dpToPx(16)
        )


        // =================================================
        // PERSON ICON CONTAINER
        // =================================================

        val iconContainer =
            LinearLayout(this)


        iconContainer.gravity =
            Gravity.CENTER


        iconContainer.background =
            getDrawable(
                R.drawable.bg_action_blue
            )


        val iconContainerParams =
            LinearLayout.LayoutParams(
                dpToPx(52),
                dpToPx(52)
            )


        // =================================================
        // PERSON ICON
        // =================================================

        val icon =
            ImageView(this)


        icon.setImageResource(
            R.drawable.ic_people
        )


        icon.setColorFilter(
            Color.WHITE
        )


        icon.setPadding(
            dpToPx(12),
            dpToPx(12),
            dpToPx(12),
            dpToPx(12)
        )


        iconContainer.addView(
            icon,
            LinearLayout.LayoutParams(
                dpToPx(52),
                dpToPx(52)
            )
        )


        content.addView(
            iconContainer,
            iconContainerParams
        )


        // =================================================
        // DETAILS CONTAINER
        // =================================================

        val details =
            LinearLayout(this)


        details.orientation =
            LinearLayout.VERTICAL


        details.setPadding(
            dpToPx(14),
            0,
            0,
            0
        )


        // =================================================
        // NAME
        // =================================================

        val name =
            TextView(this)


        name.text =
            person.name


        name.textSize =
            16f


        name.setTextColor(
            Color.parseColor(
                "#17215C"
            )
        )


        name.setTypeface(
            null,
            Typeface.BOLD
        )


        // =================================================
        // EMAIL
        // =================================================

        val email =
            TextView(this)


        email.text =
            person.email


        email.textSize =
            12f


        email.setTextColor(
            Color.parseColor(
                "#68739D"
            )
        )


        // =================================================
        // DEPARTMENT
        // =================================================

        val department =
            TextView(this)


        department.text =
            person.department


        department.textSize =
            12f


        department.setTextColor(
            Color.parseColor(
                "#68739D"
            )
        )


        // =================================================
        // ROLE
        // =================================================

        val role =
            TextView(this)


        role.text =
            person.role


        role.textSize =
            11f


        role.setTextColor(
            Color.parseColor(
                "#5938D0"
            )
        )


        role.setTypeface(
            null,
            Typeface.BOLD
        )


        // =================================================
        // ADD DETAILS
        // =================================================

        details.addView(
            name
        )


        details.addView(
            email,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                topMargin =
                    dpToPx(3)
            }
        )


        details.addView(
            department,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                topMargin =
                    dpToPx(2)
            }
        )


        details.addView(
            role,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {

                topMargin =
                    dpToPx(5)
            }
        )


        // =================================================
        // ADD DETAILS TO CARD
        // =================================================

        content.addView(
            details,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )


        // =================================================
        // ARROW
        // =================================================

        val arrow =
            TextView(this)


        arrow.text =
            "›"


        arrow.textSize =
            28f


        arrow.setTextColor(
            Color.parseColor(
                "#4B72D8"
            )
        )


        content.addView(
            arrow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )


        // =================================================
        // ADD CONTENT TO CARD
        // =================================================

        card.addView(
            content
        )


        // =================================================
        // ADD CARD TO SCREEN
        // =================================================

        personnelContainer.addView(
            card
        )
    }


    // =====================================================
    // DP TO PX
    // =====================================================

    private fun dpToPx(
        dp: Int
    ): Int {

        return (
                dp *
                        resources.displayMetrics.density
                ).toInt()
    }

}