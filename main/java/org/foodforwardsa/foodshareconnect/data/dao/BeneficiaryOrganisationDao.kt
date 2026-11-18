package org.foodforwardsa.foodshareconnect.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.foodforwardsa.foodshareconnect.data.VettingStatus
import org.foodforwardsa.foodshareconnect.data.entity.BeneficiaryOrganisation

@Dao
interface BeneficiaryOrganisationDao {

    @Query("SELECT * FROM beneficiary_organisations ORDER BY registeredAtEpochMillis DESC")
    fun observeAll(): Flow<List<BeneficiaryOrganisation>>

    @Query("SELECT * FROM beneficiary_organisations WHERE vettingStatus = :status ORDER BY registeredAtEpochMillis DESC")
    fun observeByStatus(status: VettingStatus): Flow<List<BeneficiaryOrganisation>>

    @Query("SELECT * FROM beneficiary_organisations WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): BeneficiaryOrganisation?

    @Query("SELECT COUNT(*) FROM beneficiary_organisations WHERE email = :email OR npoRegistrationNumber = :npoRegNo")
    suspend fun countDuplicates(email: String, npoRegNo: String): Int

    @Insert
    suspend fun insert(org: BeneficiaryOrganisation): Long

    @Update
    suspend fun update(org: BeneficiaryOrganisation)

    @Query("SELECT COUNT(*) FROM beneficiary_organisations WHERE vettingStatus = :status")
    suspend fun countByStatus(status: VettingStatus): Int
}
