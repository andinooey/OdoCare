package com.odocare.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import com.odocare.app.ui.isDarkFor
import com.odocare.app.ui.Spacing
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.odocare.app.ui.LavenderPrimary
import com.odocare.app.ui.OdoCareTheme
import com.odocare.app.ui.PillSelected
import com.odocare.app.ui.SurfaceDark
import com.odocare.app.ui.TextGray
import com.odocare.app.ui.TextWhite
import com.odocare.app.ui.screens.*

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_TARGET = "open_target"
        const val EXTRA_VEHICLE = "open_vehicle"
    }

    private val vm: MainViewModel by viewModels()
    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** Tujuan dari ketukan notifikasi: (target, vehicleId). Dikonsumsi OdoCareRoot lalu dikosongkan. */
    private var launchTarget by mutableStateOf<Pair<String, String?>?>(null)

    private fun readTarget(i: Intent?) {
        val t = i?.getStringExtra(EXTRA_TARGET) ?: return
        launchTarget = t to i.getStringExtra(EXTRA_VEHICLE)
        i.removeExtra(EXTRA_TARGET); i.removeExtra(EXTRA_VEHICLE)
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); readTarget(intent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        if (savedInstanceState == null) readTarget(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            val dark = isDarkFor(vm.themeMode)
            DisposableEffect(dark) {
                val transparent = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                )
                onDispose {}
            }
            OdoCareTheme(vm.themeMode) {
                OdoCareRoot(vm, launchTarget) { launchTarget = null }
            }
        }
    }
}

/** Buka layar tujuan notifikasi, dan pindah ke kendaraan yang bersangkutan. */
private fun openTarget(nav: NavHostController, vm: MainViewModel, target: String, vehicleId: String?) {
    vehicleId?.let { vm.setActiveVehicle(it) }
    when (target) {
        "service" -> { vm.serviceSection = 0; nav.navigateTab("schedule") }
        "documents" -> { vm.serviceSection = 1; nav.navigateTab("schedule") }
        "odometer" -> nav.navigate("odometer") { launchSingleTop = true }
    }
}

/** Pindah tab selalu ke layar default tab itu (sub-layar seperti Dokumen/Riwayat tidak diingat). */
private fun NavHostController.navigateTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = false }
    launchSingleTop = true
    restoreState = false
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabRoutes = setOf("home", "schedule", "history", "settings")

@Composable
private fun OdoCareRoot(vm: MainViewModel, launchTarget: Pair<String, String?>?, onTargetConsumed: () -> Unit) {
    val nav = rememberNavController()
    val snackbarState = remember { SnackbarHostState() }
    // Snackbar "Urungkan": dijalankan satu per satu. Tanpa diurungkan, onExpire (mis. hapus file foto) dijalankan.
    LaunchedEffect(Unit) {
        vm.undoEvents.collect { e ->
            val result = snackbarState.showSnackbar(e.message, actionLabel = "Urungkan", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) e.onUndo() else e.onExpire()
        }
    }
    LaunchedEffect(launchTarget) { launchTarget?.let { (t, v) -> openTarget(nav, vm, t, v); onTargetConsumed() } }
    val tabs = listOf(
        Tab("home", "Home", Icons.Default.Home),
        Tab("schedule", "Servis", Icons.Default.Build),
        Tab("history", "Histori", Icons.Default.History),
        Tab("settings", "Kendaraan", Icons.Default.TwoWheeler)
    )
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    // Halaman non-tab (Laporan, Odometer, Master) tetap menyalakan tab asalnya
    var lastTab by remember { mutableStateOf("home") }
    LaunchedEffect(route) { if (route in tabRoutes) lastTab = route!! }
    val currentTab = if (route in tabRoutes) route else lastTab

    Scaffold(
        modifier = Modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarState) },
        bottomBar = {
            Surface(color = SurfaceDark, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = Spacing.S, bottom = Spacing.S, start = Spacing.XS, end = Spacing.XS), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    tabs.forEach { tab ->
                        val selected = currentTab == tab.route
                        // Pil dan ikon tab berpegas: warna berpindah halus, ikon memantul sedikit saat terpilih
                        val pillBg by animateColorAsState(if (selected) PillSelected else Color.Transparent, spring(stiffness = Spring.StiffnessMedium), label = "pill")
                        val iconScale by animateFloatAsState(if (selected) 1.12f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "iconScale")
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable { if (tab.route == "schedule") vm.serviceSection = 0; nav.navigateTab(tab.route) }.padding(vertical = Spacing.XS)
                        ) {
                            Box(Modifier.clip(RoundedCornerShape(50)).background(pillBg).padding(horizontal = Spacing.XL, vertical = Spacing.XS)) {
                                Icon(tab.icon, tab.label, tint = if (selected) TextWhite else TextGray, modifier = Modifier.size(20.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale })
                            }
                            Spacer(Modifier.height(Spacing.XS))
                            Text(tab.label, fontSize = 12.sp, maxLines = 1, color = if (selected) TextWhite else TextGray, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav, startDestination = "home", modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(150)) },
            exitTransition = { fadeOut(tween(90)) },
            popEnterTransition = { fadeIn(tween(150)) },
            popExitTransition = { fadeOut(tween(90)) }
        ) {
            composable("home") {
                HomeScreen(vm,
                    onNavigateToSchedule = { vm.serviceSection = 0; nav.navigateTab("schedule") },
                    onNavigateToHistory = { nav.navigateTab("history") },
                    onNavigateToDocuments = { vm.serviceSection = 1; nav.navigateTab("schedule") },
                    onNavigateToOdometer = { nav.navigate("odometer") { launchSingleTop = true } },
                    onNavigateToNotifications = { nav.navigate("notifications") { launchSingleTop = true } },
                    onNavigateToSettings = { nav.navigateTab("settings") },
                    onNavigateToReport = { nav.navigate("report") { launchSingleTop = true } })
            }
            composable("schedule") {
                // Tab Servis: dropdown di judul memilih Jadwal & Komponen (default) atau Pajak & Dokumen
                if (vm.serviceSection == 0) ScheduleScreen(vm, onOpenMaster = { nav.navigate("master") { launchSingleTop = true } })
                else DocumentsScreen(vm, onOpenDocTypes = { nav.navigate("doctypes") { launchSingleTop = true } })
            }
            composable("report") { ReportScreen(vm, onBack = { nav.popBackStack() }) }
            composable("settings") { SettingsScreen(vm, onOpenMaster = { nav.navigate("master") { launchSingleTop = true } }, onOpenVehicleTypes = { nav.navigate("vehicletypes") { launchSingleTop = true } }, onOpenDocTypes = { nav.navigate("doctypes") { launchSingleTop = true } }) }
            composable("history") { HistoryScreen(vm, onOpenReport = { nav.navigate("report") { launchSingleTop = true } }, onOpenOdometer = { nav.navigate("odometer") { launchSingleTop = true } }) }
            composable("notifications") { NotificationScreen(vm, onBack = { nav.popBackStack() }, onOpenTarget = { n -> openTarget(nav, vm, n.target ?: "", n.vehicleId) }) }
            composable("odometer") { OdometerScreen(vm, onBack = { nav.popBackStack() }) }
            composable("master") { MasterScreen(vm, onBack = { nav.popBackStack() }) }
            composable("doctypes") { DocTypeScreen(vm, onBack = { nav.popBackStack() }) }
            composable("vehicletypes") { VehicleTypeScreen(vm, onBack = { nav.popBackStack() }) }
        }
    }
}
