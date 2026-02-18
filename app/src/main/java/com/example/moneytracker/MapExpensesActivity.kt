package com.example.moneytracker

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.moneytracker.databinding.ActivityMapExpensesBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.launch

class MapExpensesActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapExpensesBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var authManager: FirebaseAuthManager
    private lateinit var firestoreManager: FirestoreManager

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private var currentLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapExpensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mapa de Gastos"

        authManager = FirebaseAuthManager()
        firestoreManager = FirestoreManager()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupButtons()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.uiSettings.isCompassEnabled = true

        requestLocationPermissions()
    }

    private fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                Toast.makeText(
                    this,
                    "Permiso de ubicación necesario para mostrar tu posición",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun enableMyLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mMap.isMyLocationEnabled = true

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    if (location != null) {
                        currentLocation = location
                        val currentLatLng = LatLng(location.latitude, location.longitude)

                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
                    }
                }

                loadExpensesOnMap()
            }
        } catch (e: SecurityException) {
            android.util.Log.e("MapExpensesActivity", "Error de permisos", e)
            Toast.makeText(this, "Error de permisos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadExpensesOnMap() {
        val userId = authManager.getCurrentUserId() ?: return

        lifecycleScope.launch {
            try {
                val expenses = firestoreManager.getExpensesByUser(userId)

                val expensesWithLocation = expenses.filter { it.location != null }

                if (expensesWithLocation.isEmpty()) {
                    Toast.makeText(
                        this@MapExpensesActivity,
                        "No hay gastos con ubicación guardada",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                expensesWithLocation.forEach { expense ->
                    val location = expense.location
                    val lat = location?.get("lat") ?: return@forEach
                    val lng = location["lng"] ?: return@forEach

                    val position = LatLng(lat, lng)

                    val markerColor = when {
                        expense.amount > 1000 -> BitmapDescriptorFactory.HUE_RED
                        expense.amount > 500 -> BitmapDescriptorFactory.HUE_ORANGE
                        else -> BitmapDescriptorFactory.HUE_GREEN
                    }

                    mMap.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title(expense.description)
                            .snippet("$${String.format("%.2f", expense.amount)} - ${expense.category}")
                            .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                    )
                }

                val firstExpense = expensesWithLocation.first().location
                val firstLat = firstExpense?.get("lat") ?: 0.0
                val firstLng = firstExpense?.get("lng") ?: 0.0
                mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(firstLat, firstLng), 12f)
                )

                Toast.makeText(
                    this@MapExpensesActivity,
                    "${expensesWithLocation.size} gastos en el mapa",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                android.util.Log.e("MapExpensesActivity", "Error cargando gastos", e)
                Toast.makeText(
                    this@MapExpensesActivity,
                    "Error al cargar gastos: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupButtons() {
        binding.btnMyLocation.setOnClickListener {
            currentLocation?.let { location ->
                val currentLatLng = LatLng(location.latitude, location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            } ?: run {
                Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnNearestExpense.setOnClickListener {
            findNearestExpense()
        }
    }

    private fun findNearestExpense() {
        val current = currentLocation
        if (current == null) {
            Toast.makeText(
                this,
                "Ubicación actual no disponible",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val userId = authManager.getCurrentUserId() ?: return

        lifecycleScope.launch {
            try {
                val expenses = firestoreManager.getExpensesByUser(userId)
                val expensesWithLocation = expenses.filter { it.location != null }

                if (expensesWithLocation.isEmpty()) {
                    Toast.makeText(
                        this@MapExpensesActivity,
                        "No hay gastos con ubicación",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                var nearestExpense: Expense? = null
                var minDistance = Float.MAX_VALUE

                expensesWithLocation.forEach { expense ->
                    val expLat = expense.location?.get("lat") ?: return@forEach
                    val expLng = expense.location["lng"] ?: return@forEach

                    val results = FloatArray(1)
                    Location.distanceBetween(
                        current.latitude,
                        current.longitude,
                        expLat,
                        expLng,
                        results
                    )

                    if (results[0] < minDistance) {
                        minDistance = results[0]
                        nearestExpense = expense
                    }
                }

                nearestExpense?.let { expense ->
                    val expLat = expense.location?.get("lat") ?: return@launch
                    val expLng = expense.location?.get("lng") ?: return@launch
                    val position = LatLng(expLat, expLng)

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))

                    val distanceKm = minDistance / 1000
                    Toast.makeText(
                        this@MapExpensesActivity,
                        "Gasto más cercano: ${expense.description} (${String.format("%.2f", distanceKm)} km)",
                        Toast.LENGTH_LONG
                    ).show()

                    drawRoute(current.latitude, current.longitude, expLat, expLng)
                }

            } catch (e: Exception) {
                android.util.Log.e("MapExpensesActivity", "Error", e)
                Toast.makeText(
                    this@MapExpensesActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun drawRoute(startLat: Double, startLng: Double, endLat: Double, endLng: Double) {
        val startPoint = LatLng(startLat, startLng)
        val endPoint = LatLng(endLat, endLng)

        mMap.clear()

        loadExpensesOnMap()

        val polylineOptions = PolylineOptions()
            .add(startPoint)
            .add(endPoint)
            .width(10f)
            .color(android.graphics.Color.BLUE)
            .geodesic(true)

        mMap.addPolyline(polylineOptions)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}