package com.example.data.repository

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.local.WarehouseDatabase
import com.example.data.model.EquipmentStatus
import com.example.data.model.EquipmentUnitEntity
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
import com.example.data.model.WebhookLogEntity
import com.example.data.network.WebhookDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

class WarehouseRepository(
    private val database: WarehouseDatabase,
    private val context: Context,
    private val externalScope: CoroutineScope
) {
    private val stockDao = database.stockDao()
    private val transferDao = database.transferDao()
    private val equipmentDao = database.equipmentDao()
    private val usageDao = database.usageDao()
    private val nonInventoryDao = database.nonInventoryDao()
    private val notificationDao = database.notificationDao()
    private val integrationDao = database.integrationDao()
    private val userDao = database.userDao()

    private val webhookDispatcher = WebhookDispatcher(integrationDao)

    // Simulates the Database Row-Level Locking (SELECT ... FOR UPDATE) described in Rule 1
    private val transferLockMutex = Mutex()

    // Query streams
    val allStocks: Flow<List<StockItemEntity>> = stockDao.getAllStocks()
    val allTransfers: Flow<List<StockTransferEntity>> = transferDao.getAllTransfers()
    val allEquipment: Flow<List<EquipmentUnitEntity>> = equipmentDao.getAllEquipment()
    val allUsages: Flow<List<PartUsageEntity>> = usageDao.getAllUsages()
    val allNonInventory: Flow<List<NonInventoryEntity>> = nonInventoryDao.getAllNonInventory()
    val allNotifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    val unreadNotificationsCount: Flow<Int> = notificationDao.getUnreadCount()
    val allIntegrations: Flow<List<ThirdPartyIntegrationEntity>> = integrationDao.getAllIntegrations()
    val recentWebhookLogs: Flow<List<WebhookLogEntity>> = integrationDao.getRecentLogs()
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()

    fun getStocksForWarehouse(warehouseId: String): Flow<List<StockItemEntity>> =
        stockDao.getStocksByWarehouse(warehouseId)

    fun getIncomingTransfersForSite(siteWarehouseId: String): Flow<List<StockTransferEntity>> =
        transferDao.getIncomingTransfersForSite(siteWarehouseId)

    /**
     * Executes safe stock transfer with Database Locking (FOR UPDATE)
     * Prevents race conditions / double spend when two staff click simultaneously.
     */
    suspend fun createStockTransfer(
        partId: Long,
        fromWarehouseId: String,
        fromWarehouseName: String,
        toWarehouseId: String,
        toWarehouseName: String,
        quantity: Int,
        driverName: String,
        notes: String
    ): Result<StockTransferEntity> = withContext(Dispatchers.IO) {
        // Acquire Row Lock (Simulates SELECT FOR UPDATE)
        transferLockMutex.withLock {
            val stock = stockDao.getStockById(partId)
                ?: return@withContext Result.failure(Exception("Suku cadang tidak ditemukan di katalog"))

            if (stock.quantity < quantity) {
                return@withContext Result.failure(
                    Exception("Stok tidak mencukupi! Tersedia: ${stock.quantity} ${stock.unit}, diminta: $quantity")
                )
            }

            // Deduct sender warehouse stock atomically
            val updatedRows = stockDao.deductStockSafely(partId, quantity)
            if (updatedRows == 0) {
                return@withContext Result.failure(
                    Exception("Gagal mengunci baris database (Database Lock Conflict). Silakan coba lagi.")
                )
            }

            val transferCode = "TRF-${System.currentTimeMillis().toString().takeLast(6)}"
            val batchNumber = "BATCH-${UUID.randomUUID().toString().take(6).uppercase()}"

            val transfer = StockTransferEntity(
                transferCode = transferCode,
                fromWarehouseId = fromWarehouseId,
                fromWarehouseName = fromWarehouseName,
                toWarehouseId = toWarehouseId,
                toWarehouseName = toWarehouseName,
                partId = partId,
                partSku = stock.sku,
                partName = stock.partName,
                quantity = quantity,
                status = TransferStatus.IN_TRANSIT,
                batchNumber = batchNumber,
                driverName = driverName,
                notes = notes,
                isLockedForUpdate = true,
                timestamp = System.currentTimeMillis()
            )

            val transferId = transferDao.insertTransfer(transfer)
            val savedTransfer = transfer.copy(id = transferId)

            // Trigger Real-time Notification
            triggerNotification(
                title = "🚚 Pengiriman Baru: $transferCode",
                message = "Gudang Utama mengirim $quantity ${stock.unit} ${stock.partName} menuju $toWarehouseName (Driver: $driverName).",
                eventType = "BARANG_MASUK",
                severity = NotificationSeverity.INFO,
                source = fromWarehouseName,
                payloadJson = """{
                    "transferCode": "$transferCode",
                    "partSku": "${stock.sku}",
                    "qty": $quantity,
                    "toWarehouse": "$toWarehouseName",
                    "batch": "$batchNumber",
                    "dbLockProtected": true
                }""".trimIndent()
            )

            // Check if stock in principal warehouse is now low
            val remainingStock = stock.quantity - quantity
            if (remainingStock <= stock.minStockLevel) {
                triggerNotification(
                    title = "⚠️ Peringatan Stok Kritis!",
                    message = "Stok ${stock.partName} di $fromWarehouseName tersisa $remainingStock ${stock.unit} (Di bawah ambang minimum ${stock.minStockLevel} ${stock.unit}).",
                    eventType = "LOW_STOCK",
                    severity = NotificationSeverity.WARNING,
                    source = fromWarehouseName,
                    payloadJson = """{"sku":"${stock.sku}","remaining":$remainingStock,"min":${stock.minStockLevel}}"""
                )
                dispatchToIntegrations(
                    eventType = "ON_LOW_STOCK",
                    filterPredicate = { it.notifyOnLowStock },
                    payload = """{
                        "event": "CRITICAL_LOW_STOCK",
                        "sku": "${stock.sku}",
                        "partName": "${stock.partName}",
                        "remainingQuantity": $remainingStock,
                        "minThreshold": ${stock.minStockLevel},
                        "warehouse": "$fromWarehouseName"
                    }""".trimIndent()
                )
            }

            // Dispatch Third-party Webhook for Stock Transfer
            dispatchToIntegrations(
                eventType = "ON_TRANSFER_OUT",
                filterPredicate = { it.notifyOnTransfer },
                payload = """{
                    "event": "STOCK_TRANSFER_OUT",
                    "transferCode": "$transferCode",
                    "fromWarehouse": "$fromWarehouseId",
                    "toWarehouse": "$toWarehouseId",
                    "partSku": "${stock.sku}",
                    "partName": "${stock.partName}",
                    "quantity": $quantity,
                    "batchNumber": "$batchNumber",
                    "driver": "$driverName",
                    "timestamp": ${System.currentTimeMillis()}
                }""".trimIndent()
            )

            Result.success(savedTransfer)
        }
    }

    /**
     * Confirm receipt of items at branch / site
     */
    suspend fun receiveTransferAtSite(
        transferId: Long,
        receiverPicName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val transfer = transferDao.getTransferById(transferId)
            ?: return@withContext Result.failure(Exception("Data pengiriman tidak ditemukan"))

        if (transfer.status == TransferStatus.RECEIVED) {
            return@withContext Result.failure(Exception("Barang ini sudah pernah diterima sebelumnya"))
        }

        // 1. Update transfer status
        transferDao.markReceived(transferId)

        // 2. Add stock to the destination site warehouse
        val existingSiteStock = stockDao.getStockByWarehouseAndSku(transfer.toWarehouseId, transfer.partSku)
        if (existingSiteStock != null) {
            stockDao.addStockBySku(transfer.toWarehouseId, transfer.partSku, transfer.quantity)
        } else {
            // Get sample price from principal stock
            val origStock = stockDao.getStockById(transfer.partId)
            stockDao.insertStock(
                StockItemEntity(
                    sku = transfer.partSku,
                    partName = transfer.partName,
                    category = origStock?.category ?: PartCategory.HEAVY_EQUIPMENT_SPAREPART,
                    warehouseId = transfer.toWarehouseId,
                    warehouseName = transfer.toWarehouseName,
                    quantity = transfer.quantity,
                    unit = origStock?.unit ?: "PCS",
                    minStockLevel = 2,
                    pricePerUnit = origStock?.pricePerUnit ?: 500000L,
                    compatibleUnits = origStock?.compatibleUnits ?: "Excavator/Dump Truck"
                )
            )
        }

        // 3. Trigger Real-time Notification
        triggerNotification(
            title = "✅ Barang Diterima di Site!",
            message = "Surat Jalan ${transfer.transferCode} (${transfer.quantity}x ${transfer.partName}) telah berhasil diverifikasi dan diterima di ${transfer.toWarehouseName} oleh $receiverPicName.",
            eventType = "BARANG_MASUK",
            severity = NotificationSeverity.SUCCESS,
            source = transfer.toWarehouseName,
            payloadJson = """{
                "transferCode": "${transfer.transferCode}",
                "status": "RECEIVED",
                "receiver": "$receiverPicName",
                "receivedAt": ${System.currentTimeMillis()}
            }""".trimIndent()
        )

        // 4. Dispatch 3rd-Party Webhook (SAP ERP Goods Receipt & WhatsApp Alert)
        dispatchToIntegrations(
            eventType = "ON_RECEIVE",
            filterPredicate = { it.notifyOnReceive },
            payload = """{
                "event": "GOODS_RECEIPT_CONFIRMED",
                "transferCode": "${transfer.transferCode}",
                "siteWarehouse": "${transfer.toWarehouseId}",
                "receiverName": "$receiverPicName",
                "partSku": "${transfer.partSku}",
                "quantityReceived": ${transfer.quantity},
                "batchNumber": "${transfer.batchNumber}",
                "receivedAt": ${System.currentTimeMillis()}
            }""".trimIndent()
        )

        Result.success(Unit)
    }

    /**
     * Records spare part usage for heavy equipment unit (Input No Lambung)
     */
    suspend fun recordPartUsage(
        equipmentUnitCode: String,
        partSku: String,
        warehouseId: String,
        warehouseName: String,
        quantity: Int,
        hourMeter: Double,
        mechanicName: String,
        workOrderNo: String,
        reason: String
    ): Result<PartUsageEntity> = withContext(Dispatchers.IO) {
        val stock = stockDao.getStockByWarehouseAndSku(warehouseId, partSku)
            ?: return@withContext Result.failure(Exception("Part $partSku tidak ada di stok $warehouseName"))

        if (stock.quantity < quantity) {
            return@withContext Result.failure(
                Exception("Stok part di $warehouseName tidak cukup (tersedia ${stock.quantity}, diminta $quantity)")
            )
        }

        // Deduct site stock
        stockDao.deductStockSafely(stock.id, quantity)

        val totalCost = stock.pricePerUnit * quantity
        val equipment = equipmentDao.getEquipmentByCode(equipmentUnitCode)
        val unitName = equipment?.unitName ?: "Unit $equipmentUnitCode"

        val usage = PartUsageEntity(
            equipmentUnitCode = equipmentUnitCode,
            equipmentUnitName = unitName,
            warehouseId = warehouseId,
            warehouseName = warehouseName,
            partId = stock.id,
            partSku = stock.sku,
            partName = stock.partName,
            quantity = quantity,
            unit = stock.unit,
            hourMeterAtUsage = hourMeter,
            mechanicName = mechanicName,
            workOrderNo = workOrderNo,
            reason = reason,
            totalCost = totalCost,
            createdAt = System.currentTimeMillis()
        )

        val usageId = usageDao.insertUsage(usage)

        // Update equipment hour meter & status back to OPERATIONAL if previously breakdown
        if (equipment != null) {
            equipmentDao.updateEquipment(
                equipment.copy(
                    currentHourMeter = hourMeter.coerceAtLeast(equipment.currentHourMeter),
                    status = EquipmentStatus.OPERATIONAL,
                    lastServiceDate = System.currentTimeMillis()
                )
            )
        }

        // Real-time notification for part usage
        triggerNotification(
            title = "🔧 Pemakaian Part: Unit $equipmentUnitCode",
            message = "Mekanik $mechanicName memasang $quantity ${stock.unit} ${stock.partName} pada $equipmentUnitCode ($unitName) - HM: $hourMeter. WO: $workOrderNo.",
            eventType = "PART_USAGE",
            severity = NotificationSeverity.INFO,
            source = warehouseName,
            payloadJson = """{
                "equipmentCode": "$equipmentUnitCode",
                "partSku": "${stock.sku}",
                "qty": $quantity,
                "hourMeter": $hourMeter,
                "costIdr": $totalCost,
                "wo": "$workOrderNo"
            }""".trimIndent()
        )

        // Dispatch 3rd-Party Webhook (Mining Fleet FMS & SAP ERP Goods Issue)
        dispatchToIntegrations(
            eventType = "ON_PART_USAGE",
            filterPredicate = { it.notifyOnUsage },
            payload = """{
                "event": "PART_INSTALLED_ON_EQUIPMENT",
                "equipmentCode": "$equipmentUnitCode",
                "equipmentName": "$unitName",
                "hourMeter": $hourMeter,
                "partSku": "${stock.sku}",
                "quantity": $quantity,
                "mechanic": "$mechanicName",
                "workOrder": "$workOrderNo",
                "site": "$warehouseName",
                "totalCostIdr": $totalCost,
                "timestamp": ${System.currentTimeMillis()}
            }""".trimIndent()
        )

        Result.success(usage.copy(id = usageId))
    }

    /**
     * Record Non-Inventory manual entry (Sembako dapur umum, motor operasional)
     */
    suspend fun recordNonInventory(item: NonInventoryEntity): Result<Long> = withContext(Dispatchers.IO) {
        val id = nonInventoryDao.insert(item)

        triggerNotification(
            title = "📋 Catatan Non-Inventory Baru",
            message = "${item.category.label}: ${item.itemName} (${item.quantity} ${item.unit}) dicatat oleh ${item.picName}.",
            eventType = "NON_INVENTORY",
            severity = NotificationSeverity.INFO,
            source = item.warehouseName,
            payloadJson = """{"item":"${item.itemName}","category":"${item.category.name}","cost":${item.amountRupiah}}"""
        )

        Result.success(id)
    }

    /**
     * Reports an emergency breakdown on heavy equipment unit
     */
    suspend fun reportEquipmentBreakdown(
        unitCode: String,
        issueDescription: String,
        urgentPartNeeded: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val unit = equipmentDao.getEquipmentByCode(unitCode)
            ?: return@withContext Result.failure(Exception("Unit $unitCode tidak ditemukan"))

        equipmentDao.updateEquipment(unit.copy(status = EquipmentStatus.BREAKDOWN_MAINTENANCE))

        triggerNotification(
            title = "🚨 ALAT BERAT BREAKDOWN: $unitCode",
            message = "${unit.unitName} mengalami kerusakan di ${unit.siteLocation}: $issueDescription. Part mendesak: $urgentPartNeeded.",
            eventType = "BREAKDOWN_UNIT",
            severity = NotificationSeverity.CRITICAL,
            source = unit.siteLocation,
            payloadJson = """{
                "unitCode": "$unitCode",
                "issue": "$issueDescription",
                "partNeeded": "$urgentPartNeeded",
                "status": "BREAKDOWN"
            }""".trimIndent()
        )

        // Dispatch instant Telegram Bot Alert to Owner & Site Managers
        dispatchToIntegrations(
            eventType = "ON_BREAKDOWN_ALERT",
            filterPredicate = { it.isEnabled },
            payload = """{
                "event": "EQUIPMENT_CRITICAL_BREAKDOWN",
                "unitCode": "$unitCode",
                "unitName": "${unit.unitName}",
                "location": "${unit.siteLocation}",
                "issue": "$issueDescription",
                "urgentPartNeeded": "$urgentPartNeeded",
                "alertLevel": "P1_URGENT",
                "timestamp": ${System.currentTimeMillis()}
            }""".trimIndent()
        )

        Result.success(Unit)
    }

    /**
     * Triggers a live test webhook ping to a 3rd party integration
     */
    suspend fun testIntegrationWebhook(integrationId: String): Result<WebhookLogEntity> = withContext(Dispatchers.IO) {
        val integration = integrationDao.getIntegrationById(integrationId)
            ?: return@withContext Result.failure(Exception("Integrasi tidak ditemukan"))

        val testPayload = """{
            "testPing": true,
            "system": "WMS Heavy Equipment Enterprise",
            "integrationId": "${integration.id}",
            "gatewayName": "${integration.name}",
            "timestamp": ${System.currentTimeMillis()},
            "authBearerVerified": true,
            "sampleEvent": "HEARTBEAT_STATUS_OK"
        }""".trimIndent()

        val log = webhookDispatcher.dispatchEvent(integration, "TEST_PING", testPayload)

        triggerNotification(
            title = "🌐 Webhook Ping: ${integration.name}",
            message = "Uji koneksi API berhasil (Status: ${log.httpStatus} ${log.statusText}, Latency: ${log.latencyMs}ms).",
            eventType = "API_WEBHOOK_DISPATCH",
            severity = if (log.httpStatus in 200..299) NotificationSeverity.SUCCESS else NotificationSeverity.WARNING,
            source = integration.name,
            payloadJson = testPayload
        )

        Result.success(log)
    }

    suspend fun toggleIntegration(id: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        val integration = integrationDao.getIntegrationById(id) ?: return@withContext
        integrationDao.updateIntegration(integration.copy(isEnabled = isEnabled))
    }

    suspend fun updateIntegrationSettings(
        id: String,
        endpointUrl: String,
        notifyOnTransfer: Boolean,
        notifyOnReceive: Boolean,
        notifyOnUsage: Boolean,
        notifyOnLowStock: Boolean
    ) = withContext(Dispatchers.IO) {
        val item = integrationDao.getIntegrationById(id) ?: return@withContext
        integrationDao.updateIntegration(
            item.copy(
                endpointUrl = endpointUrl,
                notifyOnTransfer = notifyOnTransfer,
                notifyOnReceive = notifyOnReceive,
                notifyOnUsage = notifyOnUsage,
                notifyOnLowStock = notifyOnLowStock
            )
        )
    }

    suspend fun markNotificationAsRead(id: Long) = withContext(Dispatchers.IO) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() = withContext(Dispatchers.IO) {
        notificationDao.markAllAsRead()
    }

    suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
        notificationDao.clearAll()
    }

    private suspend fun triggerNotification(
        title: String,
        message: String,
        eventType: String,
        severity: NotificationSeverity,
        source: String,
        payloadJson: String
    ) {
        notificationDao.insertNotification(
            NotificationEntity(
                title = title,
                message = message,
                eventType = eventType,
                severity = severity,
                source = source,
                isRead = false,
                payloadJson = payloadJson,
                createdAt = System.currentTimeMillis()
            )
        )

        // Trigger Android Vibration
        triggerHapticAlert()
    }

    private fun triggerHapticAlert() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
            }
        } catch (_: Exception) {
            // Ignore if vibrator permission or hardware is restricted
        }
    }

    private fun dispatchToIntegrations(
        eventType: String,
        filterPredicate: (ThirdPartyIntegrationEntity) -> Boolean,
        payload: String
    ) {
        externalScope.launch(Dispatchers.IO) {
            val list = integrationDao.getAllIntegrations().firstOrNull() ?: emptyList()
            for (integration in list) {
                if (integration.isEnabled && filterPredicate(integration)) {
                    webhookDispatcher.dispatchEvent(integration, eventType, payload)
                }
            }
        }
    }
}
