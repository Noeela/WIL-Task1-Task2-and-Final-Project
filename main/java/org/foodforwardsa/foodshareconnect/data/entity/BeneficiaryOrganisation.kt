package org.foodforwardsa.foodshareconnect.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.foodforwardsa.foodshareconnect.data.VettingStatus

@Entity(
    tableName = "beneficiary_organisations",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["npoRegistrationNumber"], unique = true)
    ]
)
data class BeneficiaryOrganisation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val organisationName: String,
    val contactPersonName: String,
    val email: String,
    val phoneNumber: String,
    val physicalAddress: String,
    val province: String,
    val npoRegistrationNumber: String,
    val estimatedBeneficiaries: Int,
    val vettingStatus: VettingStatus = VettingStatus.PENDING_REVIEW,
    val adminNote: String? = null,
    val registeredAtEpochMillis: Long = System.currentTimeMillis()
)
