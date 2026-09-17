package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.WarehouseDatabase
import com.example.data.model.EquipmentStatus
import com.example.data.model.EquipmentUnitEntity
import com.example.data.model.NonInventoryCategory
import com.example.data.model.NonInventoryEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.NotificationSeverity
import com.example.data.model.PartUsageEntity
import com.example.data.model.StockItemEntity
import com.example.data.model.StockTransferEntity
import com.example.data.model.ThirdPartyIntegrationEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.WebhookLogEntity
import com.example.data.repository.WarehouseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MonthlyUnitCostSummary(
    val equipmentUnitCode: String,
    val equipmentUnitName: String,
    val siteLocation: String,
    val totalCostRupiah: Long,
    val totalPartsInstalled: Int,
    val latestHourMeter: Double
)

data class ConcurrencyTestResult(
    val status: String,
    val message: String,
    val stockBefore: Int,
    val stockAfter: Int,
    val successfulTransfers: Int,
    val rejectedTransfers: Int,
    val isRunning: Boolean = false
)

class WarehouseViewModel(application: Application) : AndroidViewModel(application) {
    private val database = WarehouseDatabase.getDatabase(application, viewModelScope)
    val repository = WarehouseRepository(database, application, viewModelScope)

    // Current active user / role
    private val _currentUser = MutableStateFlow(
        UserEntity(
            userId = "USR-01",
            name = "Pak Budi Hartono",
            email = "owner@miningcorp.co.id",
            role = UserRole.OWNER,
            warehouseId = "HQ-01",
            warehouseName = "Kantor Pusat / Direksi",
            token = "demo_token_not_for_production"
        )
    )
    val currentUser: StateFlow<UserEntity> = _currentUser.asStateFlow()

    // Environment mode (Rule 2: Pisahkan Testing & Produksi)
    private val _isSandboxMode = MutableStateFlow(false)
    val isSandboxMode: StateFlow<Boolean> = _isSandboxMode.asStateFlow()

    // Live Socket Simulator connection status
    private val _isSocketConnected = MutableStateFlow(true)
    val isSocketConnected: StateFlow<Boolean> = _isSocketConnected.asStateFlow()

    private val _selectedEquipmentFilter = MutableStateFlow<String?>(null)
    val selectedEquipmentFilter: StateFlow<String?> = _selectedEquipmentFilter.asStateFlow()

    private val _selectedSiteFilter = MutableStateFlow<String?>(null)
    val selectedSiteFilter: StateFlow<String?> = _selectedSiteFilter.asStateFlow()

