package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.EquipmentStatus
import com.example.data.model.NonInventoryCategory
import com.example.data.model.NotificationSeverity
import com.example.data.model.PartCategory
import com.example.data.model.TransferStatus
import com.example.data.model.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = runCatching { UserRole.valueOf(value) }.getOrDefault(UserRole.SITE_STAFF)

    @TypeConverter
    fun fromPartCategory(value: PartCategory): String = value.name

    @TypeConverter
    fun toPartCategory(value: String): PartCategory = runCatching { PartCategory.valueOf(value) }.getOrDefault(PartCategory.HEAVY_EQUIPMENT_SPAREPART)

    @TypeConverter
    fun fromTransferStatus(value: TransferStatus): String = value.name

    @TypeConverter
    fun toTransferStatus(value: String): TransferStatus = runCatching { TransferStatus.valueOf(value) }.getOrDefault(TransferStatus.DRAFT)

    @TypeConverter
    fun fromEquipmentStatus(value: EquipmentStatus): String = value.name

    @TypeConverter
    fun toEquipmentStatus(value: String): EquipmentStatus = runCatching { EquipmentStatus.valueOf(value) }.getOrDefault(EquipmentStatus.OPERATIONAL)

    @TypeConverter
    fun fromNonInventoryCategory(value: NonInventoryCategory): String = value.name

    @TypeConverter
    fun toNonInventoryCategory(value: String): NonInventoryCategory = runCatching { NonInventoryCategory.valueOf(value) }.getOrDefault(NonInventoryCategory.SEMBAKO_DAPUR)

    @TypeConverter
    fun fromNotificationSeverity(value: NotificationSeverity): String = value.name

    @TypeConverter
    fun toNotificationSeverity(value: String): NotificationSeverity = runCatching { NotificationSeverity.valueOf(value) }.getOrDefault(NotificationSeverity.INFO)
}
