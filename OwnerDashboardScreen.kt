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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.EquipmentStatus
import com.example.data.model.EquipmentUnitEntity
import com.example.data.model.TransferStatus
import com.example.ui.components.StatCard
import com.example.ui.components.formatRupiah
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.IndustryDark
import com.example.ui.theme.MiningBlue
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.WarehouseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OwnerDashboardScreen(
    viewModel: WarehouseViewModel,
    onNavigateToIntegrations: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stocks by viewModel.stocks.collectAsStateWithLifecycle()
    val transfers by viewModel.transfers.collectAsStateWithLifecycle()
    val equipments by viewModel.equipmentList.collectAsStateWithLifecycle()
    val monthlyReports by viewModel.monthlyUnitCostReport.collectAsStateWithLifecycle()
    val usages by viewModel.usages.collectAsStateWithLifecycle()
    val nonInventory by viewModel.nonInventory.collectAsStateWithLifecycle()
    val selectedSite by viewModel.selectedSiteFilter.collectAsStateWithLifecycle()

    val totalStockValuation = stocks.sumOf { it.pricePerUnit * it.quantity }
    val operationalUnits = equipments.count { it.status == EquipmentStatus.OPERATIONAL }
    val breakdownUnits = equipments.count { it.status == EquipmentStatus.BREAKDOWN_MAINTENANCE }
    val inTransitTransfers = transfers.count { it.status == TransferStatus.IN_TRANSIT }
    val totalNonInventoryCost = nonInventory.sumOf { it.amountRupiah }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Executive Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("owner_hero_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = IndustryDark)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Ringkasan Eksekutif Pemilik (Owner)",
                        style = MaterialTheme.typography.titleMedium,
                        color = AmberPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Total Valuasi Suku Cadang & Aset Pergudangan",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = formatRupiah(totalStockValuation),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToIntegrations,
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("owner_goto_integrations_btn")
                        ) {
                            Text(
                                text = "Integrasi API (ERP/Bot)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IndustryDark
                            )
                        }
                        Button(
                            onClick = onNavigateToNotifications,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("owner_goto_notif_btn")
                        ) {
                            Text(
                                text = "Notifikasi Real-Time",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 4 KPI Stat Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Armada Operasional",
                        value = "$operationalUnits / ${equipments.size} Unit",
                        subtitle = if (breakdownUnits > 0) "$breakdownUnits unit breakdown" else "Semua normal",
                        icon = Icons.Default.PrecisionManufacturing,
                        accentColor = if (breakdownUnits > 0) AlertRed else SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Mutasi Dalam Jalan",
                        value = "$inTransitTransfers Pengiriman",
                        subtitle = "Menuju site tambang",
                        icon = Icons.Default.LocalShipping,
                        accentColor = MiningBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Biaya Non-Inventory",
                        value = formatRupiah(totalNonInventoryCost),
                        subtitle = "Sembako mess & bensin",
                        icon = Icons.Default.Restaurant,
                        accentColor = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Item Suku Cadang",
                        value = "${stocks.size} SKU Part",
                        subtitle = "Gudang utama & cabang",
                        icon = Icons.Default.Build,
                        accentColor = AmberPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Urgent Breakdown Alert if any unit is down
        val breakdowns = equipments.filter { it.status == EquipmentStatus.BREAKDOWN_MAINTENANCE }
        if (breakdowns.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("breakdown_alert_section"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ReportProblem,
                                contentDescription = null,
                                tint = AlertRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Perhatian: Alat Berat Sedang Breakdown!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        breakdowns.forEach { eq ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${eq.unitCode} - ${eq.unitName}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Lokasi: ${eq.siteLocation} | HM: ${eq.currentHourMeter}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = AlertRed.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "BREAKDOWN",
                                            color = AlertRed,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Laporan Biaya Bulanan (Monthly Report as in blueprint)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Laporan Biaya Per Site & Alat Berat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Rekapitulasi pemakaian suku cadang per No Lambung unit",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Filter by Site Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedSite == null,
                            onClick = { viewModel.setSiteFilter(null) },
                            label = { Text("Semua Site") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberPrimary,
                                selectedLabelColor = IndustryDark
                            ),
                            modifier = Modifier.testTag("filter_all_sites")
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedSite == "Morowali",
                            onClick = { viewModel.setSiteFilter("Morowali") },
                            label = { Text("Site Morowali") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberPrimary,
                                selectedLabelColor = IndustryDark
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedSite == "Sangatta",
                            onClick = { viewModel.setSiteFilter("Sangatta") },
                            label = { Text("Site Sangatta") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberPrimary,
                                selectedLabelColor = IndustryDark
                            )
                        )
                    }
                }
            }
        }

        // List of Monthly Unit Summaries
        items(monthlyReports) { report ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("unit_report_${report.equipmentUnitCode}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.padding(end = 10.dp)
                            ) {
                                Text(
                                    text = report.equipmentUnitCode,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = report.equipmentUnitName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "${report.siteLocation} • HM: ${report.latestHourMeter}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Biaya Part Terpasang",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = formatRupiah(report.totalCostRupiah),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = AmberPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Volume Komponen",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "${report.totalPartsInstalled} Suku Cadang",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }

                    // Show breakdown of usages for this unit
                    val unitUsages = usages.filter { it.equipmentUnitCode == report.equipmentUnitCode }
                    if (unitUsages.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Riwayat Penggantian Terakhir:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                        unitUsages.take(2).forEach { u ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• ${u.quantity}x ${u.partName} (${u.workOrderNo})",
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155)
                                )
                                Text(
                                    text = formatRupiah(u.totalCost),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
