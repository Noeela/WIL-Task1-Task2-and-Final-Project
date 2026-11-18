package org.foodforwardsa.foodshareconnect.data.entity

import org.foodforwardsa.foodshareconnect.data.FoodCategory
import org.foodforwardsa.foodshareconnect.data.RequestStatus

/** Flat projection used by the Admin triage screens, so each row already carries the organisation's name. */
data class FoodRequestWithOrgName(
    val id: Long,
    val beneficiaryOrganisationId: Long,
    val organisationName: String,
    val category: FoodCategory,
    val requestedQuantityKg: Double,
    val numberOfBeneficiaries: Int,
    val neededByEpochMillis: Long,
    val motivation: String?,
    val status: RequestStatus,
    val adminNote: String?,
    val submittedAtEpochMillis: Long
)
