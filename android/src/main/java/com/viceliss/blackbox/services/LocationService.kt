package com.viceliss.blackbox.services

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.viceliss.blackbox.data.AppDatabase
import com.viceliss.blackbox.data.Journey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import android.Manifest
import android.content.pm.PackageManager


class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var journeyId: String = UUID.randomUUID().toString() // ✅ Asignar un ID único para cada viaje

    companion object {
        val locationLiveData = MutableLiveData<String>() // ✅ LiveData para actualizar en UI
    }

    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()

        journeyId = UUID.randomUUID().toString() // ✅ Generar un nuevo ID único
        database = AppDatabase.getDatabase(this) // ✅ Inicializar la base de datos
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this) // ✅ Inicializar GPS

        // ✅ Guardar journeyId en SharedPreferences correctamente
        val sharedPreferences = getSharedPreferences("BlackBoxPrefs", MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("lastJourneyId", journeyId)
            .apply()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocation(location)
                }
            }
        }
    }


    private fun updateLocation(location: Location) {
        val speedKmH = location.speed * 3.6
        val locationData = "Lat: ${location.latitude}, Lng: ${location.longitude}, Speed: %.2f km/h".format(speedKmH)

        Log.d("LocationService", "JourneyID: $journeyId - $locationData") // ✅ Log para depurar
        locationLiveData.postValue(locationData) // ✅ Enviar datos a la UI

        CoroutineScope(Dispatchers.IO).launch {
            database.journeyDao().insert(
                Journey(
                    journeyId = journeyId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    speed = speedKmH, // ✅ Corrección del nombre del parámetro
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }





    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationService", "🚨 Permisos de ubicación NO concedidos. No se iniciarán actualizaciones.")
            return
        }

        val locationRequest = LocationRequest.Builder(2000) // Cada 2 segundos
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        Log.d("LocationService", "✅ Iniciando actualizaciones de ubicación")
    }


    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
