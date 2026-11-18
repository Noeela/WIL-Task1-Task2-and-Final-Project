package org.foodforwardsa.foodshareconnect.data.repository

import kotlinx.coroutines.flow.Flow
import org.foodforwardsa.foodshareconnect.data.AppDatabase
import org.foodforwardsa.foodshareconnect.data.FoodCategory
import org.foodforwardsa.foodshareconnect.data.RequestStatus
import org.foodforwardsa.foodshareconnect.data.VettingStatus
import org.foodforwardsa.foodshareconnect.data.entity.FoodRequest
import org.foodforwardsa.foodshareconnect.data.entity.FoodRequestWithOrgName
import org.foodforwardsa.foodshareconnect.util.OperationResult
import org.foodforwardsa.foodshareconnect.util.safeCall

class FoodRequestRepository(private val db: AppDatabase) {

    private val requestDao = db.foodRequestDao()
    private val orgDao = db.beneficiaryOrganisationDao()
    private val donationDao = db.foodDonationDao()

    fun observeForOrganisation(orgId: Long): Flow<List<FoodRequest>> = requestDao.observeForOrganisation(orgId)

    fun observeByStatusWithOrgName(status: RequestStatus): Flow<List<FoodRequestWithOrgName>> =
        requestDao.observeByStatusWithOrgName(status)

    suspend fun getWithOrgName(id: Long): FoodRequestWithOrgName? = requestDao.findByIdWithOrgName(id)

    suspend fun countSubmitted(): Int = requestDao.countByStatus(RequestStatus.SUBMITTED)

    suspend fun submitRequest(
        beneficiaryOrgId: Long,
        category: FoodCategory,
        requestedQuantityKg: Double,
        numberOfBeneficiaries: Int,
        neededByEpochMillis: Long,
        motivation: String?
    ): OperationResult<Unit> = safeCall("Could not submit your request") {
        // Server-side-style re-check: never trust that the UI already enforced this -
        // the core data-integrity rule must hold no matter how this function is called.
        val org = orgDao.findById(beneficiaryOrgId) ?: throw IllegalStateException("Organisation not found.")
        if (org.vettingStatus != VettingStatus.APPROVED) {
            throw IllegalStateException("Your organisation must be approved by FoodForward SA before you can submit food requests.")
        }
        if (neededByEpochMillis <= System.currentTimeMillis()) {
            throw IllegalArgumentException("Needed-by date must be in the future.")
        }

        requestDao.insert(
            FoodRequest(
                beneficiaryOrganisationId = beneficiaryOrgId,
                category = category,
                requestedQuantityKg = requestedQuantityKg,
                numberOfBeneficiaries = numberOfBeneficiaries,
                neededByEpochMillis = neededByEpochMillis,
                motivation = motivation,
                status = RequestStatus.SUBMITTED
            )
        )
    }

    /** Approves a request and allocates stock from [donationId] atomically (never below zero). */
    suspend fun approve(requestId: Long, donationId: Long, adminNote: String?): OperationResult<Unit> =
        safeCall("Could not approve this request") {
            val request = requestDao.findById(requestId) ?: throw IllegalStateException("Request not found.")
            val donation = donationDao.findById(donationId) ?: throw IllegalStateException("Donation not found.")

            if (donation.remainingQuantityKg < request.requestedQuantityKg) {
                throw IllegalStateException("Insufficient stock: only ${donation.remainingQuantityKg}kg remains for this donation.")
            }

            donationDao.update(donation.copy(remainingQuantityKg = donation.remainingQuantityKg - request.requestedQuantityKg))
            requestDao.update(
                request.copy(
                    status = RequestStatus.APPROVED,
                    linkedDonationId = donationId,
                    adminNote = adminNote,
                    reviewedAtEpochMillis = System.currentTimeMillis()
                )
            )
        }

    suspend fun decline(requestId: Long, adminNote: String?): OperationResult<Unit> =
        safeCall("Could not decline this request") {
            val request = requestDao.findById(requestId) ?: throw IllegalStateException("Request not found.")
            requestDao.update(
                request.copy(
                    status = RequestStatus.DECLINED,
                    adminNote = adminNote,
                    reviewedAtEpochMillis = System.currentTimeMillis()
                )
            )
        }
}
