package com.example.moneytracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.moneytracker.databinding.ActivityNotificationsBinding

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Notificaciones"

        binding.btnTestExpenseSaved.setOnClickListener {
            NotificationHelper.notifyExpenseSaved(this, "Prueba de gasto", 150.0)
        }

        binding.btnTestLimitWarning.setOnClickListener {
            NotificationHelper.notifyLimitWarning(this, "Alimentación", 4200.0, 5000.0, 84)
        }

        binding.btnTestDailySummary.setOnClickListener {
            NotificationHelper.notifyDailySummary(this, 5, 1250.50, "Transporte")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}