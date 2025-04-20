package com.example.hackathonapp

data class Hackathon(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageURL: String = "",
    val city: String = "",
    val type: String = "",
    val prizeFund: Int = 0
)
