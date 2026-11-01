@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odocare.app.MainViewModel
import com.odocare.app.data.AppNotification
import com.odocare.app.notif.Notif
import com.odocare.app.ui.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Riwayat semua notifikasi yang sudah muncul. Yang belum dibaca ditandai titik dan latar berbeda; ketuk untuk menandai dibaca. */
@Composable
fun NotificationScreen(vm: MainViewModel, onBack: () -> Unit, onOpenTarget: (AppNotification) -> Unit) {
    val items by vm.notifications.collectAsStateWithLifecycle()
    val unread by vm.unreadNotifications.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = NoInsets,
        topBar = {
            BackTopBar("Notifikasi", onBack) {
                IconButton(enabled = unread > 0, onClick = { vm.markAllNotificationsRead() }) {
                    Icon(Icons.Default.DoneAll, "Tandai semua dibaca", tint = if (unread > 0) LavenderPrimary else TextGray, modifier = Modifier.size(22.dp))
                }
                IconButton(enabled = items.isNotEmpty(), onClick = { vm.clearNotifications() }) {
                    Icon(Icons.Default.DeleteSweep, "Hapus semua", tint = if (items.isNotEmpty()) TextWhite else TextGray, modifier = Modifier.size(22.dp))
                }
            }
        }
    ) { pad ->
        if (items.isEmpty()) {
            EmptyState(Icons.Default.NotificationsNone, "Belum ada notifikasi", "Pengingat servis, pajak, dokumen, dan update KM yang muncul akan tersimpan di sini.", Modifier.padding(pad).padding(top = 24.dp))
        } else {
            LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)) {
                item {
                    Text(if (unread > 0) "$unread belum dibaca · ${items.size} notifikasi" else "Semua sudah dibaca · ${items.size} notifikasi", fontSize = 12.sp, color = TextGray, modifier = Modifier.padding(bottom = 4.dp))
                }
                items(items, key = { it.id }) { n -> NotificationRow(n, onOpen = { if (!n.isRead) vm.markNotificationRead(n.id); if (n.target != null) onOpenTarget(n) }, onDelete = { vm.deleteNotification(n) }) }
            }
        }
    }

}

@Composable
private fun NotificationRow(n: AppNotification, onOpen: () -> Unit, onDelete: () -> Unit) {
    val icon = when (n.channel) { Notif.CH_DOCUMENTS -> Icons.Default.Description; Notif.CH_ODOMETER -> Icons.Default.Speed; else -> Icons.Default.Build }
    // Belum dibaca: latar biru lembut, judul tebal putih, titik di kanan. Sudah dibaca: latar biasa, teks redup.
    Surface(onClick = onOpen, color = if (n.isRead) SurfaceVariantDark else StatusBlueBg, shape = CardShape) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(PillSelected), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (n.isRead) TextGray else LavenderPrimary, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(n.title, Modifier.weight(1f, fill = false), fontSize = 13.sp, fontWeight = if (n.isRead) FontWeight.Normal else FontWeight.Bold, color = if (n.isRead) TextGray else TextWhite)
                    if (!n.isRead) { Spacer(Modifier.width(8.dp)); Box(Modifier.size(8.dp).clip(CircleShape).background(LavenderPrimary)) }
                }
                Text(n.body, fontSize = 12.sp, color = if (n.isRead) TextGray else TextWhite, modifier = Modifier.padding(top = 4.dp))
                Text(notifTime(n.createdAt), fontSize = 12.sp, color = TextGray, modifier = Modifier.padding(top = 4.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Close, "Hapus notifikasi", tint = TextGray, modifier = Modifier.size(18.dp)) }
        }
    }
}

/** "Hari ini · 09:00", "Kemarin · 09:00", atau "05 Mar 2026 · 09:00". */
private fun notifTime(ms: Long): String {
    val zdt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    val day = zdt.toLocalDate(); val today = LocalDate.now()
    val label = when (day) { today -> "Hari ini"; today.minusDays(1) -> "Kemarin"; else -> fmtDate(ms) }
    return "$label · ${fmtTime(zdt.hour, zdt.minute)}"
}
