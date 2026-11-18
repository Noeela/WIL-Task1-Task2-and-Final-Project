package org.foodforwardsa.foodshareconnect.data.repository

import kotlinx.coroutines.flow.Flow
import org.foodforwardsa.foodshareconnect.data.AppDatabase
import org.foodforwardsa.foodshareconnect.data.VettingStatus
import org.foodforwardsa.foodshareconnect.data.entity.BeneficiaryOrganisation
import org.foodforwardsa.foodshareconnect.util.OperationResult
import org.foodforwardsa.foodshareconnect.util.safeCall

class BeneficiaryOrgRepository(private val db: AppDatabase) {

    private val orgDao = db.beneficiaryOrganisationDao()

    fun observeAll(): Flow<List<BeneficiaryOrganisation>> = orgDao.observeAll()

    fun observeByStatus(status: VettingStatus): Flow<List<BeneficiaryOrganisation>> = orgDao.observeByStatus(status)

    suspend fun getById(id: Long): BeneficiaryOrganisation? = orgDao.findById(id)

    suspend fun updateVettingStatus(orgId: Long, newStatus: VettingStatus, adminNote: String?): OperationResult<Unit> =
        safeCall("Could not update this organisation") {
            val org = orgDao.findById(orgId) ?: throw IllegalStateException("Organisation not found.")
            orgDao.update(org.copy(vettingStatus = newStatus, adminNote = adminNote))
        }

    suspend fun countPending(): Int = orgDao.countByStatus(VettingStatus.PENDING_REVIEW)
    suspend fun countApproved(): Int = orgDao.countByStatus(VettingStatus.APPROVED)
}
