package com.example.moneytracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneytracker.databinding.ActivityExpenseListBinding
import kotlinx.coroutines.launch

class ExpenseListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseListBinding
    private lateinit var adapter: ExpenseAdapter
    private lateinit var authManager: FirebaseAuthManager
    private lateinit var fileManager: FileManager
    private lateinit var firestoreManager: FirestoreManager
    private val expenses = mutableListOf<Expense>()
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mis Gastos"

        authManager = FirebaseAuthManager()
        fileManager = FileManager(this)
        firestoreManager = FirestoreManager()

        userId = authManager.getCurrentUserId()
        val filterCategoryName = intent.getStringExtra("filter_category_name")
        if (filterCategoryName != null) {
            supportActionBar?.title = "Gastos - $filterCategoryName"
        }

        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupRecyclerView()

        binding.fabAddExpense.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    override fun onResume() {
        super.onResume()
        loadExpenses()
    }

    private fun setupRecyclerView() {
        adapter = ExpenseAdapter(
            expenses = expenses,
            fileManager = fileManager,
            onItemClick = { expense -> showExpenseDetails(expense) },
            onItemLongClick = { expense -> showExpenseOptions(expense) }
        )

        binding.rvExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvExpenses.adapter = adapter
    }

    private fun loadExpenses() {
        userId?.let { uid ->
            lifecycleScope.launch {
                try {
                    expenses.clear()
                    val filterCategoryName = intent.getStringExtra("filter_category_name")

                    val firebaseExpenses = if (filterCategoryName != null) {
                        firestoreManager.getExpensesByCategory(uid, filterCategoryName)
                    } else {
                        firestoreManager.getExpensesByUser(uid)
                    }

                    expenses.addAll(firebaseExpenses)

                    adapter.notifyDataSetChanged()
                    updateEmptyState()
                    updateTotal()

                } catch (e: Exception) {
                    Toast.makeText(
                        this@ExpenseListActivity,
                        "Error al cargar gastos: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun updateEmptyState() {
        if (expenses.isEmpty()) {
            binding.tvEmptyState.visibility = android.view.View.VISIBLE
            binding.rvExpenses.visibility = android.view.View.GONE
        } else {
            binding.tvEmptyState.visibility = android.view.View.GONE
            binding.rvExpenses.visibility = android.view.View.VISIBLE
        }
    }

    private fun showExpenseDetails(expense: Expense) {
        val message = buildString {
            append("Descripción: ${expense.description}\n")
            append("Monto: $${String.format("%.2f", expense.amount)}\n")
            append("Categoría: ${expense.category}\n")
            append("Fecha: ${expense.date}\n")
            append("Hora: ${expense.time}")
            if (expense.imagePath != null) {
                append("\n\n📷 Este gasto tiene una imagen adjunta")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Detalles del Gasto")
            .setMessage(message)
            .setPositiveButton("Cerrar", null)
            .setNeutralButton("Ver Imagen") { _, _ ->
                if (expense.imagePath != null) {
                    showImagePreview(expense.imagePath)
                }
            }
            .apply {
                if (expense.imagePath == null) {
                    setNeutralButton(null, null)
                }
            }
            .show()
    }

    private fun showImagePreview(imagePath: String) {
        val bitmap = fileManager.getImageFromInternalStorage(imagePath)
        if (bitmap != null) {
            val imageView = android.widget.ImageView(this).apply {
                setImageBitmap(bitmap)
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                setPadding(20, 20, 20, 20)
            }

            AlertDialog.Builder(this)
                .setTitle("Imagen del Gasto")
                .setView(imageView)
                .setPositiveButton("Cerrar", null)
                .show()
        } else {
            Toast.makeText(this, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showExpenseOptions(expense: Expense) {
        val options = arrayOf("Editar", "Eliminar")

        AlertDialog.Builder(this)
            .setTitle("Opciones")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> editExpense(expense)
                    1 -> confirmDeleteExpense(expense)
                }
            }
            .show()
    }

    private fun editExpense(expense: Expense) {
        val intent = Intent(this, EditExpenseActivity::class.java)
        intent.putExtra("expense_id", expense.id)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun updateTotal() {
        val total = expenses.sumOf { it.amount }
        binding.tvTotal.text = "$" + String.format("%.2f", total)
    }

    private fun confirmDeleteExpense(expense: Expense) {
        val message = "¿Seguro que deseas eliminar este gasto?\n\n" +
                "${expense.description} - $${String.format("%.2f", expense.amount)}"

        AlertDialog.Builder(this)
            .setTitle("Eliminar Gasto")
            .setMessage(message)
            .setPositiveButton("Eliminar") { _, _ ->
                deleteExpense(expense)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteExpense(expense: Expense) {
        lifecycleScope.launch {
            try {
                val success = firestoreManager.deleteExpense(expense.id)

                if (success) {
                    expense.imagePath?.let { imagePath ->
                        fileManager.deleteImageFromInternalStorage(imagePath)
                    }

                    Toast.makeText(
                        this@ExpenseListActivity,
                        "Gasto eliminado",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadExpenses()
                } else {
                    Toast.makeText(
                        this@ExpenseListActivity,
                        "Error al eliminar gasto",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ExpenseListActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_expense_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_filter -> {
                showFilterDialog()
                true
            }
            R.id.action_export -> {
                exportExpenses()
                true
            }
            R.id.action_cleanup -> {
                cleanupImages()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showFilterDialog() {
        val categories = listOf(
            "Todas", "Alimentación", "Transporte", "Entretenimiento",
            "Salud", "Compras", "Servicios", "Otros"
        )

        AlertDialog.Builder(this)
            .setTitle("Filtrar por categoría")
            .setItems(categories.toTypedArray()) { _, which ->
                filterByCategory(if (which == 0) null else categories[which])
            }
            .show()
    }

    private fun filterByCategory(category: String?) {
        userId?.let { uid ->
            lifecycleScope.launch {
                try {
                    expenses.clear()

                    val firebaseExpenses = if (category == null) {
                        firestoreManager.getExpensesByUser(uid)
                    } else {
                        firestoreManager.getExpensesByCategory(uid, category)
                    }

                    expenses.addAll(firebaseExpenses)

                    adapter.notifyDataSetChanged()
                    updateEmptyState()
                    updateTotal()

                    supportActionBar?.title = if (category == null) "Mis Gastos" else "Gastos - $category"
                } catch (e: Exception) {
                    android.util.Log.e("ExpenseListActivity", "Error filtrando", e)
                    Toast.makeText(
                        this@ExpenseListActivity,
                        "Error al filtrar: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun exportExpenses() {
        if (expenses.isEmpty()) {
            Toast.makeText(this, "No hay gastos para exportar", Toast.LENGTH_SHORT).show()
            return
        }

        val exportData = buildString {
            append("Reporte de Gastos\n")
            append("==================\n\n")

            expenses.forEach { expense ->
                append("Descripción: ${expense.description}\n")
                append("Monto: $${String.format("%.2f", expense.amount)}\n")
                append("Categoría: ${expense.category}\n")
                append("Fecha: ${expense.date} ${expense.time}\n")
                if (expense.imagePath != null) append("Imagen: Sí\n")
                append("---\n")
            }

            val total = expenses.sumOf { it.amount }
            append("\nTotal: $${String.format("%.2f", total)}")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, exportData)
            putExtra(Intent.EXTRA_SUBJECT, "Reporte de Gastos - MoneyTracker")
        }
        startActivity(Intent.createChooser(shareIntent, "Exportar gastos"))
    }

    private fun cleanupImages() {
        val validImagePaths = expenses.map { it.imagePath }
        fileManager.cleanupOrphanImages(validImagePaths)
        Toast.makeText(this, "Imágenes huérfanas eliminadas", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}