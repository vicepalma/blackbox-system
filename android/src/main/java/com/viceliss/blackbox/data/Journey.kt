package com.viceliss.blackbox.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journeys") // ✅ Asegurar que el nombre de la tabla es correcto
data class Journey(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val journeyId: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Double, // ✅ Asegurar que el tipo de dato es correcto
    val timestamp: Long
)
