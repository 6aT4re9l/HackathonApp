package com.example.hackathonapp

import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.TextView


class ProfileActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_activity)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBarProfile)
        toolbar.setNavigationOnClickListener {
            finish() // просто закрывает текущую активность и возвращает на предыдущую
        }

        val firstNameText = findViewById<TextView>(R.id.user_firstName)
        val lastNameText = findViewById<TextView>(R.id.user_lastName)
        val phoneText = findViewById<TextView>(R.id.profilePhone)
        val emailText = findViewById<TextView>(R.id.profileEmail)

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("firstName") ?: ""
                val lastName = doc.getString("lastName") ?: ""
                val phone = doc.getString("phoneNumber") ?: ""
                val email = doc.getString("email") ?: ""

                firstNameText.text = "$name"
                lastNameText.text = "$lastName"
                phoneText.text = "Телефон: $phone"
                emailText.text = "Email: $email"
            }
    }
}
