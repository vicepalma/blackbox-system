package com.viceliss.blackbox.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.viceliss.blackbox.data.AppDatabase
import com.viceliss.blackbox.data.Journey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var database: AppDatabase
    private lateinit var journeyId: String

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())

        database = AppDatabase.getDatabase(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    updateLocation(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        journeyId = intent?.getStringExtra("journeyId") ?: UUID.randomUUID().toString()

        if (journeyId.isEmpty()) {
            Log.e("LocationService", "❌ No se recibió un journeyId válido.")
            stopSelf()
            return START_NOT_STICKY
        }

        Log.d("LocationService", "📌 Iniciando tracking con JourneyID: $journeyId")

        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationService", "❌ Permiso de ubicación no concedido.")
            stopSelf()
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build()

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        Log.d("LocationService", "✅ Iniciando actualizaciones de ubicación")
    }

    private fun updateLocation(location: Location) {
        val speedKmH = location.speed * 3.6
        val locationData = "JourneyID: $journeyId - Lat: ${location.latitude}, Lng: ${location.longitude}, Speed: %.2f km/h".format(speedKmH)

        Log.d("LocationService", locationData) // ✅ Log para depuración

        CoroutineScope(Dispatchers.IO).launch {
            try {
                database.journeyDao().insert(
                    Journey(
                        journeyId = journeyId,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        speed = speedKmH,
                        timestamp = System.currentTimeMillis()
                    )
                )
                Log.d("LocationService", "✅ Registro guardado en BD: $locationData")
            } catch (e: Exception) {
                Log.e("LocationService", "❌ Error al guardar en BD: ${e.message}")
            }
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "blackbox_channel")
            .setContentTitle("BlackBox Tracking")
            .setContentText("Recolectando datos de ubicación")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "blackbox_channel",
            "BlackBox Tracking",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("LocationService", "🛑 Servicio detenido y tracking finalizado")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
