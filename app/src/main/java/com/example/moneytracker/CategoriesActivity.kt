package com.example.moneytracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moneytracker.databinding.ActivityCategoriesBinding
import kotlinx.coroutines.launch

class CategoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriesBinding
    private lateinit var authManager: FirebaseAuthManager
    private lateinit var firestoreManager: FirestoreManager
    private lateinit var adapter: CategoryAdapter
    private val categories = mutableListOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Categorías"

        authManager = FirebaseAuthManager()
        firestoreManager = FirestoreManager()

        setupRecyclerView()

        binding.fabAddCategory.setOnClickListener {
            openAddCategoryDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = CategoryAdapter(
            categories = categories,
            onItemClick = { category -> showCategoryDetails(category) },
            onEditClick = { category -> openEditCategoryDialog(category) },
            onDeleteClick = { category -> confirmDeleteCategory(category) }
        )

        binding.rvCategories.layoutManager = LinearLayoutManager(this)
        binding.rvCategories.adapter = adapter
    }

    private fun loadCategories() {
        val userId = authManager.getCurrentUserId()

        if (userId != null) {
            lifecycleScope.launch {
                try {
                    categories.clear()
                    val firebaseCategories = firestoreManager.getCategoriesByUser(userId)
                    categories.addAll(firebaseCategories)

                    adapter.notifyDataSetChanged()
                    updateEmptyState()

                    loadCategoriesExpenses()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@CategoriesActivity,
                        "Error al cargar categorías",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private suspend fun loadCategoriesExpenses() {
        val userId = authManager.getCurrentUserId() ?: return

        try {
            val stats = firestoreManager.getCategoryStats(userId)

            android.util.Log.d("CategoriesActivity", "Estadísticas desde Firebase:")
            stats.forEach { (name, amount) ->
                android.util.Log.d("CategoriesActivity", "  $name: $$amount")
            }

            categories.forEach { category ->
                val spent = stats[category.name] ?: 0.0
                adapter.updateCategorySpent(category.id, spent)
            }
        } catch (e: Exception) {
            android.util.Log.e("CategoriesActivity", "Error cargando gastos", e)
        }
    }

    private fun updateEmptyState() {
        if (categories.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvCategories.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvCategories.visibility = View.VISIBLE
        }
    }

    private fun openAddCategoryDialog() {
        val intent = Intent(this, AddEditCategoryActivity::class.java)
        startActivity(intent)
    }

    private fun openEditCategoryDialog(category: Category) {
        val intent = Intent(this, AddEditCategoryActivity::class.java)
        intent.putExtra("category_id", category.id)
        startActivity(intent)
    }

    private fun showCategoryDetails(category: Category) {
        lifecycleScope.launch {
            try {
                val userId = authManager.getCurrentUserId() ?: return@launch

                val expenses = firestoreManager.getExpensesByCategory(userId, category.name)
                val spent = expenses.sumOf { it.amount }
                val percentage = if (category.monthlyLimit > 0) {
                    (spent / category.monthlyLimit * 100).toInt()
                } else 0

                val message = buildString {
                    append("Categoría: ${category.name}\n")
                    append("Color: ${category.color}\n")
                    append("Tipo: ${if (category.isDefault) "Por defecto" else "Personalizada"}\n")
                    append("Límite mensual: $${String.format("%.2f", category.monthlyLimit)}\n")
                    append("Gastado este mes: $${String.format("%.2f", spent)}\n")
                    append("Progreso: $percentage%\n")
                    append("Gastos registrados: ${expenses.size}")
                }

                AlertDialog.Builder(this@CategoriesActivity)
                    .setTitle(category.name)
                    .setMessage(message)
                    .setPositiveButton("Cerrar", null)
                    .setNeutralButton("Ver Gastos") { _, _ ->
                        val intent = Intent(this@CategoriesActivity, ExpenseListActivity::class.java)
                        intent.putExtra("filter_category_name", category.name)
                        startActivity(intent)
                    }
                    .show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@CategoriesActivity,
                    "Error al cargar detalles",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun confirmDeleteCategory(category: Category) {
        if (category.isDefault) {
            Toast.makeText(
                this,
                "No puedes eliminar categorías por defecto",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Eliminar Categoría")
            .setMessage("¿Seguro que deseas eliminar '${category.name}'?\n\nLos gastos de esta categoría permanecerán pero sin categoría asignada.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteCategory(category)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteCategory(category: Category) {
        lifecycleScope.launch {
            try {
                val success = firestoreManager.deleteCategory(category.id)

                if (success) {
                    Toast.makeText(
                        this@CategoriesActivity,
                        "Categoría eliminada",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadCategories()
                } else {
                    Toast.makeText(
                        this@CategoriesActivity,
                        "Error al eliminar categoría",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@CategoriesActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadCategories()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}