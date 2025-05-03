package com.example.hackathonapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdminHackathonAdapter(
    private val items: List<Hackathon>,
    private val onClick: (Hackathon) -> Unit
) : RecyclerView.Adapter<AdminHackathonAdapter.HackathonViewHolder>() {

    inner class HackathonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.hackathonTitle)

        fun bind(hackathon: Hackathon) {
            title.text = hackathon.title
            itemView.setOnClickListener { onClick(hackathon) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HackathonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_hackathon, parent, false)
        return HackathonViewHolder(view)
    }

    override fun onBindViewHolder(holder: HackathonViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
