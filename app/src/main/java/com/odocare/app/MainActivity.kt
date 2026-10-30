package com.odocare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // <--- INI YANG KETINGGALAN
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        setContent {
            val theme by vm.themeMode.collectAsStateWithLifecycle()
            OdoCareTheme(theme) { OdoCareRoot(vm) }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun OdoCareRoot(vm: MainViewModel) {
    val nav = rememberNavController()
    val tabs = listOf(
        Tab("home", "Home", Icons.Default.Home),
        Tab("schedule", "Maintenance", Icons.Default.Build),
        Tab("odometer", "History", Icons.Default.History),
        Tab("settings", "Vehicle", Icons.Default.TwoWheeler)
    )
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(), 
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = SurfaceDark, 
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), 
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() 
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 12.dp, start = 8.dp, end = 8.dp), 
                    horizontalArrangement = Arrangement.SpaceEvenly, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .background(if (selected) LavenderPrimary.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(50))
                                .clickable { 
                                    nav.navigate(tab.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Icon(tab.icon, tab.label, tint = if (selected) LavenderPrimary else Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            Text(tab.label, fontSize = 10.sp, color = if (selected) LavenderPrimary else Color.Gray, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(vm, onNavigateToSchedule = { 
                nav.navigate("schedule") { 
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true 
                } 
            }) }
            composable("odometer") { OdometerScreen(vm) }
            composable("schedule") { ScheduleScreen(vm) }
            composable("documents") { DocumentsScreen(vm) }
            composable("settings") { SettingsScreen(vm) }
        }
    }
}
