package org.foodforwardsa.foodshareconnect.data.repository

import kotlinx.coroutines.flow.Flow
import org.foodforwardsa.foodshareconnect.data.AppDatabase
import org.foodforwardsa.foodshareconnect.data.FoodCategory
import org.foodforwardsa.foodshareconnect.data.entity.FoodDonation
import org.foodforwardsa.foodshareconnect.util.OperationResult
import org.foodforwardsa.foodshareconnect.util.safeCall

class DonationRepository(private val db: AppDatabase) {

    private val donationDao = db.foodDonationDao()

    fun observeAvailable(): Flow<List<FoodDonation>> = donationDao.observeAvailable(System.currentTimeMillis())

    suspend fun findMatching(category: FoodCategory, minQuantityKg: Double): List<FoodDonation> =
        donationDao.findMatching(category, minQuantityKg, System.currentTimeMillis())

    suspend fun totalAvailableStockKg(): Double = donationDao.totalAvailableStockKg(System.currentTimeMillis())

    suspend fun createDonation(
        donorName: String,
        category: FoodCategory,
        quantityKg: Double,
        expiryDateEpochMillis: Long,
        notes: String?
    ): OperationResult<Unit> = safeCall("Could not save this donation") {
        if (expiryDateEpochMillis <= System.currentTimeMillis()) {
            throw IllegalArgumentException("Expiry date must be in the future.")
        }
        donationDao.insert(
            FoodDonation(
                donorName = donorName,
                category = category,
                quantityKg = quantityKg,
                remainingQuantityKg = quantityKg,
                expiryDateEpochMillis = expiryDateEpochMillis,
                notes = notes
            )
        )
    }

    /**
     * Decrements stock as part of approving a request. Re-reads the donation
     * immediately before writing and re-checks the remaining quantity, so
     * that stock can never be pushed below zero even under concurrent admin
     * actions - mirroring the transaction guard on the web app.
     */
    suspend fun allocateStock(donationId: Long, requestedQuantityKg: Double): OperationResult<Unit> =
        safeCall("Could not allocate stock") {
            val donation = donationDao.findById(donationId) ?: throw IllegalStateException("Donation not found.")
            if (donation.remainingQuantityKg < requestedQuantityKg) {
                throw IllegalStateException("Insufficient stock: only ${donation.remainingQuantityKg}kg remains for this donation.")
            }
            donationDao.update(donation.copy(remainingQuantityKg = donation.remainingQuantityKg - requestedQuantityKg))
        }
}
