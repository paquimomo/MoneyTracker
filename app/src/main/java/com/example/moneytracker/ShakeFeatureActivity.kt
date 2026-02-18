package com.example.moneytracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.moneytracker.databinding.ActivityShakeFeatureBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ShakeFeatureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShakeFeatureBinding
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var authManager: FirebaseAuthManager
    private lateinit var firestoreManager: FirestoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShakeFeatureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Agita para Sorpresa"

        authManager = FirebaseAuthManager()
        firestoreManager = FirestoreManager()

        setupSensor()
        updateShakeCounter()
    }

    private fun setupSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer != null) {
            shakeDetector = ShakeDetector {
                onDeviceShaken()
            }

            binding.tvSensorStatus.text = "✅ Acelerómetro detectado"
            binding.tvSensorStatus.setTextColor(getColor(R.color.success))
        } else {
            binding.tvSensorStatus.text = "❌ Acelerómetro no disponible"
            binding.tvSensorStatus.setTextColor(getColor(R.color.error))
            Toast.makeText(
                this,
                "Tu dispositivo no tiene acelerómetro",
                Toast.LENGTH_LONG
            ).show()
        }

        binding.btnTestShake.setOnClickListener {
            onDeviceShaken()
        }
    }

    private fun onDeviceShaken() {
        val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
        binding.ivShakeIcon.startAnimation(shake)

        val prefs = getSharedPreferences("shake_prefs", Context.MODE_PRIVATE)
        val currentCount = prefs.getInt("shake_count", 0)
        prefs.edit().putInt("shake_count", currentCount + 1).apply()
        updateShakeCounter()

        showRandomExpense()
    }

    private fun showRandomExpense() {
        val userId = authManager.getCurrentUserId()

        if (userId != null) {
            lifecycleScope.launch {
                try {
                    val expenses = firestoreManager.getExpensesByUser(userId)

                    if (expenses.isNotEmpty()) {
                        val randomExpense = expenses.random()

                        val message = buildString {
                            append("💰 Gasto Aleatorio\n\n")
                            append("Descripción: ${randomExpense.description}\n")
                            append("Monto: $${String.format("%.2f", randomExpense.amount)}\n")
                            append("Categoría: ${randomExpense.category}\n")
                            append("Fecha: ${randomExpense.date}\n")
                            append("Hora: ${randomExpense.time}")
                        }

                        AlertDialog.Builder(this@ShakeFeatureActivity)
                            .setTitle("¡Sorpresa!")
                            .setMessage(message)
                            .setPositiveButton("Cerrar", null)
                            .show()
                    } else {
                        Toast.makeText(
                            this@ShakeFeatureActivity,
                            "No tienes gastos registrados aún",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@ShakeFeatureActivity,
                        "Error cargando gastos: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(
                this@ShakeFeatureActivity,
                "Debes iniciar sesión",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateShakeCounter() {
        val prefs = getSharedPreferences("shake_prefs", Context.MODE_PRIVATE)
        val count = prefs.getInt("shake_count", 0)
        binding.tvShakeCount.text = "Agitaciones detectadas: $count"
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { acc ->
            sensorManager.registerListener(
                shakeDetector,
                acc,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(shakeDetector)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}