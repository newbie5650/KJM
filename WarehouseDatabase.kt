package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.EquipmentStatus
import com.example.data.model.EquipmentUnitEntity
import com.example.data.model.NonInventoryCategory
import com.example.data.model.NonInventoryEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.NotificationSeverity
import com.example.data.model.PartCategory
import com.example.data.model.PartUsageEntity
import com.example.data.model.StockItemEntity
import com.example.data.model.StockTransferEntity
import com.example.data.model.ThirdPartyIntegrationEntity
import com.example.data.model.TransferStatus
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.WebhookLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        StockItemEntity::class,
        StockTransferEntity::class,
        EquipmentUnitEntity::class,
        PartUsageEntity::class,
        NonInventoryEntity::class,
        NotificationEntity::class,
        ThirdPartyIntegrationEntity::class,
        WebhookLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WarehouseDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun stockDao(): StockDao
    abstract fun transferDao(): TransferDao
    abstract fun equipmentDao(): EquipmentDao
    abstract fun usageDao(): UsageDao
    abstract fun nonInventoryDao(): NonInventoryDao
    abstract fun notificationDao(): NotificationDao
    abstract fun integrationDao(): IntegrationDao

    companion object {
        @Volatile
        private var INSTANCE: WarehouseDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): WarehouseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WarehouseDatabase::class.java,
                    "wms_heavy_equipment.db"
                )
                    .addCallback(WarehouseDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class WarehouseDatabaseCallback(
        private val scope: CoroutineScope
    ) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(db: WarehouseDatabase) {
            val now = System.currentTimeMillis()

            // 1. Users matching blueprint
            db.userDao().insertUsers(
                listOf(
                    UserEntity("USR-01", "Pak Budi Hartono", "owner@miningcorp.co.id", UserRole.OWNER, "HQ-01", "Kantor Pusat / Direksi", "jwt_token_owner_secure_2026"),
                    UserEntity("USR-02", "Hendra Saputra", "wh.principal@miningcorp.co.id", UserRole.HEAD_WAREHOUSE, "WH-MAIN", "Gudang Utama Balikpapan", "jwt_token_wh_principal_2026"),
                    UserEntity("USR-03", "Agus Sutanto (Mekanik)", "site.morowali@miningcorp.co.id", UserRole.SITE_STAFF, "SITE-MOR", "Site Tambang Morowali", "jwt_token_site_staff_2026"),
                    UserEntity("USR-04", "Rian Pratama (Mekanik)", "site.sangatta@miningcorp.co.id", UserRole.SITE_STAFF, "SITE-SNG", "Site Tambang Sangatta", "jwt_token_site_staff_sng_2026")
                )
            )

            // 2. Heavy Equipment Fleet
            db.equipmentDao().insertEquipmentList(
                listOf(
                    EquipmentUnitEntity("EX-201", "Komatsu PC200-8", "Hydraulic Excavator", "Site Tambang Morowali", EquipmentStatus.OPERATIONAL, 14250.0, "Slamet Riyadi", now - 86400000L * 5),
                    EquipmentUnitEntity("DT-104", "Scania P410 Heavy Tipper", "Off-Highway Dump Truck", "Site Tambang Morowali", EquipmentStatus.BREAKDOWN_MAINTENANCE, 22100.5, "Bambang M.", now - 86400000L * 2),
                    EquipmentUnitEntity("DZ-05", "Caterpillar D85-ESS", "Track Type Bulldozer", "Site Tambang Sangatta", EquipmentStatus.OPERATIONAL, 18900.0, "Dedi Kusuma", now - 86400000L * 12),
                    EquipmentUnitEntity("GR-12", "Caterpillar 140K", "Motor Grader Jalan Tambang", "Site Tambang Sangatta", EquipmentStatus.STANDBY, 8400.0, "Yanto Subekti", now - 86400000L * 30),
                    EquipmentUnitEntity("EX-305", "Hitachi ZX350H-5G", "Heavy Excavator", "Site Tambang Morowali", EquipmentStatus.OPERATIONAL, 11200.0, "Kurniawan", now - 86400000L * 15)
                )
            )

            // 3. Stocks in Principal Warehouse and Sites
            db.stockDao().insertStocks(
                listOf(
                    StockItemEntity(1, "FLT-SOL-01", "Fuel Filter Main PC200", PartCategory.LUBRICANT, "WH-MAIN", "Gudang Utama Balikpapan", 48, "PCS", 10, 385000L, "EX-201, EX-305"),
                    StockItemEntity(2, "FLT-OIL-02", "Engine Oil Filter Scania P410", PartCategory.LUBRICANT, "WH-MAIN", "Gudang Utama Balikpapan", 35, "PCS", 8, 420000L, "DT-104"),
                    StockItemEntity(3, "TRK-LNK-08", "Track Link Assy CAT D85", PartCategory.UNDERCARRIAGE, "WH-MAIN", "Gudang Utama Balikpapan", 6, "SET", 2, 28500000L, "DZ-05"),
                    StockItemEntity(4, "HYD-HSE-34", "Hydraulic High Pressure Hose 1\"", PartCategory.HYDRAULIC, "WH-MAIN", "Gudang Utama Balikpapan", 14, "PCS", 5, 1250000L, "EX-201, EX-305, DZ-05"),
                    StockItemEntity(5, "OIL-SAE-15W", "Oli Mesin Diesel Meditran SX 15W-40", PartCategory.LUBRICANT, "WH-MAIN", "Gudang Utama Balikpapan", 4, "DRUM", 10, 8500000L, "Semua Unit Alat Berat"),
                    StockItemEntity(6, "BCK-TTH-03", "Bucket Teeth Tiger PC200", PartCategory.HEAVY_EQUIPMENT_SPAREPART, "WH-MAIN", "Gudang Utama Balikpapan", 60, "PCS", 15, 650000L, "EX-201"),
                    // Stocks in Morowali Site
                    StockItemEntity(7, "FLT-SOL-01", "Fuel Filter Main PC200", PartCategory.LUBRICANT, "SITE-MOR", "Site Tambang Morowali", 6, "PCS", 4, 385000L, "EX-201, EX-305"),
                    StockItemEntity(8, "HYD-HSE-34", "Hydraulic High Pressure Hose 1\"", PartCategory.HYDRAULIC, "SITE-MOR", "Site Tambang Morowali", 1, "PCS", 2, 1250000L, "EX-201, EX-305"),
                    StockItemEntity(9, "FLT-OIL-02", "Engine Oil Filter Scania P410", PartCategory.LUBRICANT, "SITE-MOR", "Site Tambang Morowali", 2, "PCS", 3, 420000L, "DT-104")
                )
            )

            // 4. Initial Transfers (including IN_TRANSIT item for Site Morowali to receive)
            db.transferDao().insertTransfers(
                listOf(
                    StockTransferEntity(
                        id = 1,
                        transferCode = "TRF-2026-0091",
                        fromWarehouseId = "WH-MAIN",
                        fromWarehouseName = "Gudang Utama Balikpapan",
                        toWarehouseId = "SITE-MOR",
                        toWarehouseName = "Site Tambang Morowali",
                        partId = 4,
                        partSku = "HYD-HSE-34",
                        partName = "Hydraulic High Pressure Hose 1\"",
                        quantity = 3,
                        status = TransferStatus.IN_TRANSIT,
                        batchNumber = "BATCH-LOG-8812",
                        driverName = "Pak Joko (Ekspedisi Lintas Selat)",
                        notes = "Permintaan mendesak untuk perbaikan EX-201",
                        isLockedForUpdate = true,
                        timestamp = now - 3600000L * 4
                    ),
                    StockTransferEntity(
                        id = 2,
                        transferCode = "TRF-2026-0085",
                        fromWarehouseId = "WH-MAIN",
                        fromWarehouseName = "Gudang Utama Balikpapan",
                        toWarehouseId = "SITE-MOR",
                        toWarehouseName = "Site Tambang Morowali",
                        partId = 1,
                        partSku = "FLT-SOL-01",
                        partName = "Fuel Filter Main PC200",
                        quantity = 10,
                        status = TransferStatus.RECEIVED,
                        batchNumber = "BATCH-LOG-8799",
                        driverName = "PT Samudera Logistik",
                        notes = "Restok berkala mingguan",
                        isLockedForUpdate = false,
                        timestamp = now - 86400000L * 3,
                        receivedAt = now - 86400000L * 1
                    )
                )
            )

            // 5. Part Usages per Heavy Equipment Unit
            db.usageDao().insertUsages(
                listOf(
                    PartUsageEntity(
                        id = 1,
                        equipmentUnitCode = "EX-201",
                        equipmentUnitName = "Komatsu PC200-8",
                        warehouseId = "SITE-MOR",
                        warehouseName = "Site Tambang Morowali",
                        partId = 1,
                        partSku = "FLT-SOL-01",
                        partName = "Fuel Filter Main PC200",
                        quantity = 2,
                        unit = "PCS",
                        hourMeterAtUsage = 14200.0,
                        mechanicName = "Agus Sutanto",
                        workOrderNo = "WO-MOR-2026-104",
                        reason = "Perawatan Berkala 250 Jam",
                        totalCost = 770000L,
                        createdAt = now - 86400000L * 2
                    ),
                    PartUsageEntity(
                        id = 2,
                        equipmentUnitCode = "DT-104",
                        equipmentUnitName = "Scania P410 Heavy Tipper",
                        warehouseId = "SITE-MOR",
                        warehouseName = "Site Tambang Morowali",
                        partId = 2,
                        partSku = "FLT-OIL-02",
                        partName = "Engine Oil Filter Scania P410",
                        quantity = 1,
                        unit = "PCS",
                        hourMeterAtUsage = 22100.0,
                        mechanicName = "Agus Sutanto",
                        workOrderNo = "WO-MOR-2026-118",
                        reason = "Penggantian filter oli darurat",
                        totalCost = 420000L,
                        createdAt = now - 86400000L * 1
                    )
                )
            )

            // 6. Non-Inventory Items (Sembako dapur, motor operasional)
            db.nonInventoryDao().insertAll(
                listOf(
                    NonInventoryEntity(
                        id = 1,
                        itemName = "Beras Rojolele Super 50kg (10 Sak)",
                        category = NonInventoryCategory.SEMBAKO_DAPUR,
                        warehouseId = "SITE-MOR",
                        warehouseName = "Site Tambang Morowali",
                        quantity = 500.0,
                        unit = "KG",
                        amountRupiah = 7250000L,
                        picName = "Ibu Siti (Kepala Dapur Mess)",
                        notes = "Kebutuhan konsumsi katering 45 mekanik & operator site",
                        recordedAt = now - 86400000L * 3
                    ),
                    NonInventoryEntity(
                        id = 2,
                        itemName = "Bensin Pertalite Operasional Patroli Pit (KLX 150)",
                        category = NonInventoryCategory.OPERATIONAL_FUEL,
                        warehouseId = "SITE-MOR",
                        warehouseName = "Site Tambang Morowali",
                        quantity = 60.0,
                        unit = "LITER",
                        amountRupiah = 600000L,
                        picName = "Dodi (Staf GA Tambang)",
                        notes = "Untuk motor dinas pengawas tambang & mekanik keliling",
                        recordedAt = now - 86400000L * 1
                    )
                )
            )

            // 7. Initial Real-time Notifications
            db.notificationDao().insertNotifications(
                listOf(
                    NotificationEntity(
                        id = 1,
                        title = "⚠️ Stok Kritis Gudang Utama",
                        message = "Oli Mesin Meditran SX 15W-40 tersisa 4 Drum (Batas Kritis 10 Drum). Segera lakukan PO ke Principal.",
                        eventType = "LOW_STOCK",
                        severity = NotificationSeverity.WARNING,
                        source = "Gudang Utama Balikpapan",
                        isRead = false,
                        payloadJson = """{"sku":"OIL-SAE-15W","qty":4,"min":10}""",
                        createdAt = now - 3600000L * 2
                    ),
                    NotificationEntity(
                        id = 2,
                        title = "🚚 Mutasi Stok #TRF-2026-0091 Sedang Dikirim",
                        message = "Gudang Utama mengirim 3 PCS Hydraulic Hose 1\" menuju Site Tambang Morowali via Ekspedisi Lintas Selat.",
                        eventType = "BARANG_MASUK",
                        severity = NotificationSeverity.INFO,
                        source = "Gudang Utama Balikpapan",
                        isRead = false,
                        payloadJson = """{"transferCode":"TRF-2026-0091","partSku":"HYD-HSE-34","qty":3}""",
                        createdAt = now - 3600000L * 4
                    ),
                    NotificationEntity(
                        id = 3,
                        title = "🌐 Webhook SAP ERP Berhasil Terkirim",
                        message = "Event ON_TRANSFER_OUT disinkronkan ke SAP S/4HANA (HTTP 200 OK - Latency 48ms).",
                        eventType = "API_WEBHOOK_DISPATCH",
                        severity = NotificationSeverity.SUCCESS,
                        source = "SAP S/4HANA Gateway",
                        isRead = true,
                        payloadJson = """{"status":200,"endpoint":"/api/v2/inventory/sync"}""",
                        createdAt = now - 3600000L * 5
                    )
                )
            )

            // 8. Third-Party Integrations
            db.integrationDao().insertIntegrations(
                listOf(
                    ThirdPartyIntegrationEntity(
                        id = "sap-erp",
                        name = "ERP SAP S/4HANA Enterprise",
                        description = "Sinkronisasi otomatis mutasi stok, GI (Goods Issue) dan GR (Goods Receipt) ke sistem ERP korporat.",
                        endpointUrl = "https://erp.miningcorp.internal/api/v2/inventory/sync",
                        apiKeyMasked = "sap_sec_99a8****************3b",
                        isEnabled = true,
                        notifyOnTransfer = true,
                        notifyOnReceive = true,
                        notifyOnUsage = true,
                        notifyOnLowStock = false,
                        lastStatusCode = 200,
                        lastLatencyMs = 48,
                        lastPayloadPreview = """{"event":"MUTATION_OUT","code":"TRF-2026-0091","sapDoc":"MAT_DOC_98124"}""",
                        lastSyncTime = now - 3600000L * 5
                    ),
                    ThirdPartyIntegrationEntity(
                        id = "telegram-bot",
                        name = "Telegram Bot Alert Site & Owner",
                        description = "Kirim notifikasi pesan instan ke Channel Telegram Pengawas & Owner saat ada alat berat breakdown atau stok kritis.",
                        endpointUrl = "https://api.telegram.org/bot<CONFIGURE_TOKEN>/sendMessage",
                        apiKeyMasked = "bot_tok_7819****************92",
                        isEnabled = true,
                        notifyOnTransfer = false,
                        notifyOnReceive = true,
                        notifyOnUsage = true,
                        notifyOnLowStock = true,
                        lastStatusCode = 200,
                        lastLatencyMs = 120,
                        lastPayloadPreview = """{"chat_id":"-1008492041","text":"⚠️ [BREAKDOWN] DT-104 Scania P410 membutuhkan filter oli di Site Morowali"}""",
                        lastSyncTime = now - 3600000L * 2
                    ),
                    ThirdPartyIntegrationEntity(
                        id = "waba-gateway",
                        name = "WhatsApp Business Notification API",
                        description = "Pemberitahuan otomatis ke WhatsApp Kepala Mekanik & Driver ekspedisi saat Surat Jalan diterbitkan.",
                        endpointUrl = "https://waba.service-provider.com/v1/messages",
                        apiKeyMasked = "waba_prod_****************ef",
                        isEnabled = true,
                        notifyOnTransfer = true,
                        notifyOnReceive = true,
                        notifyOnUsage = false,
                        notifyOnLowStock = false,
                        lastStatusCode = 200,
                        lastLatencyMs = 85,
                        lastPayloadPreview = """{"to":"+62811559988","template":"sp_terima_barang","params":["TRF-2026-0091"]}""",
                        lastSyncTime = now - 3600000L * 4
                    ),
                    ThirdPartyIntegrationEntity(
                        id = "fleet-fms",
                        name = "Mining Fleet Management System (FMS)",
                        description = "Integrasi telematika Hour Meter alat berat dan riwayat penggantian suku cadang langsung ke sensor armada.",
                        endpointUrl = "https://fms-telemetry.miningcorp.internal/telematics/part-installed",
                        apiKeyMasked = "fms_api_****************40",
                        isEnabled = true,
                        notifyOnTransfer = false,
                        notifyOnReceive = false,
                        notifyOnUsage = true,
                        notifyOnLowStock = false,
                        lastStatusCode = 200,
                        lastLatencyMs = 38,
                        lastPayloadPreview = """{"unitCode":"EX-201","hourMeter":14200.0,"partSku":"FLT-SOL-01"}""",
                        lastSyncTime = now - 86400000L * 2
                    )
                )
            )

            // Initial Webhook Logs
            db.integrationDao().insertLog(
                WebhookLogEntity(
                    integrationName = "ERP SAP S/4HANA Enterprise",
                    endpointUrl = "https://erp.miningcorp.internal/api/v2/inventory/sync",
                    eventType = "ON_TRANSFER_OUT",
                    httpStatus = 200,
                    statusText = "OK",
                    latencyMs = 48,
                    requestPayloadJson = """{"eventId":"EVT-9021","event":"TRANSFER_OUT","sku":"HYD-HSE-34","qty":3,"dest":"SITE-MOR"}""",
                    responseBodyJson = """{"success":true,"sapDocId":"SAP-MAT-2026-8819","posted":true}""",
                    timestamp = now - 3600000L * 4
                )
            )
        }
    }
}
