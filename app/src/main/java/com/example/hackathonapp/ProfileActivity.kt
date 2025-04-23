package com.example.hackathonapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hackathonapp.MyApplicationsActivity.ApplicationAdapter
import com.example.hackathonapp.MyApplicationsActivity.ApplicationInfo
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {
    private lateinit var adapter: MyApplicationsActivity.ApplicationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_activity)

        val emptyMessage = findViewById<TextView>(R.id.emptyListMessage)
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBarProfile)
        toolbar.setNavigationOnClickListener { finish() }

        val firstNameText = findViewById<TextView>(R.id.user_firstName)
        val lastNameText = findViewById<TextView>(R.id.user_lastName)
        val phoneText = findViewById<TextView>(R.id.profilePhone)
        val emailText = findViewById<TextView>(R.id.profileEmail)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewApplications)

        adapter = MyApplicationsActivity.ApplicationAdapter(
            onClick = { app ->
                fetchAndShowHackathon(app.hackathonId)
            },
            onCancel = {
                loadMyApplications(adapter)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                firstNameText.text = doc.getString("firstName") ?: ""
                lastNameText.text = doc.getString("lastName") ?: ""
                phoneText.text = "Телефон: ${doc.getString("phoneNumber") ?: ""}"
                emailText.text = "Email: ${doc.getString("email") ?: ""}"
            }

        loadMyApplications(adapter)
    }

    private fun loadMyApplications(adapter: ApplicationAdapter) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("applications")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val emptyMessage = findViewById<TextView>(R.id.emptyListMessage)
                val apps = result.mapNotNull { it.toObject(ApplicationInfo::class.java) }
                if (apps.isEmpty()) {
                    emptyMessage.visibility = View.VISIBLE
                } else {
                    emptyMessage.visibility = View.GONE
                    adapter.submitList(apps)
                }
            }
    }

    private fun fetchAndShowHackathon(hackathonId: String) {
        FirebaseFirestore.getInstance().collection("hackathons")
            .document(hackathonId)
            .get()
            .addOnSuccessListener { doc ->
                val hackathon = doc.toObject(Hackathon::class.java) ?: return@addOnSuccessListener
                val adapter = HackathonAdapter(
                    isAdmin = false,
                    onDelete = {},
                    onEdit = {}
                )
                adapter.showHackathonDialog(this, hackathon)
            }
    }
}
