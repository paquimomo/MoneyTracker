package com.example.moneytracker

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.moneytracker.databinding.ActivityAddEditCategoryBinding
import kotlinx.coroutines.launch

class AddEditCategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditCategoryBinding
    private lateinit var authManager: FirebaseAuthManager
    private lateinit var firestoreManager: FirestoreManager
    private var categoryId: String? = null
    private var isEditMode = false
    private var selectedColor = "#607D8B"

    private val availableColors = listOf(
        "#FF5722", "#3F51B5", "#E91E63", "#4CAF50",
        "#9C27B0", "#FF9800", "#607D8B", "#F44336",
        "#2196F3", "#4CAF50", "#FFEB3B", "#795548"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = FirebaseAuthManager()
        firestoreManager = FirestoreManager()

        categoryId = intent.getStringExtra("category_id")
        isEditMode = categoryId != null

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) "Editar Categoría" else "Nueva Categoría"

        setupColorPicker()
        setupSaveButton()

        if (isEditMode) {
            loadCategoryData()
        }
    }

    private fun setupColorPicker() {
        availableColors.forEach { color ->
            val colorView = layoutInflater.inflate(
                R.layout.item_color_picker,
                binding.colorsContainer,
                false
            )

            try {
                colorView.setBackgroundColor(Color.parseColor(color))
            } catch (e: Exception) {
                colorView.setBackgroundColor(Color.GRAY)
            }

            colorView.setOnClickListener {
                selectedColor = color
                updateSelectedColor(colorView)
            }

            binding.colorsContainer.addView(colorView)
        }
    }

    private fun updateSelectedColor(selectedView: android.view.View) {
        for (i in 0 until binding.colorsContainer.childCount) {
            val child = binding.colorsContainer.getChildAt(i)
            child.setPadding(0, 0, 0, 0)
        }

        selectedView.setPadding(8, 8, 8, 8)

        binding.colorPreview.setBackgroundColor(Color.parseColor(selectedColor))
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveCategory()
        }
    }

    private fun loadCategoryData() {
        lifecycleScope.launch {
            try {
                val category = firestoreManager.getCategoryById(categoryId!!)

                if (category != null) {
                    binding.etCategoryName.setText(category.name)
                    binding.etMonthlyLimit.setText(category.monthlyLimit.toString())
                    selectedColor = category.color

                    binding.colorPreview.setBackgroundColor(Color.parseColor(selectedColor))
                } else {
                    Toast.makeText(this@AddEditCategoryActivity, "Categoría no encontrada", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddEditCategoryActivity, "Error al cargar categoría", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun saveCategory() {
        val name = binding.etCategoryName.text.toString().trim()
        val limitStr = binding.etMonthlyLimit.text.toString().trim()

        if (name.isEmpty()) {
            binding.etCategoryName.error = "Nombre requerido"
            return
        }

        val monthlyLimit = limitStr.toDoubleOrNull() ?: 0.0

        val userId = authManager.getCurrentUserId()
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Guardando..."

        lifecycleScope.launch {
            try {
                if (isEditMode && categoryId != null) {
                    val updatedCategory = Category(
                        id = categoryId!!,
                        userId = userId,
                        name = name,
                        color = selectedColor,
                        icon = "category",
                        monthlyLimit = monthlyLimit,
                        isDefault = false
                    )

                    val success = firestoreManager.updateCategory(updatedCategory)

                    if (success) {
                        Toast.makeText(this@AddEditCategoryActivity, "Categoría actualizada", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@AddEditCategoryActivity, "Error al actualizar", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val newCategory = Category(
                        userId = userId,
                        name = name,
                        color = selectedColor,
                        icon = "category",
                        monthlyLimit = monthlyLimit,
                        isDefault = false
                    )

                    val (success, message) = firestoreManager.insertCategory(newCategory)

                    if (success) {
                        Toast.makeText(this@AddEditCategoryActivity, "Categoría creada", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@AddEditCategoryActivity, "Error: $message", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddEditCategoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnSave.isEnabled = true
                binding.btnSave.text = if (isEditMode) "Actualizar" else "Guardar"
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}