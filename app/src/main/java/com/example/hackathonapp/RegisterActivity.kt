package com.example.hackathonapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

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
        val registerButton = findViewById<Button>(R.id.reg_button)

        user = User()

        registerButton.setOnClickListener {
            Log.d("RegisterActivity", "Кнопка регистрации нажата")
            val firstName = firstNameInput.text.toString().trim()
            val lastName = lastNameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            } else {
                registerUser(firstName, lastName, email, phone)
            }
        }
    }

    private fun registerUser(name: String, lastName: String, eMail: String, phone: String) {
        auth.createUserWithEmailAndPassword(eMail, "defaultPassword123") // Используй реальный пароль
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        addDataToFirestore(userId, name, lastName, eMail, phone)
                    }
                } else {
                    Toast.makeText(this, "Ошибка регистрации: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun addDataToFirestore(userId: String, name: String, lastName: String, eMail: String, phone: String) {
        user = User(name, lastName, eMail, phone)

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
