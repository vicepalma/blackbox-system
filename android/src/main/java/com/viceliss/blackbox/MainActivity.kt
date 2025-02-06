package com.viceliss.blackbox

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.viceliss.blackbox.data.AppDatabase
import com.viceliss.blackbox.data.Journey
import com.viceliss.blackbox.services.LocationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.util.UUID
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Limpia el último journeyId al abrir la app
        val sharedPreferences = getSharedPreferences("BlackBoxPrefs", MODE_PRIVATE)
        sharedPreferences.edit().remove("lastJourneyId").apply()

        setContent {
            BlackBoxApp(
                onRequestPermissions = { requestLocationPermissions() },
                onStartTracking = { startTracking() },
                onStopTracking = { stopTracking() }
            )
        }
    }

    // ✅ Solicitar permisos de ubicación
    private fun requestLocationPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (!hasLocationPermissions()) {
            ActivityCompat.requestPermissions(this, permissions, 1001)
        }
    }

    // ✅ Iniciar un nuevo tracking
    private fun startTracking() {
        if (!hasLocationPermissions()) {
            Log.e("BlackBox", "❌ No tienes permisos de ubicación.")
            return
        }

        // 🔥 Generar nuevo JourneyID y actualizar UI
        val newJourneyId = UUID.randomUUID().toString()
        val sharedPreferences = getSharedPreferences("BlackBoxPrefs", MODE_PRIVATE)
        sharedPreferences.edit().putString("lastJourneyId", newJourneyId).apply()

        Log.d("BlackBox", "📌 Nuevo JourneyID generado: $newJourneyId")

        // **REINICIAR EL SERVICIO PARA ACTUALIZAR NOTIFICACIÓN**
        val intent = Intent(this, LocationService::class.java).apply {
            putExtra("journeyId", newJourneyId) // Pasar el nuevo ID
        }
        stopService(intent) // Asegurar que se reinicia el servicio
        startService(intent) // Iniciar el servicio nuevamente con la nueva notificación
    }

    // ✅ Detener el tracking
    private fun stopTracking() {
        val intent = Intent(this, LocationService::class.java)
        stopService(intent)
        Log.d("BlackBox", "🛑 Servicio detenido")

        val sharedPreferences = getSharedPreferences("BlackBoxPrefs", MODE_PRIVATE)
        sharedPreferences.edit().remove("lastJourneyId").apply()
    }

    // ✅ Verificar permisos
    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}

// ✅ Exportar data a JSON
private fun exportDataToJSON(context: Context, journeys: List<Journey>) {
    if (journeys.isEmpty()) {
        Log.e("BlackBox", "❌ No hay datos para exportar.")
        return
    }

    val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    val jsonString = gson.toJson(journeys)

    val fileName = "blackbox_journeys.json"
    val file = File(context.getExternalFilesDir(null), fileName)

    try {
        if (file.exists()) {
            file.delete()
        }

        FileWriter(file, false).use { it.write(jsonString) }
        Log.d("BlackBox", "✅ Archivo exportado: ${file.absolutePath}")
    } catch (e: Exception) {
        Log.e("BlackBox", "❌ Error al exportar: ${e.message}")
    }
}

// ✅ Copiar base de datos
private fun copyDatabaseToStorage(context: Context) {
    val dbPath = File(context.getDatabasePath("blackbox_database").absolutePath)
    val timestamp = System.currentTimeMillis() // Obtener timestamp
    val outFile = File(context.getExternalFilesDir(null), "blackbox_database_backup_$timestamp.sqlite")

    try {
        FileInputStream(dbPath).use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
        Log.d("BlackBox", "✅ Base de datos copiada a: ${outFile.absolutePath}")
    } catch (e: Exception) {
        Log.e("BlackBox", "❌ Error copiando la BD: ${e.message}")
    }
}

// ✅ Composable principal
@Composable
fun BlackBoxApp(
    onRequestPermissions: () -> Unit = {},
    onStartTracking: () -> Unit = {},
    onStopTracking: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val journeys = remember { mutableStateListOf<Journey>() }
    val coroutineScope = rememberCoroutineScope()
    var currentJourneyId by remember { mutableStateOf("No disponible") }

    // 🔥 Actualizar UI al iniciar la app
    LaunchedEffect(Unit) {
        val sharedPreferences = context.getSharedPreferences("BlackBoxPrefs", Context.MODE_PRIVATE)
        currentJourneyId = sharedPreferences.getString("lastJourneyId", "No disponible") ?: "No disponible"
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "BlackBox - Registro de Viajes", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (currentJourneyId.isNotEmpty() && currentJourneyId != "No disponible")
                "📌 ID del Journey: $currentJourneyId"
            else
                "🛑 No hay un tracking activo",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                Log.d("BlackBox", "Botón presionado - Intentando iniciar tracking")
                onStartTracking()
                val sharedPreferences = context.getSharedPreferences("BlackBoxPrefs", Context.MODE_PRIVATE)
                currentJourneyId = sharedPreferences.getString("lastJourneyId", "No disponible") ?: "No disponible"
            }
        ) {
            Text(text = "Iniciar Registro")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                onStopTracking()
                currentJourneyId = "No disponible"
                journeys.clear()
            }
        ) {
            Text(text = "Detener Registro")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    val sharedPreferences = context.getSharedPreferences("BlackBoxPrefs", Context.MODE_PRIVATE)
                    val lastJourneyId = sharedPreferences.getString("lastJourneyId", null)

                    if (lastJourneyId != null) {
                        journeys.clear()
                        val journeyData = database.journeyDao().getJourneysById(lastJourneyId)
                        journeys.addAll(journeyData)
                    } else {
                        Log.e("BlackBox", "❌ No se encontró un journeyId válido")
                    }
                }
            }
        ) {
            Text(text = "Mostrar Datos Guardados")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    exportDataToJSON(context, journeys)
                }
            }
        ) {
            Text(text = "Exportar Datos a JSON")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { copyDatabaseToStorage(context) }
        ) {
            Text(text = "Copiar BD a Almacenamiento")
        }

        LazyColumn {
            items(journeys.size) { index ->
                BasicText(text = "📍 ${journeys[index].latitude}, ${journeys[index].longitude} - 🚗 ${journeys[index].speed} km/h")
            }
        }
    }
}
