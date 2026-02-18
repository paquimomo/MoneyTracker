package com.example.moneytracker

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.moneytracker.databinding.ActivityEditExpenseBinding
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class EditExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditExpenseBinding
    private lateinit var authManager: FirebaseAuthManager
    private lateinit var fileManager: FileManager
    private lateinit var firestoreManager: FirestoreManager
    private val calendar: Calendar = Calendar.getInstance()
    private var selectedDateStr: String = ""
    private var selectedTimeStr: String = ""
    private var currentImagePath: String? = null
    private var currentPhotoUri: Uri? = null
    private var expenseId: String = ""
    private var originalImagePath: String? = null

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            handleCapturedImage()
        }
    }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val imagePath = fileManager.copyImageFromUri(it)
            if (imagePath != null) {
                if (currentImagePath != originalImagePath && currentImagePath != null) {
                    fileManager.deleteImageFromInternalStorage(currentImagePath!!)
                }
                currentImagePath = imagePath
                displaySelectedImage()
                binding.btnRemoveImage.visibility = android.view.View.VISIBLE
            } else {
                Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = FirebaseAuthManager()
        firestoreManager = FirestoreManager()
        fileManager = FileManager(this)

        expenseId = intent.getStringExtra("expense_id") ?: ""
        if (expenseId.isEmpty()) {
            Toast.makeText(this, "Error: Gasto no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupSpinner()
        setupDateTimePickers()
        setupImageButtons()
        setupSaveButton()
        loadExpenseData()
    }

    private fun setupSpinner() {
        val categories = listOf(
            "Alimentación", "Transporte", "Entretenimiento",
            "Salud", "Compras", "Servicios", "Otros"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupDateTimePickers() {
        binding.btnPickDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, day)
                    updateDateLabel()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.btnPickTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    updateTimeLabel()
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    private fun setupImageButtons() {
        binding.btnAddImage.setOnClickListener {
            showImageOptionsDialog()
        }

        binding.btnRemoveImage.setOnClickListener {
            removeImage()
        }

        binding.ivExpenseImage.setOnClickListener {
            if (currentImagePath != null) {
                showImagePreview()
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnUpdateExpense.setOnClickListener {
            updateExpense()
        }
    }

    private fun loadExpenseData() {
        lifecycleScope.launch {
            try {
                val expense = firestoreManager.getExpenseById(expenseId)

                if (expense != null) {
                    binding.etDescription.setText(expense.description)
                    binding.etAmount.setText(expense.amount.toString())

                    val categories = listOf(
                        "Alimentación", "Transporte", "Entretenimiento",
                        "Salud", "Compras", "Servicios", "Otros"
                    )
                    val categoryIndex = categories.indexOf(expense.category)
                    if (categoryIndex != -1) {
                        binding.spinnerCategory.setSelection(categoryIndex)
                    }

                    selectedDateStr = expense.date
                    selectedTimeStr = expense.time
                    binding.tvDate.text = selectedDateStr
                    binding.tvTime.text = selectedTimeStr

                    try {
                        val dateTimeStr = "${expense.date} ${expense.time}"
                        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        calendar.time = sdf.parse(dateTimeStr) ?: Date()
                    } catch (e: Exception) {
                        calendar.time = Date()
                    }

                    if (expense.imagePath != null) {
                        currentImagePath = expense.imagePath
                        originalImagePath = expense.imagePath
                        displaySelectedImage()
                        binding.btnRemoveImage.visibility = android.view.View.VISIBLE
                    }
                } else {
                    Toast.makeText(
                        this@EditExpenseActivity,
                        "Gasto no encontrado",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@EditExpenseActivity,
                    "Error cargando gasto",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun showImageOptionsDialog() {
        val options = if (currentImagePath != null) {
            arrayOf("Tomar nueva foto", "Seleccionar de galería", "Quitar imagen actual")
        } else {
            arrayOf("Tomar foto", "Seleccionar de galería")
        }

        AlertDialog.Builder(this)
            .setTitle("Opciones de imagen")
            .setItems(options) { _, which ->
                when {
                    which == 0 -> checkCameraPermissionAndTakePicture()
                    which == 1 -> pickImage.launch("image/*")
                    which == 2 && currentImagePath != null -> removeImage()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndTakePicture() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        if (photoFile != null) {
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                photoFile
            )
            takePicture.launch(currentPhotoUri)
        }
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = File(externalCacheDir, "temp_images")
            if (!storageDir.exists()) storageDir.mkdirs()
            File(storageDir, "TEMP_${timeStamp}.jpg")
        } catch (ex: Exception) {
            null
        }
    }

    private fun handleCapturedImage() {
        try {
            val tempFile = File(currentPhotoUri?.path ?: return)
            val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)

            if (currentImagePath != originalImagePath && currentImagePath != null) {
                fileManager.deleteImageFromInternalStorage(currentImagePath!!)
            }

            val imagePath = fileManager.saveImageToInternalStorage(bitmap)
            if (imagePath != null) {
                currentImagePath = imagePath
                displaySelectedImage()
                binding.btnRemoveImage.visibility = android.view.View.VISIBLE
            }

            tempFile.delete()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al procesar imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displaySelectedImage() {
        currentImagePath?.let { path ->
            val bitmap = fileManager.getImageFromInternalStorage(path)
            if (bitmap != null) {
                binding.ivExpenseImage.setImageBitmap(bitmap)
                binding.ivExpenseImage.visibility = android.view.View.VISIBLE
                binding.tvImageHint.visibility = android.view.View.GONE
            }
        }
    }

    private fun removeImage() {
        if (currentImagePath != originalImagePath && currentImagePath != null) {
            fileManager.deleteImageFromInternalStorage(currentImagePath!!)
        }

        currentImagePath = null
        binding.ivExpenseImage.setImageDrawable(null)
        binding.ivExpenseImage.visibility = android.view.View.GONE
        binding.tvImageHint.visibility = android.view.View.VISIBLE
        binding.btnRemoveImage.visibility = android.view.View.GONE
    }

    private fun showImagePreview() {
        currentImagePath?.let { path ->
            val bitmap = fileManager.getImageFromInternalStorage(path)
            if (bitmap != null) {
                AlertDialog.Builder(this)
                    .setTitle("Vista previa")
                    .setView(android.widget.ImageView(this).apply {
                        setImageBitmap(bitmap)
                        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                    })
                    .setPositiveButton("Cerrar", null)
                    .show()
            }
        }
    }

    private fun updateExpense() {
        val description = binding.etDescription.text.toString().trim()
        val amountStr = binding.etAmount.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem.toString()

        if (description.isEmpty()) {
            binding.etDescription.error = "Requerido"
            return
        }
        if (amountStr.isEmpty()) {
            binding.etAmount.error = "Requerido"
            return
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.etAmount.error = "Monto inválido"
            return
        }

        val userId = authManager.getCurrentUserId()
        if (userId == null) {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        if (originalImagePath != null && currentImagePath == null) {
            fileManager.deleteImageFromInternalStorage(originalImagePath!!)
        }
        else if (originalImagePath != null && currentImagePath != originalImagePath) {
            fileManager.deleteImageFromInternalStorage(originalImagePath!!)
        }

        binding.btnUpdateExpense.isEnabled = false
        binding.btnUpdateExpense.text = "Actualizando..."

        val updatedExpense = Expense(
            id = expenseId,
            description = description,
            amount = amount,
            category = category,
            categoryId = "",
            date = selectedDateStr,
            time = selectedTimeStr,
            imagePath = currentImagePath,
            userId = userId
        )

        lifecycleScope.launch {
            val success = firestoreManager.updateExpense(updatedExpense)

            binding.btnUpdateExpense.isEnabled = true
            binding.btnUpdateExpense.text = "Actualizar"

            if (success) {
                Toast.makeText(this@EditExpenseActivity,
                    "Gasto actualizado exitosamente",
                    Toast.LENGTH_SHORT)
                finish()
            } else {
                Toast.makeText(this@EditExpenseActivity,
                    "Error al actualizar el gasto",
                    Toast.LENGTH_SHORT)
            }
        }
    }

    private fun updateDateLabel() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        selectedDateStr = sdf.format(calendar.time)
        binding.tvDate.text = selectedDateStr
    }

    private fun updateTimeLabel() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        selectedTimeStr = sdf.format(calendar.time)
        binding.tvTime.text = selectedTimeStr
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        currentPhotoUri?.path?.let { path ->
            File(path).delete()
        }
    }
}