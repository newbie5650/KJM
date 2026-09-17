package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.outlined.Api
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserRole
import com.example.ui.components.WmsTopAppBar
import com.example.ui.screens.OwnerDashboardScreen
import com.example.ui.screens.RealtimeNotificationScreen
import com.example.ui.screens.SiteReceiveAndUsageScreen
import com.example.ui.screens.ThirdPartyIntegrationScreen
import com.example.ui.screens.WarehouseTransferScreen
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.IndustryDark
import com.example.ui.theme.MiningBlue
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.WarehouseViewModel

enum class WmsScreen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    OWNER_DASHBOARD(
        title = "Owner",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        tag = "nav_owner"
    ),
    WAREHOUSE_TRANSFER(
        title = "Gudang Utama",
        selectedIcon = Icons.Filled.LocalShipping,
        unselectedIcon = Icons.Outlined.LocalShipping,
        tag = "nav_warehouse"
    ),
    SITE_OPERATIONS(
        title = "Site Cabang",
        selectedIcon = Icons.Filled.PrecisionManufacturing,
        unselectedIcon = Icons.Outlined.PrecisionManufacturing,
        tag = "nav_site"
    ),
    NOTIFICATIONS(
        title = "Notifikasi",
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications,
        tag = "nav_notifications"
    ),
    INTEGRATIONS(
        title = "Integrasi API",
        selectedIcon = Icons.Filled.Api,
        unselectedIcon = Icons.Outlined.Api,
        tag = "nav_integrations"
    )
}

class MainActivity : ComponentActivity() {
    private val viewModel: WarehouseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                WmsApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun WmsApp(
    viewModel: WarehouseViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(WmsScreen.OWNER_DASHBOARD) }
    var showRoleDialog by remember { mutableStateOf(false) }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()
    val isSocketConnected by viewModel.isSocketConnected.collectAsStateWithLifecycle()
    val isSandboxMode by viewModel.isSandboxMode.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearUiMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            WmsTopAppBar(
                currentUser = currentUser,
                unreadCount = unreadCount,
                isSocketConnected = isSocketConnected,
                isSandboxMode = isSandboxMode,
                onRoleClick = { showRoleDialog = true },
                onNotificationClick = { currentScreen = WmsScreen.NOTIFICATIONS },
                onSandboxToggle = { viewModel.toggleSandboxMode() }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = IndustryDark,
                contentColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_navigation_bar")
            ) {
                WmsScreen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        modifier = Modifier.testTag(screen.tag),
                        icon = {
                            if (screen == WmsScreen.NOTIFICATIONS && unreadCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = AmberPrimary, contentColor = IndustryDark) {
                                            Text(text = if (unreadCount > 9) "9+" else unreadCount.toString())
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            }
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IndustryDark,
                            selectedTextColor = AmberPrimary,
                            indicatorColor = AmberPrimary,
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    WmsScreen.OWNER_DASHBOARD -> OwnerDashboardScreen(
                        viewModel = viewModel,
                        onNavigateToIntegrations = { currentScreen = WmsScreen.INTEGRATIONS },
                        onNavigateToNotifications = { currentScreen = WmsScreen.NOTIFICATIONS }
                    )
                    WmsScreen.WAREHOUSE_TRANSFER -> WarehouseTransferScreen(
                        viewModel = viewModel
                    )
                    WmsScreen.SITE_OPERATIONS -> SiteReceiveAndUsageScreen(
                        viewModel = viewModel
                    )
                    WmsScreen.NOTIFICATIONS -> RealtimeNotificationScreen(
                        viewModel = viewModel
                    )
                    WmsScreen.INTEGRATIONS -> ThirdPartyIntegrationScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Role Switcher Modal Dialog
    if (showRoleDialog) {
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            title = {
                Text(
                    text = "Ganti Hak Akses / Peran Akun",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pilih peran pengguna untuk menguji alur kerja sesuai arsitektur sistem:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    UserRole.values().forEach { role ->
                        val isCurrent = currentUser.role == role
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.switchRole(role)
                                    // Auto switch screen to matching role screen for convenient testing
                                    currentScreen = when (role) {
                                        UserRole.OWNER -> WmsScreen.OWNER_DASHBOARD
                                        UserRole.HEAD_WAREHOUSE -> WmsScreen.WAREHOUSE_TRANSFER
                                        UserRole.SITE_STAFF -> WmsScreen.SITE_OPERATIONS
                                    }
                                    showRoleDialog = false
                                }
                                .testTag("role_option_${role.name}"),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) AmberPrimary.copy(alpha = 0.15f) else Color(0xFFF8FAFC)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (role) {
                                        UserRole.OWNER -> Icons.Default.Business
                                        UserRole.HEAD_WAREHOUSE -> Icons.Default.Lock
                                        UserRole.SITE_STAFF -> Icons.Default.Construction
                                    },
                                    contentDescription = null,
                                    tint = Color(role.badgeColor),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = role.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = when (role) {
                                            UserRole.OWNER -> "Akses Laporan Biaya Bulanan & KPI Valuasi Aset"
                                            UserRole.HEAD_WAREHOUSE -> "Katalog Suku Cadang, Mutasi FOR UPDATE, Sembako"
                                            UserRole.SITE_STAFF -> "Terima Barang Masuk & Form Pakai Part (No Lambung)"
                                        },
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleDialog = false }) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
