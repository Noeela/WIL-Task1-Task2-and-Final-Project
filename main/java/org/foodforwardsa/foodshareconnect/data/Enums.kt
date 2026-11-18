package org.foodforwardsa.foodshareconnect.data

/** Admin = FoodForward SA staff (back-end). BeneficiaryOrg = community organisation user (front-end). */
enum class UserRole { ADMIN, BENEFICIARY_ORG }

/** Vetting status of a Beneficiary Organisation, controlled by an Admin. */
enum class VettingStatus { PENDING_REVIEW, APPROVED, REJECTED, SUSPENDED }

/** Lifecycle of a Food Request submitted by a Beneficiary Organisation. */
enum class RequestStatus { SUBMITTED, APPROVED, DECLINED, CANCELLED }

/** Broad food categories, used for both donations and requests so perishable stock can be matched. */
enum class FoodCategory {
    FRESH_PRODUCE, BAKERY, DAIRY_AND_EGGS, MEAT_AND_PROTEIN,
    DRY_GOODS_AND_STAPLES, BEVERAGES, NON_FOOD_GROCERIES;

    fun display(): String = name.split("_").joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }
}

fun VettingStatus.display(): String = when (this) {
    VettingStatus.PENDING_REVIEW -> "Pending Review"
    VettingStatus.APPROVED -> "Approved"
    VettingStatus.REJECTED -> "Rejected"
    VettingStatus.SUSPENDED -> "Suspended"
}

fun RequestStatus.display(): String = when (this) {
    RequestStatus.SUBMITTED -> "Submitted"
    RequestStatus.APPROVED -> "Approved"
    RequestStatus.DECLINED -> "Declined"
    RequestStatus.CANCELLED -> "Cancelled"
}
