package com.example.moneytracker

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.moneytracker.databinding.ActivityAlarmsBinding
import java.util.*

class AlarmsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmsBinding
    private var dailyReminderHour = 20
    private var dailyReminderMinute = 0
    private var weeklySummaryDay = Calendar.SUNDAY
    private var weeklySummaryHour = 9

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Alarmas y Recordatorios"

        loadSavedPreferences()
        setupSwitches()
        setupButtons()
        updateTimeLabels()
    }

    private fun loadSavedPreferences() {
        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

        binding.switchDailyReminder.isChecked = prefs.getBoolean("daily_reminder_enabled", false)
        binding.switchWeeklySummary.isChecked = prefs.getBoolean("weekly_summary_enabled", false)

        dailyReminderHour = prefs.getInt("daily_reminder_hour", 20)
        dailyReminderMinute = prefs.getInt("daily_reminder_minute", 0)
        weeklySummaryDay = prefs.getInt("weekly_summary_day", Calendar.SUNDAY)
        weeklySummaryHour = prefs.getInt("weekly_summary_hour", 9)
    }

    private fun setupSwitches() {
        binding.switchDailyReminder.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableDailyReminder()
            } else {
                disableDailyReminder()
            }
        }

        binding.switchWeeklySummary.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableWeeklySummary()
            } else {
                disableWeeklySummary()
            }
        }
    }

    private fun setupButtons() {
        binding.btnSetDailyTime.setOnClickListener {
            showTimePickerForDaily()
        }

        binding.btnSetWeeklyTime.setOnClickListener {
            showTimePickerForWeekly()
        }

        binding.btnTestDaily.setOnClickListener {
            testDailyReminder()
        }

        binding.btnTestWeekly.setOnClickListener {
            testWeeklySummary()
        }
    }

    private fun showTimePickerForDaily() {
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                dailyReminderHour = hourOfDay
                dailyReminderMinute = minute
                updateTimeLabels()
                savePreferences()

                if (binding.switchDailyReminder.isChecked) {
                    AlarmHelper.scheduleDailyReminder(this, dailyReminderHour, dailyReminderMinute)
                    Toast.makeText(
                        this,
                        "Recordatorio actualizado para las ${String.format("%02d:%02d", dailyReminderHour, dailyReminderMinute)}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            dailyReminderHour,
            dailyReminderMinute,
            true
        ).show()
    }

    private fun showTimePickerForWeekly() {
        TimePickerDialog(
            this,
            { _, hourOfDay, _ ->
                weeklySummaryHour = hourOfDay
                updateTimeLabels()
                savePreferences()

                if (binding.switchWeeklySummary.isChecked) {
                    AlarmHelper.scheduleWeeklySummary(this, weeklySummaryDay, weeklySummaryHour)
                    Toast.makeText(
                        this,
                        "Resumen actualizado para las ${String.format("%02d:00", weeklySummaryHour)}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            weeklySummaryHour,
            0,
            true
        ).show()
    }

    private fun updateTimeLabels() {
        binding.tvDailyTime.text = String.format("%02d:%02d", dailyReminderHour, dailyReminderMinute)
        binding.tvWeeklyTime.text = "Domingos a las ${String.format("%02d:00", weeklySummaryHour)}"
    }

    private fun enableDailyReminder() {
        AlarmHelper.scheduleDailyReminder(this, dailyReminderHour, dailyReminderMinute)
        savePreferences()
        Toast.makeText(
            this,
            "Recordatorio diario activado para las ${String.format("%02d:%02d", dailyReminderHour, dailyReminderMinute)}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun disableDailyReminder() {
        AlarmHelper.cancelDailyReminder(this)
        savePreferences()
        Toast.makeText(this, "Recordatorio diario desactivado", Toast.LENGTH_SHORT).show()
    }

    private fun enableWeeklySummary() {
        AlarmHelper.scheduleWeeklySummary(this, weeklySummaryDay, weeklySummaryHour)
        savePreferences()
        Toast.makeText(
            this,
            "Resumen semanal activado para domingos a las ${String.format("%02d:00", weeklySummaryHour)}",
            Toast.LENGTH_SHORT).show()
    }

    private fun disableWeeklySummary() {
        AlarmHelper.cancelWeeklySummary(this)
        savePreferences()
        Toast.makeText(this, "Resumen semanal desactivado", Toast.LENGTH_SHORT).show()
    }

    private fun savePreferences() {
        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("daily_reminder_enabled", binding.switchDailyReminder.isChecked)
            putBoolean("weekly_summary_enabled", binding.switchWeeklySummary.isChecked)
            putInt("daily_reminder_hour", dailyReminderHour)
            putInt("daily_reminder_minute", dailyReminderMinute)
            putInt("weekly_summary_day", weeklySummaryDay)
            putInt("weekly_summary_hour", weeklySummaryHour)
            apply()
        }
    }

    private fun testDailyReminder() {
        val notification = androidx.core.app.NotificationCompat.Builder(
            this,
            "money_tracker_channel"
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💰 Recordatorio Diario (PRUEBA)")
            .setContentText("¿Ya registraste tus gastos de hoy?")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        androidx.core.app.NotificationManagerCompat.from(this)
            .notify(9001, notification)

        Toast.makeText(this, "Notificación de prueba enviada", Toast.LENGTH_SHORT).show()
    }

    private fun testWeeklySummary() {
        NotificationHelper.notifyDailySummary(this, 5, 1250.50, "Alimentación")
        Toast.makeText(this, "Resumen de prueba enviado", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}