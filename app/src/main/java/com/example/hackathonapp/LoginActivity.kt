package com.example.hackathonapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth// Объект аутентификации Firebase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        auth = FirebaseAuth.getInstance()

        val loginInput = findViewById<EditText>(R.id.loginText)
        val passInput = findViewById<EditText>(R.id.loginPass)
        val button = findViewById<Button>(R.id.login_button)
        val goRegDisplay = findViewById<TextView>(R.id.goRegisterDisplay)


        goRegDisplay.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java) // ✅ Правильно
            startActivity(intent)
        }

        button.setOnClickListener {
            val login = loginInput.text.toString().trim()
            val password = passInput.text.toString().trim()

            if (login.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else {
                auth.signInWithEmailAndPassword(login, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Вход выполнен!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Ошибка: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }
    }
}
