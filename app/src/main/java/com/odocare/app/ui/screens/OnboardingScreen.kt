package com.odocare.app.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odocare.app.ui.*

private data class OnboardingStep(val icon: ImageVector, val title: String, val body: String)

private val onboardingSteps = listOf(
    OnboardingStep(Icons.Default.Speed, "Catat KM kendaraanmu", "Perbarui odometer sesekali. OdoCare menghitung kapan servis berikutnya."),
    OnboardingStep(Icons.Default.Build, "Pantau servis dan komponen", "Lihat mana yang aman, segera, atau lewat jadwal. Catat servis dan simpan riwayatnya."),
    OnboardingStep(Icons.Default.NotificationsActive, "Jangan lewatkan jatuh tempo", "Pajak, STNK, SIM, dan asuransi diingatkan sebelum jatuh tempo. Data tersimpan di HP-mu.")
)

/** Pengantar 3 langkah, hanya tampil sekali untuk pengguna baru. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    val last = step == onboardingSteps.lastIndex
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
            if (!last) TextButton(onClick = onDone) { Text("Lewati", color = TextGray) }
        }
        Crossfade(step, Modifier.weight(1f).fillMaxWidth(), label = "onboarding") { i ->
            val s = onboardingSteps[i]
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(160.dp).clip(CircleShape).background(SurfaceVariantDark), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(100.dp).clip(CircleShape).background(PillSelected), contentAlignment = Alignment.Center) {
                        Icon(s.icon, null, tint = LavenderPrimary, modifier = Modifier.size(48.dp))
                    }
                }
                Spacer(Modifier.height(32.dp))
                Text(s.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextWhite, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(s.body, fontSize = 14.sp, color = TextGray, textAlign = TextAlign.Center, lineHeight = 20.sp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.Center) {
            onboardingSteps.indices.forEach { i ->
                Box(Modifier.padding(horizontal = 4.dp).size(if (i == step) 10.dp else 8.dp).clip(CircleShape).background(if (i == step) LavenderPrimary else SurfaceVariantDark))
            }
        }
        Button(onClick = { if (last) onDone() else step++ }, modifier = Modifier.fillMaxWidth().height(48.dp).padding(bottom = 0.dp)) {
            Text(if (last) "Mulai" else "Lanjut", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
    }
}
