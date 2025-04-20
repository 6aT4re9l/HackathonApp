package com.example.hackathonapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.TextView

class RegisterActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var user: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_activity)

        // Инициализация Firestore и Firebase Auth
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val firstNameInput = findViewById<EditText>(R.id.user_firstName)
        val lastNameInput = findViewById<EditText>(R.id.user_lastName)
        val emailInput = findViewById<EditText>(R.id.user_email)
        val phoneInput = findViewById<EditText>(R.id.user_phone_number)
        val passInput = findViewById<EditText>(R.id.user_pass)
        val passInput1 = findViewById<EditText>(R.id.user_pass1)
        val registerButton = findViewById<Button>(R.id.reg_button)
        val loginText = findViewById<TextView>(R.id.goLoginDisplay)

        loginText.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        user = User()

        registerButton.setOnClickListener {
            val firstName = firstNameInput.text.toString().trim()
            val lastName = lastNameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val password = passInput.text.toString().trim()
            val password1 = passInput1.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()
                || password1.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            } else if (password != password1) {
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
            } else {
                registerUser(firstName, lastName, email, phone, password)
            }
        }
    }

    private fun registerUser(name: String, lastName: String, eMail: String, phone: String, password: String) {
        auth.createUserWithEmailAndPassword(eMail, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        addDataToFireStore(userId, name, lastName, eMail, phone, password)
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Ошибка регистрации: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun addDataToFireStore(userId: String, name: String, lastName: String, eMail: String, phone: String, password: String) {
        user = User(name, lastName, eMail, phone, password, isAdmin = false)

        // Сохраняем данные с UID пользователя
        firestore.collection("users").document(userId).set(user)
            .addOnSuccessListener {
                Toast.makeText(this@RegisterActivity, "Пользователь сохранён!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(this@RegisterActivity, "Ошибка: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
