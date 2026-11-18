package org.foodforwardsa.foodshareconnect.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.foodforwardsa.foodshareconnect.data.RequestStatus
import org.foodforwardsa.foodshareconnect.data.entity.FoodRequest
import org.foodforwardsa.foodshareconnect.data.entity.FoodRequestWithOrgName

@Dao
interface FoodRequestDao {

    @Query("SELECT * FROM food_requests WHERE beneficiaryOrganisationId = :orgId ORDER BY submittedAtEpochMillis DESC")
    fun observeForOrganisation(orgId: Long): Flow<List<FoodRequest>>

    @Query("""
        SELECT r.id, r.beneficiaryOrganisationId, o.organisationName, r.category, r.requestedQuantityKg,
               r.numberOfBeneficiaries, r.neededByEpochMillis, r.motivation, r.status, r.adminNote, r.submittedAtEpochMillis
        FROM food_requests r
        INNER JOIN beneficiary_organisations o ON o.id = r.beneficiaryOrganisationId
        WHERE r.status = :status
        ORDER BY r.neededByEpochMillis ASC
    """)
    fun observeByStatusWithOrgName(status: RequestStatus): Flow<List<FoodRequestWithOrgName>>

    @Query("SELECT * FROM food_requests WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): FoodRequest?

    @Query("""
        SELECT r.id, r.beneficiaryOrganisationId, o.organisationName, r.category, r.requestedQuantityKg,
               r.numberOfBeneficiaries, r.neededByEpochMillis, r.motivation, r.status, r.adminNote, r.submittedAtEpochMillis
        FROM food_requests r
        INNER JOIN beneficiary_organisations o ON o.id = r.beneficiaryOrganisationId
        WHERE r.id = :id LIMIT 1
    """)
    suspend fun findByIdWithOrgName(id: Long): FoodRequestWithOrgName?

    @Query("SELECT COUNT(*) FROM food_requests WHERE status = :status")
    suspend fun countByStatus(status: RequestStatus): Int

    @Insert
    suspend fun insert(request: FoodRequest): Long

    @Update
    suspend fun update(request: FoodRequest)
}
