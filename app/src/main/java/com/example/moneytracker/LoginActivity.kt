package com.example.moneytracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.moneytracker.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authManager: FirebaseAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = FirebaseAuthManager()

        if (authManager.isUserLoggedIn()) {
            val userId = authManager.getCurrentUserId()
            if (userId != null) {
                saveUserSession(userId)
                navigateToMain()
                return
            }
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun loginUser() {
        val email = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        binding.tilUsername?.error = null
        binding.tilPassword?.error = null

        if (email.isEmpty()) {
            binding.tilUsername?.error = "Ingrese su email"
            binding.etUsername.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.tilPassword?.error = "Ingrese su contraseña"
            binding.etPassword.requestFocus()
            return
        }

        showLoading(true)
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            val (user, message) = authManager.loginUser(email, password)

            showLoading(false)
            binding.btnLogin.isEnabled = true

            if (user != null) {
                // Login exitoso
                Toast.makeText(
                    this@LoginActivity,
                    "¡Bienvenido ${user.username}!",
                    Toast.LENGTH_SHORT
                ).show()

                saveUserSession(user.id, user.username, user.email)

                navigateToMain()
            } else {
                binding.tilPassword?.error = message
                Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserSession(userId: String, username: String = "", email: String = "") {
        val prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("user_id", userId)
            putString("username", username)
            putString("email", email)
            apply()
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun showLoading(show: Boolean) {
        binding.btnLogin.text = if (show) "Iniciando sesión..." else "Iniciar Sesión"
    }
}