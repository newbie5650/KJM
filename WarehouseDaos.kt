package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.EquipmentUnitEntity
import com.example.data.model.NonInventoryEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.PartUsageEntity
import com.example.data.model.StockItemEntity
import com.example.data.model.StockTransferEntity
import com.example.data.model.ThirdPartyIntegrationEntity
import com.example.data.model.UserEntity
import com.example.data.model.WebhookLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE userId = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)
}

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks ORDER BY warehouseId, partName ASC")
    fun getAllStocks(): Flow<List<StockItemEntity>>

    @Query("SELECT * FROM stocks WHERE warehouseId = :warehouseId ORDER BY partName ASC")
    fun getStocksByWarehouse(warehouseId: String): Flow<List<StockItemEntity>>

    @Query("SELECT * FROM stocks WHERE id = :id")
    suspend fun getStockById(id: Long): StockItemEntity?

    @Query("SELECT * FROM stocks WHERE warehouseId = :warehouseId AND sku = :sku LIMIT 1")
    suspend fun getStockByWarehouseAndSku(warehouseId: String, sku: String): StockItemEntity?

    @Query("SELECT * FROM stocks WHERE quantity <= minStockLevel")
    fun getLowStockAlerts(): Flow<List<StockItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: StockItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<StockItemEntity>)

    @Update
    suspend fun updateStock(stock: StockItemEntity)

    @Query("UPDATE stocks SET quantity = quantity - :amount, lastUpdated = :now WHERE id = :id AND quantity >= :amount")
    suspend fun deductStockSafely(id: Long, amount: Int, now: Long = System.currentTimeMillis()): Int

    @Query("UPDATE stocks SET quantity = quantity + :amount, lastUpdated = :now WHERE warehouseId = :warehouseId AND sku = :sku")
    suspend fun addStockBySku(warehouseId: String, sku: String, amount: Int, now: Long = System.currentTimeMillis()): Int
}

@Dao
interface TransferDao {
    @Query("SELECT * FROM stock_transfers ORDER BY timestamp DESC")
    fun getAllTransfers(): Flow<List<StockTransferEntity>>

    @Query("SELECT * FROM stock_transfers WHERE toWarehouseId = :warehouseId AND status = 'IN_TRANSIT' ORDER BY timestamp DESC")
    fun getIncomingTransfersForSite(warehouseId: String): Flow<List<StockTransferEntity>>

    @Query("SELECT * FROM stock_transfers WHERE id = :id")
    suspend fun getTransferById(id: Long): StockTransferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: StockTransferEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfers(transfers: List<StockTransferEntity>)

    @Update
    suspend fun updateTransfer(transfer: StockTransferEntity)

    @Query("UPDATE stock_transfers SET status = 'RECEIVED', receivedAt = :now WHERE id = :id")
    suspend fun markReceived(id: Long, now: Long = System.currentTimeMillis())
}

@Dao
interface EquipmentDao {
    @Query("SELECT * FROM equipment_units ORDER BY unitCode ASC")
    fun getAllEquipment(): Flow<List<EquipmentUnitEntity>>

    @Query("SELECT * FROM equipment_units WHERE unitCode = :code")
    suspend fun getEquipmentByCode(code: String): EquipmentUnitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipment(equipment: EquipmentUnitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEquipmentList(list: List<EquipmentUnitEntity>)

    @Update
    suspend fun updateEquipment(equipment: EquipmentUnitEntity)
}

@Dao
interface UsageDao {
    @Query("SELECT * FROM part_usages ORDER BY createdAt DESC")
    fun getAllUsages(): Flow<List<PartUsageEntity>>

    @Query("SELECT * FROM part_usages WHERE equipmentUnitCode = :unitCode ORDER BY createdAt DESC")
    fun getUsagesByUnit(unitCode: String): Flow<List<PartUsageEntity>>

    @Query("SELECT * FROM part_usages WHERE warehouseId = :warehouseId ORDER BY createdAt DESC")
    fun getUsagesByWarehouse(warehouseId: String): Flow<List<PartUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: PartUsageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsages(usages: List<PartUsageEntity>)
}

@Dao
interface NonInventoryDao {
    @Query("SELECT * FROM non_inventory_items ORDER BY recordedAt DESC")
    fun getAllNonInventory(): Flow<List<NonInventoryEntity>>

    @Query("SELECT * FROM non_inventory_items WHERE warehouseId = :warehouseId ORDER BY recordedAt DESC")
    fun getByWarehouse(warehouseId: String): Flow<List<NonInventoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: NonInventoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<NonInventoryEntity>)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM realtime_notifications ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM realtime_notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE realtime_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE realtime_notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM realtime_notifications")
    suspend fun clearAll()
}

@Dao
interface IntegrationDao {
    @Query("SELECT * FROM third_party_integrations")
    fun getAllIntegrations(): Flow<List<ThirdPartyIntegrationEntity>>

    @Query("SELECT * FROM third_party_integrations WHERE id = :id")
    suspend fun getIntegrationById(id: String): ThirdPartyIntegrationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntegration(item: ThirdPartyIntegrationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntegrations(list: List<ThirdPartyIntegrationEntity>)

    @Update
    suspend fun updateIntegration(item: ThirdPartyIntegrationEntity)

    @Query("SELECT * FROM webhook_dispatch_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<WebhookLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WebhookLogEntity): Long
}
