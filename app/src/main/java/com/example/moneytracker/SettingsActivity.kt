package com.example.moneytracker

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.moneytracker.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val PREFS_NAME = "MoneyTrackerPrefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCurrency = prefs.getString("currency", "MXN")
        val notificationsEnabled = prefs.getBoolean("notifications", false)

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.currencies,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spCurrency.adapter = adapter

        val position = adapter.getPosition(savedCurrency)
        binding.spCurrency.setSelection(position)
        binding.cbNotifications.isChecked = notificationsEnabled

        binding.btnSaveSettings.setOnClickListener {
            val editor = prefs.edit()
            editor.putString("currency", binding.spCurrency.selectedItem.toString())
            editor.putBoolean("notifications", binding.cbNotifications.isChecked)
            editor.apply()

            Toast.makeText(this, "Configuracion guardada", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
