package com.example.moneytracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DAILY_REMINDER = "com.example.moneytracker.DAILY_REMINDER"
        const val ACTION_WEEKLY_SUMMARY = "com.example.moneytracker.WEEKLY_SUMMARY"
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarma recibida: ${intent.action}")

        when (intent.action) {
            ACTION_DAILY_REMINDER -> {
                sendDailyReminder(context)
            }
            ACTION_WEEKLY_SUMMARY -> {
                sendWeeklySummary(context)
            }
        }
    }

    private fun sendDailyReminder(context: Context) {
        NotificationHelper.createNotificationChannel(context)

        val notification = androidx.core.app.NotificationCompat.Builder(
            context,
            "money_tracker_channel"
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("💰 Recordatorio Diario")
            .setContentText("¿Ya registraste tus gastos de hoy?")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        androidx.core.app.NotificationManagerCompat.from(context)
            .notify(1001, notification)

        Log.d(TAG, "Recordatorio diario enviado")
    }

    private fun sendWeeklySummary(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authManager = FirebaseAuthManager()
                val userId = authManager.getCurrentUserId()

                if (userId != null) {
                    val firestoreManager = FirestoreManager()
                    val expenses = firestoreManager.getExpensesByUser(userId)

                    val weekExpenses = expenses.filter { expense ->
                        isFromLastWeek(expense.date)
                    }

                    val totalExpenses = weekExpenses.size
                    val totalAmount = weekExpenses.sumOf { it.amount }
                    val topCategory = weekExpenses.groupBy { it.category }
                        .maxByOrNull { it.value.size }?.key ?: "N/A"

                    NotificationHelper.notifyDailySummary(
                        context,
                        totalExpenses,
                        totalAmount,
                        topCategory
                    )

                    Log.d(TAG, "Resumen semanal enviado: $totalExpenses gastos, $$totalAmount")
                } else {
                    Log.w(TAG, "No hay usuario logueado")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generando resumen", e)
            }
        }
    }

    private fun isFromLastWeek(dateStr: String): Boolean {
        return try {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val expenseDate = sdf.parse(dateStr) ?: return false

            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
            val weekAgo = calendar.time

            expenseDate.after(weekAgo)
        } catch (e: Exception) {
            false
        }
    }
}