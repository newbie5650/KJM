package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NonInventoryCategory
import com.example.data.model.PartCategory
import com.example.data.model.StockItemEntity
import com.example.ui.components.ConcurrencyLockBanner
import com.example.ui.components.formatRupiah
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.IndustryDark
import com.example.ui.theme.MiningBlue
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.WarehouseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseTransferScreen(
    viewModel: WarehouseViewModel,
    modifier: Modifier = Modifier
) {
    val stocks by viewModel.stocks.collectAsStateWithLifecycle()
    val nonInventoryList by viewModel.nonInventory.collectAsStateWithLifecycle()
    val concurrencyResult by viewModel.concurrencyTestState.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Mutasi Antar-Gudang", "Katalog Stok", "Sembako & Non-Inv")

    // Transfer Form State
    val principalStocks = stocks.filter { it.warehouseId == "WH-MAIN" }
    var selectedPart by remember { mutableStateOf<StockItemEntity?>(null) }
    var partDropdownExpanded by remember { mutableStateOf(false) }

    val destinationSites = listOf(
        Pair("SITE-MOR", "Site Tambang Morowali"),
        Pair("SITE-SNG", "Site Tambang Sangatta")
    )
    var selectedDestination by remember { mutableStateOf(destinationSites.first()) }
    var destDropdownExpanded by remember { mutableStateOf(false) }

    var transferQtyText by remember { mutableStateOf("1") }
    var driverName by remember { mutableStateOf("Pak Joko (Ekspedisi Lintas Selat)") }
    var transferNotes by remember { mutableStateOf("Permintaan suku cadang darurat untuk alat berat") }

    // Non-Inventory Form State
    var nonInvName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(NonInventoryCategory.SEMBAKO_DAPUR) }
    var nonInvQtyText by remember { mutableStateOf("") }
    var nonInvUnitText by remember { mutableStateOf("KG") }
    var nonInvCostText by remember { mutableStateOf("") }
    var nonInvPic by remember { mutableStateOf("") }
    var nonInvNotes by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Rule 1 Concurrency Lock Banner
        item {
            ConcurrencyLockBanner(
                onRunTest = { viewModel.runConcurrencyLockTest() }
            )
        }

        // Animated Concurrency Test Result Card
        if (concurrencyResult != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("concurrency_result_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (concurrencyResult?.isRunning == true) Color(0xFFEFF6FF) else Color(0xFFF0FDF4)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (concurrencyResult?.isRunning == true) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MiningBlue
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hasil Uji Aturan Emas 1 (Race Condition Prevention):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = concurrencyResult?.message ?: "",
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 18.sp
                        )
                        if (concurrencyResult?.isRunning == false) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Stok Awal -> Akhir", fontSize = 10.sp, color = Color(0xFF64748B))
                                        Text(
                                            "${concurrencyResult?.stockBefore} -> ${concurrencyResult?.stockAfter}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Transaksi Diterima", fontSize = 10.sp, color = SuccessGreen)
                                        Text(
                                            "${concurrencyResult?.successfulTransfers} Berhasil",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = SuccessGreen
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Transaksi Ditolak", fontSize = 10.sp, color = AlertRed)
                                        Text(
                                            "${concurrencyResult?.rejectedTransfers} Aman",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = AlertRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tab Navigation
        item {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = AmberPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = AmberPrimary
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) AmberPrimary else Color(0xFF64748B)
                            )
                        },
                        modifier = Modifier.testTag("tab_$index")
                    )
                }
            }
        }

        // Tab 0: Form Pengiriman Barang (Mutasi Antar-Gudang)
        if (selectedTabIndex == 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transfer_form_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Form Mutasi Stok Gudang Utama",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Dilengkapi Database Locking & Webhook Otomatis",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Destination Site Selector
                        ExposedDropdownMenuBox(
                            expanded = destDropdownExpanded,
                            onExpandedChange = { destDropdownExpanded = !destDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedDestination.second,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tujuan Site Cabang") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = destDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("dest_warehouse_input"),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = destDropdownExpanded,
                                onDismissRequest = { destDropdownExpanded = false }
                            ) {
                                destinationSites.forEach { site ->
                                    DropdownMenuItem(
                                        text = { Text(site.second) },
                                        onClick = {
                                            selectedDestination = site
                                            destDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Part Selector
                        ExposedDropdownMenuBox(
                            expanded = partDropdownExpanded,
                            onExpandedChange = { partDropdownExpanded = !partDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedPart?.let { "${it.sku} - ${it.partName} (Sisa: ${it.quantity} ${it.unit})" }
                                    ?: "Pilih Suku Cadang Alat Berat",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Pilih Suku Cadang") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = partDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("part_select_input"),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = partDropdownExpanded,
                                onDismissRequest = { partDropdownExpanded = false }
                            ) {
                                principalStocks.forEach { item ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    "${item.sku} • ${item.partName}",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    "Tersedia: ${item.quantity} ${item.unit} | ${formatRupiah(item.pricePerUnit)}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedPart = item
                                            partDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quantity Input
                        OutlinedTextField(
                            value = transferQtyText,
                            onValueChange = { transferQtyText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Jumlah Dikirim") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transfer_qty_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Driver / Expedition
                        OutlinedTextField(
                            value = driverName,
                            onValueChange = { driverName = it },
                            label = { Text("Nama Driver / Ekspedisi Pengantar") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("driver_name_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Notes
                        OutlinedTextField(
                            value = transferNotes,
                            onValueChange = { transferNotes = it },
                            label = { Text("Catatan / Keterangan Kebutuhan") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transfer_notes_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val part = selectedPart ?: principalStocks.firstOrNull()
                                if (part != null) {
                                    val qty = transferQtyText.toIntOrNull() ?: 1
                                    viewModel.submitTransfer(
                                        partId = part.id,
                                        destinationWarehouseId = selectedDestination.first,
                                        destinationWarehouseName = selectedDestination.second,
                                        quantity = qty,
                                        driverName = driverName,
                                        notes = transferNotes
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_transfer_button")
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = IndustryDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kirim Mutasi Stok (Locked FOR UPDATE)",
                                fontWeight = FontWeight.Bold,
                                color = IndustryDark
                            )
                        }
                    }
                }
            }
        }

        // Tab 1: Katalog Stok
        if (selectedTabIndex == 1) {
            items(principalStocks) { stock ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stock_item_${stock.sku}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = stock.sku,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF334155),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = stock.category.label,
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stock.partName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Kompatibel: ${stock.compatibleUnits}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "${formatRupiah(stock.pricePerUnit)} / ${stock.unit}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AmberPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${stock.quantity} ${stock.unit}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (stock.quantity <= stock.minStockLevel) AlertRed else SuccessGreen
                            )
                            if (stock.quantity <= stock.minStockLevel) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AlertRed.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "STOK KRITIS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AlertRed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tab 2: Sembako & Non-Inventory (as in blueprint)
        if (selectedTabIndex == 2) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("non_inventory_form_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Catatan Non-Inventory (Sembako & Operasional)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Sembako dapur umum mess tambang, bensin motor, ATK",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Category
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NonInventoryCategory.values().forEach { cat ->
                                val isSelected = selectedCategory == cat
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) AmberPrimary else Color(0xFFF1F5F9),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedCategory = cat }
                                ) {
                                    Text(
                                        text = when (cat) {
                                            NonInventoryCategory.SEMBAKO_DAPUR -> "Sembako Dapur"
                                            NonInventoryCategory.OPERATIONAL_FUEL -> "BBM Motor"
                                            NonInventoryCategory.GENERAL_AFFAIR -> "Umum / GA"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) IndustryDark else Color(0xFF475569),
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = nonInvName,
                            onValueChange = { nonInvName = it },
                            label = { Text("Nama Barang (misal: Beras 50kg, Pertalite)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("non_inv_name_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = nonInvQtyText,
                                onValueChange = { nonInvQtyText = it },
                                label = { Text("Volume/Qty") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("non_inv_qty_input"),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = nonInvUnitText,
                                onValueChange = { nonInvUnitText = it },
                                label = { Text("Satuan (KG/LTR/SAK)") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("non_inv_unit_input"),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = nonInvCostText,
                            onValueChange = { nonInvCostText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Nominal Biaya (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("non_inv_cost_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = nonInvPic,
                            onValueChange = { nonInvPic = it },
                            label = { Text("Nama PIC Penanggung Jawab") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("non_inv_pic_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (nonInvName.isNotBlank()) {
                                    val qty = nonInvQtyText.toDoubleOrNull() ?: 1.0
                                    val cost = nonInvCostText.toLongOrNull() ?: 0L
                                    viewModel.submitNonInventory(
                                        itemName = nonInvName,
                                        category = selectedCategory,
                                        quantity = qty,
                                        unit = nonInvUnitText.ifBlank { "UNIT" },
                                        amountRupiah = cost,
                                        picName = nonInvPic.ifBlank { "Staf Gudang" },
                                        notes = nonInvNotes
                                    )
                                    nonInvName = ""
                                    nonInvCostText = ""
                                    nonInvQtyText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_non_inv_button")
                        ) {
                            Text(
                                text = "Simpan Catatan Non-Inventory",
                                fontWeight = FontWeight.Bold,
                                color = IndustryDark
                            )
                        }
                    }
                }
            }

            items(nonInventoryList) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = item.category.label,
                                    fontSize = 10.sp,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = item.itemName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Qty: ${item.quantity} ${item.unit} | PIC: ${item.picName}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Text(
                            text = formatRupiah(item.amountRupiah),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = AmberPrimary
                        )
                    }
                }
            }
        }
    }
}
