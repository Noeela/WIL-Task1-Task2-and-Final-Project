package org.foodforwardsa.foodshareconnect.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.foodforwardsa.foodshareconnect.data.FoodCategory

/** Surplus food received by FoodForward SA from a donor. Captured by an Admin and drawn down as requests are approved. */
@Entity(tableName = "food_donations")
data class FoodDonation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val donorName: String,
    val category: FoodCategory,
    val quantityKg: Double,
    val remainingQuantityKg: Double,
    val expiryDateEpochMillis: Long,
    val notes: String? = null,
    val receivedAtEpochMillis: Long = System.currentTimeMillis()
)
