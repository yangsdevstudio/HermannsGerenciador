package com.hermanns.hermannsgerenciador.salesman.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val isExported: Boolean = false,
    val local: String,
    val motivo: String,
    val km: String = "",
    val comentario: String = "",
    val salesmanId: String,
    val vehiclePlate: String
) {
    fun toSnapshot() = LocationSnapshot(latitude, longitude, accuracy, timestamp)
}
