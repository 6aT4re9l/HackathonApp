package com.example.hackathonapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HackathonAdapter(
    private val isAdmin: Boolean,
    private val onDelete: (Hackathon) -> Unit,
    private val onEdit: (Hackathon) -> Unit
) : ListAdapter<Hackathon, HackathonAdapter.HackathonViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HackathonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hackathon, parent, false)
        return HackathonViewHolder(view)
    }

    override fun onBindViewHolder(holder: HackathonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HackathonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.hackathonTitle)
        private val image: ImageView = view.findViewById(R.id.hackathonImage)

        fun bind(hackathon: Hackathon) {
            title.text = hackathon.title
            val imageFile = File(hackathon.imageURL)
            if (imageFile.exists()) {
                Glide.with(itemView.context).load(imageFile).into(image)
            } else {
                image.setImageResource(R.drawable.placeholder)
            }

            itemView.setOnClickListener {
                val context = itemView.context
                val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_hackathon_details, null)

                val imageView = dialogView.findViewById<ImageView>(R.id.dialogImage)
                val titleView = dialogView.findViewById<TextView>(R.id.dialogTitle)
                val descView = dialogView.findViewById<TextView>(R.id.dialogDescription)
                val cityView = dialogView.findViewById<TextView>(R.id.dialogCity)
                val typeView = dialogView.findViewById<TextView>(R.id.dialogType)
                val participateButton = dialogView.findViewById<Button>(R.id.buttonParticipate)

                titleView.text = hackathon.title
                descView.text = "Описание: ${hackathon.description}"
                cityView.text = "Город: ${hackathon.city}"
                typeView.text = "Тип: ${hackathon.type}"

                if (imageFile.exists()) {
                    Glide.with(context).load(imageFile).into(imageView)
                } else {
                    imageView.setImageResource(R.drawable.placeholder)
                }

                if (!isAdmin) {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid

                    val participateButton = dialogView.findViewById<Button>(R.id.buttonParticipate)
                    participateButton.visibility = View.VISIBLE

                    FirebaseFirestore.getInstance()
                        .collection("applications")
                        .document("${hackathon.id}-$userId")
                        .get()
                        .addOnSuccessListener { doc ->
                            val status = doc.getString("status")
                            if (status == "pending" || status == "approved") {
                                participateButton.isEnabled = false
                                participateButton.text = "Заявка отправлена"
                            } else {
                                participateButton.isEnabled = true
                                participateButton.setOnClickListener {
                                    participateButton.isEnabled = false
                                    submitApplication(itemView.context, hackathon.id, hackathon.title)
                                    participateButton.text = "Заявка отправлена"
                                }
                            }
                        }
                }

                val dialogBuilder = AlertDialog.Builder(context)
                    .setView(dialogView)

                if (isAdmin) {
                    dialogBuilder
                        .setPositiveButton("Редактировать") { _, _ -> onEdit(hackathon) }
                        .setNegativeButton("Удалить") { _, _ -> onDelete(hackathon) }
                        .setNeutralButton("ОК", null)
                } else {
                    dialogBuilder.setPositiveButton("ОК", null)
                }

                dialogBuilder.create().show()
            }
        }
    }

    private fun submitApplication(context: Context, hackathonId: String, hackathonTitle: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            val firstName = doc.getString("firstName") ?: ""
            val lastName = doc.getString("lastName") ?: ""
            val email = doc.getString("email") ?: ""
            val phone = doc.getString("phoneNumber") ?: ""

            val application = mapOf(
                "hackathonId" to hackathonId,
                "userId" to userId,
                "firstName" to firstName,
                "lastName" to lastName,
                "email" to email,
                "phone" to phone,
                "status" to "pending",
                "timestamp" to SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date()),
                "hackathonTitle" to hackathonTitle
            )

            db.collection("applications")
                .document("$hackathonId-$userId")
                .set(application)
                .addOnSuccessListener {
                    Toast.makeText(context, "Заявка отправлена", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Ошибка при отправке заявки", Toast.LENGTH_SHORT).show()
                }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Hackathon>() {
        override fun areItemsTheSame(oldItem: Hackathon, newItem: Hackathon) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Hackathon, newItem: Hackathon) = oldItem == newItem
    }
}
