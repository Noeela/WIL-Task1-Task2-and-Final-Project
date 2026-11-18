package org.foodforwardsa.foodshareconnect.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.foodforwardsa.foodshareconnect.data.FoodCategory
import org.foodforwardsa.foodshareconnect.data.entity.FoodDonation

@Dao
interface FoodDonationDao {

    // Only non-expired stock with remaining quantity, soonest-to-expire first,
    // so both Admins and Beneficiary Organisations naturally see the stock
    // most at risk of spoilage prioritised at the top of the list.
    @Query("SELECT * FROM food_donations WHERE remainingQuantityKg > 0 AND expiryDateEpochMillis > :nowEpochMillis ORDER BY expiryDateEpochMillis ASC")
    fun observeAvailable(nowEpochMillis: Long): Flow<List<FoodDonation>>

    @Query("""
        SELECT * FROM food_donations
        WHERE category = :category AND remainingQuantityKg >= :minQuantityKg AND expiryDateEpochMillis > :nowEpochMillis
        ORDER BY expiryDateEpochMillis ASC
    """)
    suspend fun findMatching(category: FoodCategory, minQuantityKg: Double, nowEpochMillis: Long): List<FoodDonation>

    @Query("SELECT * FROM food_donations WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): FoodDonation?

    @Query("SELECT COALESCE(SUM(remainingQuantityKg), 0) FROM food_donations WHERE remainingQuantityKg > 0 AND expiryDateEpochMillis > :nowEpochMillis")
    suspend fun totalAvailableStockKg(nowEpochMillis: Long): Double

    @Insert
    suspend fun insert(donation: FoodDonation): Long

    @Update
    suspend fun update(donation: FoodDonation)
}
