package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class UserRole(val label: String, val badgeColor: Long) {
    OWNER("Owner / Direksi", 0xFFD97706),
    HEAD_WAREHOUSE("Kepala Gudang Utama", 0xFF0284C7),
    SITE_STAFF("Mekanik / Staf Site Cabang", 0xFF10B981)
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val warehouseId: String,
    val warehouseName: String,
    val token: String
)

enum class PartCategory(val label: String) {
    HEAVY_EQUIPMENT_SPAREPART("Suku Cadang Alat Berat"),
    UNDERCARRIAGE("Undercarriage & Track"),
    HYDRAULIC("Sistem Hidrolik"),
    LUBRICANT("Pelumas & Filter"),
    CONSUMABLE("Consumable Tambang")
}

@Entity(
    tableName = "stocks",
    indices = [
        Index(value = ["warehouseId"]),
        Index(value = ["sku"]),
        Index(value = ["category"])
    ]
)
data class StockItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sku: String,
    val partName: String,
    val category: PartCategory,
    val warehouseId: String,
    val warehouseName: String,
    val quantity: Int,
    val unit: String,
    val minStockLevel: Int,
    val pricePerUnit: Long,
    val compatibleUnits: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

enum class TransferStatus(val label: String) {
    DRAFT("Draft"),
    IN_TRANSIT("Dalam Pengiriman"),
    RECEIVED("Diterima di Cabang"),
    CANCELLED("Dibatalkan")
}

@Entity(
    tableName = "stock_transfers",
    indices = [
        Index(value = ["fromWarehouseId"]),
        Index(value = ["toWarehouseId"]),
        Index(value = ["transferCode"]),
        Index(value = ["timestamp"])
    ]
)
data class StockTransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transferCode: String,
    val fromWarehouseId: String,
    val fromWarehouseName: String,
    val toWarehouseId: String,
    val toWarehouseName: String,
    val partId: Long,
    val partSku: String,
    val partName: String,
    val quantity: Int,
    val status: TransferStatus,
    val batchNumber: String,
    val driverName: String,
    val notes: String,
    val isLockedForUpdate: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val receivedAt: Long? = null
)

enum class EquipmentStatus(val label: String) {
    OPERATIONAL("Beroperasi Normal"),
    BREAKDOWN_MAINTENANCE("Breakdown / Perbaikan"),
    STANDBY("Standby Siap Kerja")
}

@Entity(
    tableName = "equipment_units",
    indices = [
        Index(value = ["unitCode"], unique = true),
        Index(value = ["siteLocation"])
    ]
)
data class EquipmentUnitEntity(
    @PrimaryKey val unitCode: String, // e.g. "EX-201", "DT-104", "DZ-05"
    val unitName: String,             // e.g. "Excavator Komatsu PC200-8"
    val modelType: String,            // e.g. "Hydraulic Excavator"
    val siteLocation: String,         // e.g. "Site Sangatta"
    val status: EquipmentStatus,
    val currentHourMeter: Double,     // e.g. 12450.5 HM
    val operatorName: String,
    val lastServiceDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "part_usages",
    indices = [
        Index(value = ["warehouseId"]),
        Index(value = ["equipmentUnitCode"]),
        Index(value = ["createdAt"])
    ]
)
data class PartUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val equipmentUnitCode: String,
    val equipmentUnitName: String,
    val warehouseId: String,
    val warehouseName: String,
    val partId: Long,
    val partSku: String,
    val partName: String,
    val quantity: Int,
    val unit: String,
    val hourMeterAtUsage: Double,
    val mechanicName: String,
    val workOrderNo: String,
    val reason: String,
    val totalCost: Long,
    val createdAt: Long = System.currentTimeMillis()
)

enum class NonInventoryCategory(val label: String) {
    SEMBAKO_DAPUR("Sembako Dapur Mess"),
    OPERATIONAL_FUEL("BBM Motor Operasional"),
    GENERAL_AFFAIR("Perlengkapan Umum / GA")
}

@Entity(
    tableName = "non_inventory_items",
    indices = [
        Index(value = ["warehouseId"]),
        Index(value = ["category"]),
        Index(value = ["recordedAt"])
    ]
)
data class NonInventoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String,
    val category: NonInventoryCategory,
    val warehouseId: String,
    val warehouseName: String,
    val quantity: Double,
    val unit: String,
    val amountRupiah: Long,
    val picName: String,
    val notes: String,
    val recordedAt: Long = System.currentTimeMillis()
)

enum class NotificationSeverity {
    INFO, WARNING, CRITICAL, SUCCESS
}

@Entity(
    tableName = "realtime_notifications",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["isRead"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val eventType: String, // "BARANG_MASUK", "LOW_STOCK", "BREAKDOWN_UNIT", "API_WEBHOOK_DISPATCH", "DB_LOCK_HELD"
    val severity: NotificationSeverity,
    val source: String, // "Gudang Utama", "Site Morowali", "SAP S/4HANA", "Telegram Gateway"
    val isRead: Boolean = false,
    val payloadJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "third_party_integrations")
data class ThirdPartyIntegrationEntity(
    @PrimaryKey val id: String, // "sap-erp", "telegram-bot", "waba-gateway", "fleet-fms"
    val name: String,
    val description: String,
    val endpointUrl: String,
    val apiKeyMasked: String,
    val isEnabled: Boolean = true,
    val notifyOnTransfer: Boolean = true,
    val notifyOnReceive: Boolean = true,
    val notifyOnUsage: Boolean = true,
    val notifyOnLowStock: Boolean = true,
    val lastStatusCode: Int? = 200,
    val lastLatencyMs: Long? = 64,
    val lastPayloadPreview: String = "{}",
    val lastSyncTime: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "webhook_dispatch_logs",
    indices = [Index(value = ["timestamp"])]
)
data class WebhookLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val integrationName: String,
    val endpointUrl: String,
    val eventType: String,
    val httpStatus: Int,
    val statusText: String,
    val latencyMs: Long,
    val requestPayloadJson: String,
    val responseBodyJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
