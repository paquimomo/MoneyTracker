package com.example.moneytracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.example.moneytracker.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import android.os.Build
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authManager: FirebaseAuthManager
    private lateinit var fileManager: FileManager
    private lateinit var firestoreManager: FirestoreManager
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = FirebaseAuthManager()

        if (!authManager.isUserLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        fileManager = FileManager(this)
        firestoreManager = FirestoreManager()

        NotificationHelper.createNotificationChannel(this)

        requestNotificationPermission()

        setupNavigationDrawer()

        loadUserData()

        setupButtons()
        loadUserStats()
    }

    private fun setupNavigationDrawer() {
        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener(this)

        binding.navigationView.setCheckedItem(R.id.nav_dashboard)
    }

    private fun loadUserData() {
        val prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val username = prefs.getString("username", null)
        val email = prefs.getString("email", null)

        if (username.isNullOrEmpty() || email.isNullOrEmpty()) {
            android.util.Log.d("MainActivity", "No hay username/email en SharedPreferences, cargando desde Firebase...")

            lifecycleScope.launch {
                try {
                    val userData = authManager.getCurrentUserData()

                    if (userData != null) {
                        android.util.Log.d("MainActivity", "Usuario obtenido de Firebase: ${userData.username}")

                        prefs.edit().apply {
                            putString("username", userData.username)
                            putString("email", userData.email)
                            apply()
                        }

                        updateUserUI(userData.username, userData.email)
                    } else {
                        android.util.Log.e("MainActivity", "No se pudo obtener datos de Firebase")
                        updateUserUI("Usuario", "email@ejemplo.com")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error cargando datos de usuario", e)
                    updateUserUI("Usuario", "email@ejemplo.com")
                }
            }
        } else {
            android.util.Log.d("MainActivity", "Usando datos de SharedPreferences: $username")
            updateUserUI(username, email)
        }
    }

    private fun updateUserUI(username: String, email: String) {
        binding.tvWelcome.text = "¡Hola, $username!"

        val headerView = binding.navigationView.getHeaderView(0)
        val navHeaderUsername = headerView.findViewById<TextView>(R.id.navHeaderUsername)
        val navHeaderEmail = headerView.findViewById<TextView>(R.id.navHeaderEmail)

        navHeaderUsername.text = username
        navHeaderEmail.text = email
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationHelper.hasNotificationPermission(this)) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.nav_dashboard -> {
            }
            R.id.nav_expenses -> {
                startActivity(Intent(this, ExpenseListActivity::class.java))
            }
            R.id.nav_categories -> {
                startActivity(Intent(this, CategoriesActivity::class.java))
            }
            R.id.nav_map -> {
                startActivity(Intent(this, MapExpensesActivity::class.java))
            }
            R.id.nav_notifications -> {
                startActivity(Intent(this, NotificationsActivity::class.java))
            }
            R.id.nav_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            R.id.nav_about -> {
                showAboutDialog()
            }
            R.id.nav_logout -> {
                confirmLogout()
            }
            R.id.nav_alarms -> {
                startActivity(Intent(this, AlarmsActivity::class.java))
            }
            R.id.nav_shake -> {
                startActivity(Intent(this, ShakeFeatureActivity::class.java))
            }
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun setupButtons() {
        binding.btnAdd.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        binding.btnList.setOnClickListener {
            startActivity(Intent(this, ExpenseListActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun loadUserStats() {
        val userId = authManager.getCurrentUserId()

        if (userId != null) {
            lifecycleScope.launch {
                try {
                    val expenses = firestoreManager.getExpensesByUser(userId)

                    val totalExpenses = expenses.size
                    val totalAmount = expenses.sumOf { it.amount }
                    val imagesCount = expenses.count { !it.imagePath.isNullOrEmpty() }

                    binding.tvTotalExpenses.text = "$totalExpenses gastos registrados"
                    binding.tvTotalAmount.text = String.format("Total: $%.2f", totalAmount)
                    binding.tvImagesCount.text = "$imagesCount fotos guardadas"

                    val topCategory = if (expenses.isNotEmpty()) {
                        expenses.groupBy { it.category }
                            .maxByOrNull { it.value.size }?.key ?: "N/A"
                    } else {
                        "N/A"
                    }
                    binding.tvTopCategory.text = topCategory

                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error cargando estadísticas", e)

                    binding.tvTotalExpenses.text = "0 gastos registrados"
                    binding.tvTotalAmount.text = "Total: $0.00"
                    binding.tvImagesCount.text = "0 fotos guardadas"
                    binding.tvTopCategory.text = "N/A"
                }
            }
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Acerca de MoneyTracker")
            .setMessage("""
                MoneyTracker v3.0
                
                Firebase Authentication
                Base de datos local SQLite
                Navigation Drawer
                Notificaciones
                Captura de fotos
                Categorías personalizadas
                
                Desarrollada en Kotlin.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Seguro que deseas cerrar tu sesión?")
            .setPositiveButton("Sí") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun logout() {
        authManager.logout()

        val prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onResume() {
        super.onResume()
        loadUserStats()

        loadUserData()
    }
}