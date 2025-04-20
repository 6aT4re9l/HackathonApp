package com.example.hackathonapp

import com.google.firebase.firestore.PropertyName

data class User(
    var firstName: String = "",
    var lastName: String = "",
    var email: String = "",
    var phoneNumber: String = "",
    var password: String="",
    @get:PropertyName("isAdmin") @set:PropertyName("isAdmin")
    var isAdmin: Boolean = false
)