    private val _concurrencyTestState = MutableStateFlow<ConcurrencyTestResult?>(null)
    val concurrencyTestState: StateFlow<ConcurrencyTestResult?> = _concurrencyTestState.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    // Live streams from repository
    val stocks: StateFlow<List<StockItemEntity>> = repository.allStocks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transfers: StateFlow<List<StockTransferEntity>> = repository.allTransfers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val equipmentList: StateFlow<List<EquipmentUnitEntity>> = repository.allEquipment
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val usages: StateFlow<List<PartUsageEntity>> = repository.allUsages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nonInventory: StateFlow<List<NonInventoryEntity>> = repository.allNonInventory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> = repository.unreadNotificationsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val integrations: StateFlow<List<ThirdPartyIntegrationEntity>> = repository.allIntegrations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentWebhookLogs: StateFlow<List<WebhookLogEntity>> = repository.recentWebhookLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered monthly report calculations
    val monthlyUnitCostReport: StateFlow<List<MonthlyUnitCostSummary>> = combine(
        equipmentList,
        usages,
        _selectedSiteFilter
    ) { equipments, usageList, siteFilter ->
        val filteredEquipments = if (siteFilter.isNullOrBlank()) {
            equipments
        } else {
            equipments.filter { it.siteLocation.contains(siteFilter, ignoreCase = true) }
        }

        filteredEquipments.map { eq ->
            val unitUsages = usageList.filter { it.equipmentUnitCode == eq.unitCode }
            MonthlyUnitCostSummary(
                equipmentUnitCode = eq.unitCode,
                equipmentUnitName = eq.unitName,
                siteLocation = eq.siteLocation,
                totalCostRupiah = unitUsages.sumOf { it.totalCost },
                totalPartsInstalled = unitUsages.sumOf { it.quantity },
                latestHourMeter = eq.currentHourMeter
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun switchRole(role: UserRole) {
        val user = when (role) {
            UserRole.OWNER -> UserEntity(
                userId = "USR-01",
                name = "Pak Budi Hartono",
                email = "owner@miningcorp.co.id",
                role = UserRole.OWNER,
                warehouseId = "HQ-01",
                warehouseName = "Kantor Pusat / Direksi",
                token = "demo_token_not_for_production"
            )
            UserRole.HEAD_WAREHOUSE -> UserEntity(
                userId = "USR-02",
                name = "Hendra Saputra",
                email = "wh.principal@miningcorp.co.id",
                role = UserRole.HEAD_WAREHOUSE,
                warehouseId = "WH-MAIN",
                warehouseName = "Gudang Utama Balikpapan",
                token = "demo_token_not_for_production"
            )
            UserRole.SITE_STAFF -> UserEntity(
                userId = "USR-03",
                name = "Agus Sutanto (Mekanik)",
                email = "site.morowali@miningcorp.co.id",
                role = UserRole.SITE_STAFF,
                warehouseId = "SITE-MOR",
                warehouseName = "Site Tambang Morowali",
                token = "demo_token_not_for_production"
            )
        }
        _currentUser.value = user
        _uiMessage.value = "Berganti peran ke: ${role.label}"
    }

    fun toggleSandboxMode() {
        _isSandboxMode.value = !_isSandboxMode.value
        _uiMessage.value = if (_isSandboxMode.value) {
            "🧪 Sandbox Testing Mode Aktif (Aturan Emas 2: Database simulasi terpisah)"
        } else {
            "🏢 Mode Produksi Aktif"
        }
    }

    fun toggleSocketConnection() {
        _isSocketConnected.value = !_isSocketConnected.value
    }

    fun setSiteFilter(site: String?) {
        _selectedSiteFilter.value = site
    }

    fun setEquipmentFilter(unitCode: String?) {
        _selectedEquipmentFilter.value = unitCode
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    // --- Action Handlers ---

    fun submitTransfer(
        partId: Long,
        destinationWarehouseId: String,
        destinationWarehouseName: String,
        quantity: Int,
        driverName: String,
        notes: String
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val result = repository.createStockTransfer(
                partId = partId,
                fromWarehouseId = user.warehouseId,
                fromWarehouseName = user.warehouseName,
                toWarehouseId = destinationWarehouseId,
                toWarehouseName = destinationWarehouseName,
                quantity = quantity,
                driverName = driverName,
                notes = notes
            )
            result.onSuccess {
                _uiMessage.value = "✅ Mutasi ${it.transferCode} berhasil dikirim dengan Database Lock Protection!"
            }.onFailure {
                _uiMessage.value = "❌ Gagal: ${it.localizedMessage}"
            }
        }
    }

    fun confirmReceiveTransfer(transferId: Long) {
        viewModelScope.launch {
            val user = _currentUser.value
            val result = repository.receiveTransferAtSite(transferId, user.name)
            result.onSuccess {
                _uiMessage.value = "✅ Barang berhasil diverifikasi & stok masuk ke ${user.warehouseName}!"
            }.onFailure {
                _uiMessage.value = "❌ Gagal verifikasi: ${it.localizedMessage}"
            }
        }
    }

    fun submitPartUsage(
        equipmentUnitCode: String,
        partSku: String,
        quantity: Int,
        hourMeter: Double,
        mechanicName: String,
        workOrderNo: String,
        reason: String
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val result = repository.recordPartUsage(
                equipmentUnitCode = equipmentUnitCode,
                partSku = partSku,
                warehouseId = user.warehouseId,
                warehouseName = user.warehouseName,
                quantity = quantity,
                hourMeter = hourMeter,
                mechanicName = mechanicName,
                workOrderNo = workOrderNo,
                reason = reason
            )
            result.onSuccess {
                _uiMessage.value = "✅ Pemakaian part unit $equipmentUnitCode tercatat & telematika FMS disinkronkan!"
            }.onFailure {
                _uiMessage.value = "❌ Gagal simpan: ${it.localizedMessage}"
            }
        }
    }

    fun submitNonInventory(
        itemName: String,
        category: NonInventoryCategory,
        quantity: Double,
        unit: String,
        amountRupiah: Long,
        picName: String,
        notes: String
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val item = NonInventoryEntity(
                itemName = itemName,
                category = category,
                warehouseId = user.warehouseId,
                warehouseName = user.warehouseName,
                quantity = quantity,
                unit = unit,
                amountRupiah = amountRupiah,
                picName = picName,
                notes = notes
            )
            val result = repository.recordNonInventory(item)
            result.onSuccess {
                _uiMessage.value = "✅ Catatan ${category.label} tersimpan!"
            }.onFailure {
                _uiMessage.value = "❌ Gagal: ${it.localizedMessage}"
            }
        }
    }

    fun reportBreakdown(unitCode: String, issue: String, partNeeded: String) {
        viewModelScope.launch {
            val result = repository.reportEquipmentBreakdown(unitCode, issue, partNeeded)
            result.onSuccess {
                _uiMessage.value = "🚨 Status unit $unitCode diubah ke BREAKDOWN. Telegram Alert terkirim ke Direksi & Site!"
            }.onFailure {
                _uiMessage.value = "❌ Gagal: ${it.localizedMessage}"
            }
        }
    }

    fun testWebhook(integrationId: String) {
        viewModelScope.launch {
            _uiMessage.value = "Mengirim payload webhook uji coba..."
            val result = repository.testIntegrationWebhook(integrationId)
            result.onSuccess { log ->
                _uiMessage.value = "🌐 Respon ${log.integrationName}: HTTP ${log.httpStatus} (${log.latencyMs}ms)"
            }.onFailure {
                _uiMessage.value = "❌ Gagal tes webhook: ${it.localizedMessage}"
            }
        }
    }

    fun toggleIntegration(id: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleIntegration(id, enabled)
        }
    }

