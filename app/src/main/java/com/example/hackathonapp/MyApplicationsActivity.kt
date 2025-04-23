package com.example.hackathonapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import java.io.File
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyApplicationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ApplicationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.my_applications_activity)

        recyclerView = findViewById(R.id.recyclerViewApplications)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ApplicationAdapter(
            onClick = { app ->
                showHackathonDetails(app.hackathonId)
            },
            onCancel = {
                loadMyApplications()
            }
        )
        recyclerView.adapter = adapter

        loadMyApplications()
    }

    private fun loadMyApplications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("applications")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val apps = result.mapNotNull { doc ->
                    doc.toObject(ApplicationInfo::class.java)
                }
                adapter.submitList(apps)
            }
    }

    data class ApplicationInfo(
        val hackathonId: String = "",
        val hackathonTitle: String = "",
        val status: String = ""
    )

    class ApplicationAdapter(
        private val onClick: (ApplicationInfo) -> Unit,
        private val onCancel: () -> Unit
    ) : ListAdapter<ApplicationInfo, ApplicationAdapter.ApplicationViewHolder>(DiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_application, parent, false)
            return ApplicationViewHolder(view)
        }

        override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class ApplicationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val title: TextView = view.findViewById(R.id.textTitle)
            private val status: TextView = view.findViewById(R.id.textStatus)
            private val cancelButton: Button = view.findViewById(R.id.buttonCancel)

            fun bind(app: ApplicationInfo) {
                title.text = app.hackathonTitle
                when (app.status) {
                    "pending" -> {
                        status.text = "На рассмотрении"
                        status.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                    }
                    "approved" -> {
                        status.text = "Одобрено"
                        status.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                    }
                    "rejected" -> {
                        status.text = "Отклонено"
                        status.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                    }
                    else -> {
                        status.text = "Неизвестно"
                        status.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
                    }
                }

                itemView.setOnClickListener {
                    onClick(app)
                }

                if (app.status == "pending") {
                    cancelButton.visibility = View.VISIBLE
                    cancelButton.setOnClickListener {
                        AlertDialog.Builder(itemView.context)
                            .setTitle("Подтверждение")
                            .setMessage("Вы уверены, что хотите отозвать заявку на хакатон \"${app.hackathonTitle}\"?")
                            .setPositiveButton("Отозвать") { _, _ ->
                                val docId = "${app.hackathonId}-${FirebaseAuth.getInstance().currentUser?.uid}"
                                FirebaseFirestore.getInstance().collection("applications")
                                    .document(docId)
                                    .delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(itemView.context, "Заявка отозвана", Toast.LENGTH_SHORT).show()
                                        onCancel()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(itemView.context, "Ошибка при отзыве", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .setNegativeButton("Отмена", null)
                            .show()
                    }

                } else {
                    cancelButton.visibility = View.GONE
                }
            }
        }

        class DiffCallback : DiffUtil.ItemCallback<ApplicationInfo>() {
            override fun areItemsTheSame(oldItem: ApplicationInfo, newItem: ApplicationInfo): Boolean {
                return oldItem.hackathonId == newItem.hackathonId
            }

            override fun areContentsTheSame(oldItem: ApplicationInfo, newItem: ApplicationInfo): Boolean {
                return oldItem == newItem
            }
        }
    }


    // При нажатии на заявку, открывается карточка хакатона
    private fun showHackathonDetails(hackathonId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("hackathons").document(hackathonId).get()
            .addOnSuccessListener { doc ->
                val hackathon = doc.toObject(Hackathon::class.java) ?: return@addOnSuccessListener

                val dialogView = layoutInflater.inflate(R.layout.dialog_hackathon_details, null)
                val titleView = dialogView.findViewById<TextView>(R.id.dialogTitle)
                val descView = dialogView.findViewById<TextView>(R.id.dialogDescription)
                val cityView = dialogView.findViewById<TextView>(R.id.dialogCity)
                val typeView = dialogView.findViewById<TextView>(R.id.dialogType)
                val imageView = dialogView.findViewById<ImageView>(R.id.dialogImage)

                titleView.text = hackathon.title
                descView.text = "Описание: ${hackathon.description}"
                cityView.text = "Город: ${hackathon.city}"
                typeView.text = "Тип: ${hackathon.type}"

                val imageFile = File(hackathon.imageURL)
                if (imageFile.exists()) {
                    Glide.with(this).load(imageFile).into(imageView)
                } else {
                    imageView.setImageResource(R.drawable.placeholder)
                }

                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setPositiveButton("ОК", null)
                    .show()
            }
    }

}


