package com.example.hackathonapp

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class AdminApplicationAdapter(
    private val apps: List<MyApplicationsActivity.ApplicationInfo>,
    private val hackathonId: String
) : RecyclerView.Adapter<AdminApplicationAdapter.AppViewHolder>() {

    inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameText: TextView = view.findViewById(R.id.applicantName)
        private val approveBtn: Button = view.findViewById(R.id.btnApprove)
        private val rejectBtn: Button = view.findViewById(R.id.btnReject)

        @SuppressLint("SetTextI18n")
        fun bind(app: MyApplicationsActivity.ApplicationInfo) {
            nameText.text = """
                ${app.lastName} ${app.firstName}
                ${app.phoneNumber}
                ${app.email}
            """.trimIndent()

            val documentId = "${app.hackathonId}-${app.userId}"
            approveBtn.setOnClickListener { updateStatus(documentId, "approved") }
            rejectBtn.setOnClickListener { updateStatus(documentId, "rejected") }
        }

        private fun updateStatus(appId: String, status: String) {
            FirebaseFirestore.getInstance().collection("applications")
                .document(appId)
                .update("status", status)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_application, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size
}
