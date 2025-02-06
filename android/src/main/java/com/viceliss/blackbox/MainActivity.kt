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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.viceliss.blackbox.data.AppDatabase
import com.viceliss.blackbox.data.Journey
import com.viceliss.blackbox.services.LocationService
import kotlinx.coroutines.CoroutineScope // ✅ AGREGADO (Importante para rememberCoroutineScope)
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import android.os.Environment
import java.io.File
import java.io.FileWriter
import com.google.gson.Gson
import com.google.gson.GsonBuilder

import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestLocationPermissions() // ✅ Asegurar que se piden permisos al inicio
        setContent {
            BlackBoxApp(
                onRequestPermissions = { requestLocationPermissions() }, // ✅ Pasar función correcta
                onStartTracking = { startTracking() }, // ✅ Iniciar tracking
                onStopTracking = { stopTracking() } // ✅ Detener tracking
            )
        }
        val dbPath = getDatabasePath("blackbox_database").absolutePath
        Log.d("BlackBox", "📂 Ruta de la BD: $dbPath")

    }

    // ✅ Función para solicitar permisos
    private fun requestLocationPermissions() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (!hasLocationPermissions()) {
            ActivityCompat.requestPermissions(this, permissions, 1001)
        } else {
            Log.d("BlackBox", "✅ Permisos ya concedidos.")
        }

        if (!fineLocation || !coarseLocation) {
            Log.d("BlackBox", "Pidiendo permisos básicos de ubicación")
            requestPermissionsLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val backgroundLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!backgroundLocation) {
                Log.d("BlackBox", "Pidiendo permiso de ubicación en segundo plano")
                requestPermissionsLauncher.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            } else {
                Log.d("BlackBox", "Todos los permisos concedidos, iniciando servicio")
                startTracking()
            }
        } else {
            Log.d("BlackBox", "Todos los permisos concedidos, iniciando servicio")
            startTracking()
        }
    }



    // ✅ Solicitar permisos usando ActivityResultContracts
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value }
            Log.d("BlackBox", "Permisos concedidos: $granted") // ✅ Verificar si los permisos fueron aceptados
            if (granted) {
                startTracking()
            } else {
                Log.e("BlackBox", "Permisos denegados por el usuario")
            }
        }


    // ✅ Función para iniciar el servicio de ubicación
    private fun startTracking() {
        if (!hasLocationPermissions()) {
            Log.e("BlackBox", "❌ No tienes permisos de ubicación, no se puede iniciar el tracking.")
            return
        }

        val intent = Intent(this, LocationService::class.java)
        startService(intent)

        val sharedPreferences = getSharedPreferences("BlackBoxPrefs", MODE_PRIVATE)
        val journeyId = sharedPreferences.getString("lastJourneyId", "No disponible")
        Log.d("BlackBox", "📌 Iniciado tracking - JourneyID: $journeyId")
    }

    private fun stopTracking() {
        val intent = Intent(this, LocationService::class.java)
        stopService(intent)
        Log.d("BlackBox", "🛑 Servicio detenido por el usuario")
    }

    // ✅ Verificar si los permisos fueron concedidos
    private fun hasLocationPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true

        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

}

private fun exportDataToJSON(context: Context, journeys: List<Journey>) {
    if (journeys.isEmpty()) {
        Log.e("BlackBox", "❌ No hay datos para exportar.")
        return
    }

    val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    val jsonString = gson.toJson(journeys)

    // 📂 Definir la ubicación del archivo
    val fileName = "blackbox_journeys.json"
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

    try {
        if (file.exists()) {
            file.delete() // ✅ Eliminar el archivo antes de escribirlo para evitar bloqueos
        }

        FileWriter(file, false).use { it.write(jsonString) } // ✅ Modo `false` para sobrescribir
        Log.d("BlackBox", "✅ Archivo exportado: ${file.absolutePath}")
    } catch (e: Exception) {
        Log.e("BlackBox", "❌ Error al exportar: ${e.message}")
    }
}

private fun copyDatabaseToStorage(context: Context) {
    val dbPath = File(context.getDatabasePath("blackbox_database").absolutePath)
    val outFile = File(context.getExternalFilesDir(null), "blackbox_database_copy")

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

    // ✅ Cargar el último journeyId al abrir la app
    LaunchedEffect(Unit) {
        val sharedPreferences = context.getSharedPreferences("BlackBoxPrefs", android.content.Context.MODE_PRIVATE)
        currentJourneyId = sharedPreferences.getString("lastJourneyId", "No disponible") ?: "No disponible"
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "BlackBox - Registro de Viajes", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(10.dp))

        // ✅ Mostrar el ID del Journey en la UI
        Text(text = "📌 ID del Journey: $currentJourneyId", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(20.dp))

        // ✅ Botón para INICIAR REGISTRO
        Button(
            onClick = {
                onStartTracking()
                val sharedPreferences = context.getSharedPreferences("BlackBoxPrefs", android.content.Context.MODE_PRIVATE)
                currentJourneyId = sharedPreferences.getString("lastJourneyId", "No disponible") ?: "No disponible"
            }
        ) {
            Text(text = "Iniciar Registro")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ✅ Botón para DETENER REGISTRO
        Button(
            onClick = {
                onStopTracking()
            }
        ) {
            Text(text = "Detener Registro")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ✅ Botón para MOSTRAR DATOS GUARDADOS
        Button(
            onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    val sharedPreferences = context.getSharedPreferences("BlackBoxPrefs", android.content.Context.MODE_PRIVATE)
                    val lastJourneyId = sharedPreferences.getString("lastJourneyId", null)

                    if (lastJourneyId != null) {
                        Log.d("BlackBox", "✅ Recuperado journeyId: $lastJourneyId")
                        journeys.clear()
                        val journeyData = database.journeyDao().getJourneysById(lastJourneyId)
                        journeys.addAll(journeyData)

                        Log.d("BlackBox", "📌 Se encontraron ${journeyData.size} registros en la BD")
                    } else {
                        Log.e("BlackBox", "❌ No se encontró un journeyId válido en SharedPreferences")
                    }
                }
            }
        ) {
            Text(text = "Mostrar Datos Guardados")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ✅ Botón para EXPORTAR DATOS A JSON
        Button(
            onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    if (journeys.isEmpty()) {
                        Log.e("BlackBox", "❌ No hay datos cargados. Intentando recuperar desde la BD...")

                        val sharedPreferences = context.getSharedPreferences("BlackBoxPrefs", android.content.Context.MODE_PRIVATE)
                        val lastJourneyId = sharedPreferences.getString("lastJourneyId", null)

                        if (lastJourneyId != null) {
                            val database = AppDatabase.getDatabase(context)
                            val journeyData = database.journeyDao().getJourneysById(lastJourneyId)

                            if (journeyData.isNotEmpty()) {
                                journeys.clear()
                                journeys.addAll(journeyData)
                                Log.d("BlackBox", "✅ Datos recuperados desde la BD. Intentando exportar nuevamente...")
                                exportDataToJSON(context, journeys) // ✅ Exportar después de cargar
                            } else {
                                Log.e("BlackBox", "❌ No hay datos en la BD para exportar.")
                            }
                        } else {
                            Log.e("BlackBox", "❌ No se encontró un journeyId válido en SharedPreferences.")
                        }
                    } else {
                        exportDataToJSON(context, journeys)
                    }
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


        // ✅ Lista de datos guardados
        LazyColumn {
            items(journeys.size) { index ->
                BasicText(text = "📍 ${journeys[index].latitude}, ${journeys[index].longitude} - 🚗 ${journeys[index].speed} km/h")
            }
        }
    }
}





