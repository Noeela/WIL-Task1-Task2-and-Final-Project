package org.foodforwardsa.foodshareconnect.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name
    @TypeConverter
    fun toUserRole(value: String): UserRole = UserRole.valueOf(value)

    @TypeConverter
    fun fromVettingStatus(value: VettingStatus): String = value.name
    @TypeConverter
    fun toVettingStatus(value: String): VettingStatus = VettingStatus.valueOf(value)

    @TypeConverter
    fun fromRequestStatus(value: RequestStatus): String = value.name
    @TypeConverter
    fun toRequestStatus(value: String): RequestStatus = RequestStatus.valueOf(value)

    @TypeConverter
    fun fromFoodCategory(value: FoodCategory): String = value.name
    @TypeConverter
    fun toFoodCategory(value: String): FoodCategory = FoodCategory.valueOf(value)
}