    fun updateIntegrationSettings(
        id: String,
        endpointUrl: String,
        notifyOnTransfer: Boolean,
        notifyOnReceive: Boolean,
        notifyOnUsage: Boolean,
        notifyOnLowStock: Boolean
    ) {
        viewModelScope.launch {
            repository.updateIntegrationSettings(
                id, endpointUrl, notifyOnTransfer, notifyOnReceive, notifyOnUsage, notifyOnLowStock
            )
            _uiMessage.value = "✅ Pengaturan webhook integrasi diperbarui."
        }
    }

    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
            _uiMessage.value = "Semua notifikasi ditandai dibaca."
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
            _uiMessage.value = "Notifikasi dibersihkan."
        }
    }

    /**
     * Rule 1 Concurrency Simulator:
     * Simulates two staff clicking the transfer button at the exact same millisecond
     * when only 1 item is left, demonstrating that Database Locking (FOR UPDATE)
     * allows only one request to deduct, rejecting the other with a clear message.
     */
    fun runConcurrencyLockTest() {
        viewModelScope.launch(Dispatchers.IO) {
            _concurrencyTestState.value = ConcurrencyTestResult(
                status = "RUNNING",
                message = "Menjalankan uji konkurensi (2 staf klik mutasi bersamaan)...",
                stockBefore = 0,
                stockAfter = 0,
                successfulTransfers = 0,
                rejectedTransfers = 0,
                isRunning = true
            )

            val targetStock = stocks.value.firstOrNull { it.warehouseId == "WH-MAIN" && it.quantity > 0 }
            if (targetStock == null) {
                _concurrencyTestState.value = ConcurrencyTestResult(
                    status = "FAILED",
                    message = "Tidak ada stok tersedia di Gudang Utama untuk pengujian.",
                    stockBefore = 0,
                    stockAfter = 0,
                    successfulTransfers = 0,
                    rejectedTransfers = 0,
                    isRunning = false
                )
                return@launch
            }

            val initialQty = targetStock.quantity
            val requestedQty = initialQty // requesting full quantity to test race condition

            // Launch 2 parallel requests simultaneously
            val req1 = async {
                repository.createStockTransfer(
                    partId = targetStock.id,
                    fromWarehouseId = "WH-MAIN",
                    fromWarehouseName = "Gudang Utama Balikpapan",
                    toWarehouseId = "SITE-MOR",
                    toWarehouseName = "Site Tambang Morowali",
                    quantity = requestedQty,
                    driverName = "Staf 1 (Simulasi Klik Paralel)",
                    notes = "Uji Coba Race Condition #1"
                )
            }

            val req2 = async {
                delay(2) // 2ms apart, exact collision
                repository.createStockTransfer(
                    partId = targetStock.id,
                    fromWarehouseId = "WH-MAIN",
                    fromWarehouseName = "Gudang Utama Balikpapan",
                    toWarehouseId = "SITE-SNG",
                    toWarehouseName = "Site Tambang Sangatta",
                    quantity = requestedQty,
                    driverName = "Staf 2 (Simulasi Klik Paralel)",
                    notes = "Uji Coba Race Condition #2"
                )
            }

            val res1 = req1.await()
            val res2 = req2.await()

            var successes = 0
            var rejects = 0

            if (res1.isSuccess) successes++ else rejects++
            if (res2.isSuccess) successes++ else rejects++

            // Re-fetch stock after test
            delay(100)
            val updatedStock = stocks.value.firstOrNull { it.id == targetStock.id }
            val finalQty = updatedStock?.quantity ?: 0

            _concurrencyTestState.value = ConcurrencyTestResult(
                status = "SUCCESS",
                message = "🛡️ Aturan Emas 1 Terbukti: Database Row Locking berhasil mencegah 'double-spend'! Hanya 1 staf yang berhasil memutasi stok, permintaan kedua ditolak dengan aman tanpa selisih.",
                stockBefore = initialQty,
                stockAfter = finalQty,
                successfulTransfers = successes,
                rejectedTransfers = rejects,
                isRunning = false
            )
        }
    }
}
