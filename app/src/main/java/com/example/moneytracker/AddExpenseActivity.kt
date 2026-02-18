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
import com.example.moneytracker.databinding.ActivityAddExpenseBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private lateinit var authManager: FirebaseAuthManager
    private lateinit var fileManager: FileManager
    private lateinit var firestoreManager: FirestoreManager
    private val calendar = Calendar.getInstance()
    private var selectedDateStr = ""
    private var selectedTimeStr = ""
    private var currentImagePath: String? = null
    private var currentPhotoUri: Uri? = null
    private var currentPhotoPath: String? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private val LOCATION_PERMISSION_CODE = 1003

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
        else Toast.makeText(this, "Permiso de camara denegado", Toast.LENGTH_SHORT).show()
    }

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) handleCapturedImage()
    }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val path = fileManager.copyImageFromUri(it)
            if (path != null) {
                currentImagePath = path
                displaySelectedImage()
                binding.btnRemoveImage.visibility = android.view.View.VISIBLE
            } else {
                Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = FirebaseAuthManager()
        firestoreManager = FirestoreManager()
        fileManager = FileManager(this)

        setupSpinner()
        setupDateTimePickers()
        setupImageButtons()
        setupSaveButton()
        updateDateLabel()
        updateTimeLabel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationAndGet()
    }

    private fun setupSpinner() {
        val categories = listOf("Alimentacion", "Transporte", "Entretenimiento", "Salud", "Compras", "Servicios", "Otros")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupDateTimePickers() {
        binding.btnPickDate.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                calendar.set(y, m, d)
                updateDateLabel()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnPickTime.setOnClickListener {
            TimePickerDialog(this, { _, h, min ->
                calendar.set(Calendar.HOUR_OF_DAY, h)
                calendar.set(Calendar.MINUTE, min)
                updateTimeLabel()
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }
    }

    private fun setupImageButtons() {
        binding.btnAddImage.setOnClickListener { showImageOptionsDialog() }
        binding.btnRemoveImage.setOnClickListener { removeImage() }
    }

    private fun setupSaveButton() {
        binding.btnSaveExpense.setOnClickListener { saveExpense() }
    }

    private fun showImageOptionsDialog() {
        AlertDialog.Builder(this)
            .setItems(arrayOf("Tomar foto", "Seleccionar de galeria")) { _, i ->
                if (i == 0) checkCameraPermissionAndTakePicture() else pickImage.launch("image/*")
            }
            .show()
    }

    private fun checkCameraPermissionAndTakePicture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val file = createImageFile() ?: return
        currentPhotoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        takePicture.launch(currentPhotoUri)
    }

    private fun createImageFile(): File? {
        return try {
            val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val dir = File(externalCacheDir, "temp_images")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "TEMP_$time.jpg")
            currentPhotoPath = file.absolutePath
            file
        } catch (e: Exception) {
            null
        }
    }

    private fun handleCapturedImage() {
        val file = File(currentPhotoPath ?: return)
        if (!file.exists()) return

        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
        val savedPath = fileManager.saveImageToInternalStorage(bitmap)

        if (savedPath != null) {
            currentImagePath = savedPath
            displaySelectedImage()
            binding.btnRemoveImage.visibility = android.view.View.VISIBLE
        }

        file.delete()
    }

    private fun displaySelectedImage() {
        currentImagePath?.let {
            val bmp = fileManager.getImageFromInternalStorage(it)
            if (bmp != null) {
                binding.ivExpenseImage.setImageBitmap(bmp)
                binding.ivExpenseImage.visibility = android.view.View.VISIBLE
                binding.tvImageHint.visibility = android.view.View.GONE
            }
        }
    }

    private fun removeImage() {
        currentImagePath?.let { fileManager.deleteImageFromInternalStorage(it) }
        currentImagePath = null
        binding.ivExpenseImage.setImageDrawable(null)
        binding.ivExpenseImage.visibility = android.view.View.GONE
        binding.tvImageHint.visibility = android.view.View.VISIBLE
        binding.btnRemoveImage.visibility = android.view.View.GONE
    }

    private fun saveExpense() {
        val description = binding.etDescription.text.toString().trim()
        val amountStr = binding.etAmount.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem.toString()
        val location = if (currentLocation != null) {
            mapOf(
                "lat" to currentLocation!!.latitude,
                "lng" to currentLocation!!.longitude
            )
        } else {
            null
        }

        if (description.isEmpty()) {
            binding.etDescription.error = "Campo requerido"
            return
        }

        if (amountStr.isEmpty()) {
            binding.etAmount.error = "Campo requerido"
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.etAmount.error = "Monto invalido"
            return
        }

        val userId = authManager.getCurrentUserId()
        if (userId == null) {
            Toast.makeText(this, "Usuario no identificado", Toast.LENGTH_SHORT).show()
            return
        }

        val expense = Expense(
            id = "",
            description = description,
            amount = amount,
            category = category,
            categoryId = "",
            date = selectedDateStr,
            time = selectedTimeStr,
            imagePath = currentImagePath,
            userId = userId,
            location = location
        )

        lifecycleScope.launch {
            try {
                val (success, message) = firestoreManager.insertExpense(expense)

                binding.btnSaveExpense.isEnabled = true
                binding.btnSaveExpense.text = "Guardar"

                if (success) {
                    NotificationHelper.notifyExpenseSaved(
                        this@AddExpenseActivity,
                        description,
                        amount
                    )

                    Toast.makeText(
                        this@AddExpenseActivity,
                        "Gasto guardado exitosamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                } else {
                    Toast.makeText(
                        this@AddExpenseActivity,
                        "Error al guardar: $message",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                binding.btnSaveExpense.isEnabled = true
                binding.btnSaveExpense.text = "Guardar"

                Toast.makeText(
                    this@AddExpenseActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun requestLocationAndGet() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        } else {
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                android.util.Log.w("AddExpenseActivity", "Permiso de ubicación denegado")
            }
        }
    }

    private fun getCurrentLocation() {
        try {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        currentLocation = location
                        android.util.Log.d(
                            "AddExpenseActivity",
                            "Ubicación obtenida: ${location.latitude}, ${location.longitude}"
                        )
                    } else {
                        android.util.Log.w("AddExpenseActivity", "Location es null")
                    }
                }.addOnFailureListener { e ->
                    android.util.Log.e("AddExpenseActivity", "Error obteniendo ubicación", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AddExpenseActivity", "Error en getCurrentLocation", e)
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

    override fun onDestroy() {
        super.onDestroy()
        currentPhotoPath?.let {
            val f = File(it)
            if (f.exists()) f.delete()
        }
    }
}
