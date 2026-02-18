package com.example.moneytracker

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.moneytracker.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authManager: FirebaseAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = FirebaseAuthManager()

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.tvGoToLogin.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun registerUser() {
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        binding.tilUsername?.error = null
        binding.tilEmail?.error = null
        binding.tilPassword?.error = null
        binding.tilConfirmPassword?.error = null

        if (username.isEmpty()) {
            binding.tilUsername?.error = "Ingrese un nombre de usuario"
            binding.etUsername.requestFocus()
            return
        }

        if (username.length < 3) {
            binding.tilUsername?.error = "El usuario debe tener al menos 3 caracteres"
            binding.etUsername.requestFocus()
            return
        }

        if (email.isEmpty()) {
            binding.tilEmail?.error = "Ingrese un email"
            binding.etEmail.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail?.error = "Email inválido"
            binding.etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.tilPassword?.error = "Ingrese una contraseña"
            binding.etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            binding.tilPassword?.error = "La contraseña debe tener al menos 6 caracteres"
            binding.etPassword.requestFocus()
            return
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword?.error = "Confirme su contraseña"
            binding.etConfirmPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            binding.tilConfirmPassword?.error = "Las contraseñas no coinciden"
            binding.etConfirmPassword.requestFocus()
            return
        }

        showLoading(true)
        binding.btnRegister.isEnabled = false

        lifecycleScope.launch {
            try {
                val emailExists = authManager.isEmailExists(email)
                if (emailExists) {
                    showLoading(false)
                    binding.btnRegister.isEnabled = true
                    binding.tilEmail?.error = "Este email ya está registrado"
                    return@launch
                }

                val user = User(
                    username = username,
                    email = email,
                    password = password
                )

                val (success, message) = authManager.registerUser(user)

                if (success) {
                    val (loggedUser, loginMessage) = authManager.loginUser(email, password)

                    if (loggedUser != null) {
                        val prefs = getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("user_id", loggedUser.id)
                            putString("username", loggedUser.username)
                            putString("email", loggedUser.email)
                            apply()
                        }

                        runOnUiThread {
                            Toast.makeText(
                                this@RegisterActivity,
                                "¡Bienvenido ${loggedUser.username}!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        val intent = android.content.Intent(this@RegisterActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@RegisterActivity,
                                "Usuario creado. Inicia sesión.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        finish()
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                } else {
                    showLoading(false)
                    binding.btnRegister.isEnabled = true

                    runOnUiThread {
                        Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_LONG).show()

                        when {
                            message.contains("username", ignoreCase = true) -> {
                                binding.tilUsername?.error = message
                            }
                            message.contains("email", ignoreCase = true) -> {
                                binding.tilEmail?.error = message
                            }
                            else -> {
                                binding.tilPassword?.error = message
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                showLoading(false)
                binding.btnRegister.isEnabled = true

                runOnUiThread {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.btnRegister.text = if (show) "Registrando..." else "Registrarse"
    }
}