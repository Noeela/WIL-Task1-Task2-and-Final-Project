package org.foodforwardsa.foodshareconnect.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.foodforwardsa.foodshareconnect.data.FoodCategory
import org.foodforwardsa.foodshareconnect.data.RequestStatus

@Entity(
    tableName = "food_requests",
    indices = [Index("beneficiaryOrganisationId"), Index("linkedDonationId")],
    foreignKeys = [
        ForeignKey(
            entity = BeneficiaryOrganisation::class,
            parentColumns = ["id"],
            childColumns = ["beneficiaryOrganisationId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodDonation::class,
            parentColumns = ["id"],
            childColumns = ["linkedDonationId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class FoodRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val beneficiaryOrganisationId: Long,
    val category: FoodCategory,
    val requestedQuantityKg: Double,
    val numberOfBeneficiaries: Int,
    val neededByEpochMillis: Long,
    val motivation: String? = null,
    val status: RequestStatus = RequestStatus.SUBMITTED,
    val adminNote: String? = null,
    val linkedDonationId: Long? = null,
    val submittedAtEpochMillis: Long = System.currentTimeMillis(),
    val reviewedAtEpochMillis: Long? = null
)
