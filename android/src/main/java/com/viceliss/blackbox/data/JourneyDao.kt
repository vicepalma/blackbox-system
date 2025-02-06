package com.viceliss.blackbox.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface JourneyDao {
    @Insert
    suspend fun insert(journey: Journey)

    @Query("SELECT * FROM journeys WHERE journeyId = :journeyId") // ✅ Anotación correcta
    suspend fun getJourneysById(journeyId: String): List<Journey> // ✅ Debe devolver List<Journey>

    @Query("DELETE FROM journeys")
    suspend fun clearAll()
}
