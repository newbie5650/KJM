package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.model.EquipmentStatus
import com.example.data.model.EquipmentUnitEntity
import com.example.data.model.StockItemEntity
import com.example.data.model.TransferStatus
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.IndustryDark
import com.example.ui.theme.MiningBlue
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.WarehouseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteReceiveAndUsageScreen(
    viewModel: WarehouseViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val transfers by viewModel.transfers.collectAsStateWithLifecycle()
    val equipments by viewModel.equipmentList.collectAsStateWithLifecycle()
    val stocks by viewModel.stocks.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Terima Barang Masuk", "Pakai Part Alat Berat", "Status Armada")

    // Filter incoming transfers for this site
    val incomingTransfers = transfers.filter {
        it.toWarehouseId == currentUser.warehouseId && it.status == TransferStatus.IN_TRANSIT
    }

    // Site local stock for mechanic usage
    val siteStocks = stocks.filter { it.warehouseId == currentUser.warehouseId && it.quantity > 0 }

    // Usage Form State
    var selectedUnit by remember { mutableStateOf<EquipmentUnitEntity?>(null) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    var selectedPart by remember { mutableStateOf<StockItemEntity?>(null) }
    var partDropdownExpanded by remember { mutableStateOf(false) }

    var usageQtyText by remember { mutableStateOf("1") }
    var hourMeterText by remember { mutableStateOf("14250.0") }
    var mechanicName by remember { mutableStateOf(currentUser.name) }
    var workOrderNo by remember { mutableStateOf("WO-SITE-2026-140") }
    var reasonText by remember { mutableStateOf("Pergantian suku cadang berkala / perawatan") }

    // Breakdown Report State
    var breakdownUnitCode by remember { mutableStateOf("DT-104") }
    var breakdownIssue by remember { mutableStateOf("Kebocoran pipa hidrolik saat operasi di disposal") }
    var breakdownPartNeeded by remember { mutableStateOf("Hydraulic High Pressure Hose 1\"") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Site Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("site_header_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = IndustryDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = currentUser.warehouseName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Petugas: ${currentUser.name}",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "SITE TAMBANG",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Tabs
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
                                fontSize = 11.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) AmberPrimary else Color(0xFF64748B)
                            )
                        },
                        modifier = Modifier.testTag("site_tab_$index")
                    )
                }
            }
        }

        // Tab 0: Terima Barang Masuk (Receive items with 1-click verify)
        if (selectedTabIndex == 0) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Barang Sedang Dikirim ke Site (${incomingTransfers.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            if (incomingTransfers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Semua Barang Masuk Sudah Diterima",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Tidak ada kiriman berstatus dalam perjalanan untuk site ini.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            } else {
                items(incomingTransfers) { trf ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("incoming_transfer_${trf.transferCode}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = MiningBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = trf.transferCode,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFE0F2FE)
                                ) {
                                    Text(
                                        text = "IN TRANSIT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MiningBlue,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${trf.quantity}x ${trf.partName} (${trf.partSku})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Asal: ${trf.fromWarehouseName} • Batch: ${trf.batchNumber}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "Pengantar: ${trf.driverName}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            if (trf.notes.isNotBlank()) {
                                Text(
                                    text = "Catatan: ${trf.notes}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { viewModel.confirmReceiveTransfer(trf.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_receive_${trf.transferCode}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Verifikasi & Terima Barang Masuk",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tab 1: Form Pakai Part Mekanik (Input No Lambung Alat Berat)
        if (selectedTabIndex == 1) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("part_usage_form_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Form Pemakaian Part Mekanik",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Wajib input No Lambung alat berat & meteran HM",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Heavy Equipment Selector (No Lambung)
                        ExposedDropdownMenuBox(
                            expanded = unitDropdownExpanded,
                            onExpandedChange = { unitDropdownExpanded = !unitDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedUnit?.let { "${it.unitCode} - ${it.unitName} (HM: ${it.currentHourMeter})" }
                                    ?: "Pilih No Lambung Alat Berat",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("No Lambung Alat Berat (Kode Unit)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("equipment_unit_input"),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = unitDropdownExpanded,
                                onDismissRequest = { unitDropdownExpanded = false }
                            ) {
                                equipments.forEach { unit ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    "${unit.unitCode} • ${unit.unitName}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    "Lokasi: ${unit.siteLocation} | HM: ${unit.currentHourMeter} | Status: ${unit.status.label}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedUnit = unit
                                            hourMeterText = unit.currentHourMeter.toString()
                                            unitDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Hour Meter (HM)
                        OutlinedTextField(
                            value = hourMeterText,
                            onValueChange = { hourMeterText = it },
                            label = { Text("Hour Meter (HM) Saat Pemasangan") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hour_meter_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Part Selector from site stock
                        ExposedDropdownMenuBox(
                            expanded = partDropdownExpanded,
                            onExpandedChange = { partDropdownExpanded = !partDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedPart?.let { "${it.sku} - ${it.partName} (Stok Site: ${it.quantity})" }
                                    ?: (if (siteStocks.isNotEmpty()) "Pilih Suku Cadang dari Gudang Site" else "Stok di Site Kosong (Lakukan Mutasi)"),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Suku Cadang yang Dipakai") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = partDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("usage_part_input"),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = partDropdownExpanded,
                                onDismissRequest = { partDropdownExpanded = false }
                            ) {
                                siteStocks.forEach { item ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("${item.sku} • ${item.partName} (Sisa: ${item.quantity} ${item.unit})")
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = usageQtyText,
                                onValueChange = { usageQtyText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Jumlah (Qty)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("usage_qty_input"),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = workOrderNo,
                                onValueChange = { workOrderNo = it },
                                label = { Text("No Work Order (WO)") },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("usage_wo_input"),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = mechanicName,
                            onValueChange = { mechanicName = it },
                            label = { Text("Nama Mekanik Pemasang") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mechanic_name_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = reasonText,
                            onValueChange = { reasonText = it },
                            label = { Text("Keterangan Masalah / Kerusakan") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("usage_reason_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val unit = selectedUnit ?: equipments.firstOrNull()
                                val part = selectedPart ?: siteStocks.firstOrNull()
                                if (unit != null && part != null) {
                                    val qty = usageQtyText.toIntOrNull() ?: 1
                                    val hm = hourMeterText.toDoubleOrNull() ?: unit.currentHourMeter
                                    viewModel.submitPartUsage(
                                        equipmentUnitCode = unit.unitCode,
                                        partSku = part.sku,
                                        quantity = qty,
                                        hourMeter = hm,
                                        mechanicName = mechanicName,
                                        workOrderNo = workOrderNo,
                                        reason = reasonText
                                    )
                                }
                            },
                            enabled = siteStocks.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_usage_button")
                        ) {
                            Text(
                                text = "Catat Pemakaian Part Alat Berat",
                                fontWeight = FontWeight.Bold,
                                color = IndustryDark
                            )
                        }
                    }
                }
            }
        }

        // Tab 2: Status Armada & Lapor Breakdown Cepat
        if (selectedTabIndex == 2) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_breakdown_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AlertRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lapor Kerusakan / Breakdown Darurat",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF991B1B)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Memicu Notifikasi Real-time & Webhook ke Telegram Direksi secara instan.",
                            fontSize = 11.sp,
                            color = Color(0xFF7F1D1D)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = breakdownUnitCode,
                            onValueChange = { breakdownUnitCode = it },
                            label = { Text("No Lambung (misal: DT-104)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = breakdownIssue,
                            onValueChange = { breakdownIssue = it },
                            label = { Text("Deskripsi Kerusakan") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = breakdownPartNeeded,
                            onValueChange = { breakdownPartNeeded = it },
                            label = { Text("Part Mendesak yang Dibutuhkan") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.reportBreakdown(
                                    breakdownUnitCode,
                                    breakdownIssue,
                                    breakdownPartNeeded
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_broadcast_breakdown")
                        ) {
                            Text(
                                text = "Kirim Alert Breakdown (Broadcast Telegram & WMS)",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            items(equipments) { eq ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PrecisionManufacturing,
                                contentDescription = null,
                                tint = if (eq.status == EquipmentStatus.BREAKDOWN_MAINTENANCE) AlertRed else SuccessGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${eq.unitCode} • ${eq.unitName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "${eq.siteLocation} | HM: ${eq.currentHourMeter}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Operator: ${eq.operatorName}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (eq.status) {
                                EquipmentStatus.OPERATIONAL -> SuccessGreen.copy(alpha = 0.15f)
                                EquipmentStatus.BREAKDOWN_MAINTENANCE -> AlertRed.copy(alpha = 0.15f)
                                EquipmentStatus.STANDBY -> AmberPrimary.copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = eq.status.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (eq.status) {
                                    EquipmentStatus.OPERATIONAL -> SuccessGreen
                                    EquipmentStatus.BREAKDOWN_MAINTENANCE -> AlertRed
                                    EquipmentStatus.STANDBY -> AmberPrimary
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
