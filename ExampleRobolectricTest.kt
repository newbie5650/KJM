package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.WarehouseDatabase
import com.example.data.model.NotificationSeverity
import com.example.data.model.StockItemEntity
import com.example.data.model.TransferStatus
import com.example.data.repository.WarehouseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("WMS Alat Berat", appName)
  }

  @Test
  fun `verify warehouse repository stock transfer with db locking and real-time notification`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testDispatcher = UnconfinedTestDispatcher(testScheduler)
    val testScope = TestScope(testDispatcher)

    val db = WarehouseDatabase.getDatabase(context, testScope)
    val repository = WarehouseRepository(db, context, testScope)

    // Verify initial seed stocks
    val initialStocks = repository.allStocks.first()
    assertTrue(initialStocks.isNotEmpty())

    val targetStock = initialStocks.first { it.warehouseId == "WH-MAIN" && it.quantity > 0 }
    val initialQty = targetStock.quantity

    // Perform stock transfer
    val result = repository.createStockTransfer(
      partId = targetStock.id,
      fromWarehouseId = "WH-MAIN",
      fromWarehouseName = "Gudang Utama Balikpapan",
      toWarehouseId = "SITE-MOR",
      toWarehouseName = "Site Tambang Morowali",
      quantity = 1,
      driverName = "Pak Joko",
      notes = "Uji Coba Otomasi"
    )

    assertTrue(result.isSuccess)
    val transfer = result.getOrNull()
    assertNotNull(transfer)
    assertEquals(TransferStatus.IN_TRANSIT, transfer?.status)

    // Verify stock is deducted
    val updatedStocks = repository.allStocks.first()
    val updatedItem = updatedStocks.first { it.id == targetStock.id }
    assertEquals(initialQty - 1, updatedItem.quantity)

    // Verify real-time notification generated
    val notifications = repository.allNotifications.first()
    assertTrue(notifications.any { it.eventType == "BARANG_MASUK" })
  }
}
