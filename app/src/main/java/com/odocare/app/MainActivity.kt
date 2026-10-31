package com.odocare.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.odocare.app.ui.SurfaceDark
import com.odocare.app.ui.screens.*

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            OdoCareTheme("dark") { OdoCareRoot(vm) }
        }
    }
}

private fun NavHostController.navigateTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

/** Halaman non-tab ditandai ke tab induknya agar tab tetap menyala. */
private fun parentTab(route: String?): String? = when (route) {
    "history", "odometer", "documents" -> "home"
    "master" -> "settings"
    else -> route
}

@Composable
private fun OdoCareRoot(vm: MainViewModel) {
    val nav = rememberNavController()
    val tabs = listOf(
        Tab("home", "Home", Icons.Default.Home),
        Tab("schedule", "Servis", Icons.Default.Build),
        Tab("dashboard", "Dashboard", Icons.Default.Dashboard),
        Tab("report", "Laporan", Icons.Default.BarChart),
        Tab("settings", "Kendaraan", Icons.Default.TwoWheeler)
    )
    val backStack by nav.currentBackStackEntryAsState()
    val currentTab = parentTab(backStack?.destination?.route)

    Scaffold(
        modifier = Modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(color = SurfaceDark, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 12.dp, bottom = 16.dp, start = 4.dp, end = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    tabs.forEach { tab ->
                        val selected = currentTab == tab.route
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (selected) LavenderPrimary.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { nav.navigateTab(tab.route) }.padding(vertical = 8.dp)
                        ) {
                            Icon(tab.icon, tab.label, tint = if (selected) LavenderPrimary else Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            Text(tab.label, fontSize = 10.sp, maxLines = 1, color = if (selected) LavenderPrimary else Color.Gray, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(vm,
                    onNavigateToSchedule = { nav.navigateTab("schedule") },
                    onNavigateToHistory = { nav.navigate("history") { launchSingleTop = true } },
                    onNavigateToDocuments = { nav.navigate("documents") { launchSingleTop = true } },
                    onNavigateToOdometer = { nav.navigate("odometer") { launchSingleTop = true } })
            }
            composable("schedule") { ScheduleScreen(vm) }
            composable("dashboard") {
                DashboardScreen(vm) { vehicleId, route ->
                    vm.setActiveVehicle(vehicleId)
                    if (route == "schedule") nav.navigateTab("schedule") else nav.navigate(route) { launchSingleTop = true }
                }
            }
            composable("report") { ReportScreen(vm) }
            composable("settings") { SettingsScreen(vm, onOpenMaster = { nav.navigate("master") { launchSingleTop = true } }) }
            composable("history") { HistoryScreen(vm, onBack = { nav.popBackStack() }, onOpenOdometer = { nav.navigate("odometer") { launchSingleTop = true } }) }
            composable("odometer") { OdometerScreen(vm, onBack = { nav.popBackStack() }) }
            composable("documents") { DocumentsScreen(vm, onBack = { nav.popBackStack() }) }
            composable("master") { MasterScreen(vm, onBack = { nav.popBackStack() }) }
        }
    }
}