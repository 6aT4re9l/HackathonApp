package com.example.hackathonapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
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
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
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
        return HackathonViewHolder(view, parent.context)
    }


    override fun onBindViewHolder(holder: HackathonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HackathonViewHolder(view: View, private val context: Context) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.hackathonTitle)
        private val image: ImageView = view.findViewById(R.id.hackathonImage)

        @SuppressLint("SetTextI18n")
        fun bind(hackathon: Hackathon) {
            title.text = hackathon.title
            val imageFile = File(hackathon.imageURL)
            if (imageFile.exists()) {
                Glide.with(itemView.context).load(imageFile).into(image)
            } else {
                image.setImageResource(R.drawable.placeholder)
            }

            itemView.setOnClickListener {
                val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_hackathon_details, null)
                val dialogBuilder = AlertDialog.Builder(context).setView(dialogView)

                val titleTextView = dialogView.findViewById<TextView>(R.id.dialogTitle)
                val descriptionTextView = dialogView.findViewById<TextView>(R.id.dialogDescription)
                val cityTextView = dialogView.findViewById<TextView>(R.id.dialogCity)
                val addressTextView = dialogView.findViewById<TextView>(R.id.dialogAddress)
                val mapView = dialogView.findViewById<MapView>(R.id.mapView)

                titleTextView.text = hackathon.title
                descriptionTextView.text = hackathon.description
                cityTextView.text = hackathon.city
                addressTextView.text = hackathon.address.ifBlank { "Адрес не указан" }

                if (hackathon.latitude != null && hackathon.longitude != null) {
                    val point = com.yandex.mapkit.geometry.Point(hackathon.latitude, hackathon.longitude)
                    mapView.map.move(CameraPosition(point, 15.0f, 0.0f, 0.0f))
                    mapView.map.mapObjects.addPlacemark(point)
                } else {
                    mapView.visibility = View.GONE
                }

                val dialog = dialogBuilder.create()

                dialog.setOnShowListener {
                    MapKitFactory.getInstance().onStart()
                    mapView.onStart()
                }

                dialog.setOnDismissListener {
                    mapView.onStop()
                    MapKitFactory.getInstance().onStop()
                }

                dialog.show()
            }

        }
    }

    @SuppressLint("SetTextI18n")
    fun showHackathonDialog(context: Context, hackathon: Hackathon) {
        val imageFile = File(hackathon.imageURL)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_hackathon_details, null)

        val imageView = dialogView.findViewById<ImageView>(R.id.dialogImage)
        val titleView = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val descView = dialogView.findViewById<TextView>(R.id.dialogDescription)
        val cityView = dialogView.findViewById<TextView>(R.id.dialogCity)
        val typeView = dialogView.findViewById<TextView>(R.id.dialogType)
        val participateButton = dialogView.findViewById<Button>(R.id.buttonParticipate)
        val addressTextView = dialogView.findViewById<TextView>(R.id.dialogAddress)
        val mapView = dialogView.findViewById<MapView>(R.id.mapView)

        titleView.text = hackathon.title
        descView.text = "Описание: ${hackathon.description}"
        cityView.text = "Город: ${hackathon.city}"
        typeView.text = "Тип: ${hackathon.type}"
        addressTextView.text = hackathon.address.ifBlank { "Адрес не указан" }

        if (imageFile.exists()) {
            Glide.with(context).load(imageFile).into(imageView)
        } else {
            imageView.setImageResource(R.drawable.placeholder)
        }

        if (hackathon.latitude != null && hackathon.longitude != null) {
            val point = com.yandex.mapkit.geometry.Point(hackathon.latitude!!, hackathon.longitude!!)
            mapView.map.move(CameraPosition(point, 15.0f, 0.0f, 0.0f))
            mapView.map.mapObjects.addPlacemark(point)
        } else {
            mapView.visibility = View.GONE
        }


        participateButton.visibility = View.GONE // отключаем кнопку "Участвовать"

        AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("ОК", null)
            .show()
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
