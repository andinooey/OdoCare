@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odocare.app.domain.DocStatus
import com.odocare.app.domain.ServiceStatus

// Aman = hijau, Segera = kuning, Lewat = merah
fun serviceColor(status: ServiceStatus): Pair<Color, Color> = when (status) {
    ServiceStatus.NORMAL -> StatusGreenBg to StatusGreenText
    ServiceStatus.SOON, ServiceStatus.DUE -> StatusYellowBg to StatusYellowText
    ServiceStatus.LATE -> StatusRedBg to StatusRedText
    ServiceStatus.NO_ODOMETER -> StatusGrayBg to StatusGrayText
}

fun docColor(status: DocStatus): Pair<Color, Color> = when (status) {
    DocStatus.SAFE -> StatusGreenBg to StatusGreenText
    DocStatus.SOON -> StatusYellowBg to StatusYellowText
    DocStatus.DUE_TODAY, DocStatus.LATE -> StatusRedBg to StatusRedText
}

val CardShape = RoundedCornerShape(14.dp)

@Composable
fun StatusBadge(text: String, colors: Pair<Color, Color>) {
    Surface(color = colors.first, shape = RoundedCornerShape(50)) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = colors.second)
    }
}

@Composable
fun SectionCard(title: String, icon: ImageVector? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = CardShape, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) { Icon(icon, null, tint = LavenderPrimary, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

/** Kartu dengan garis status di sisi kiri. */
@Composable
fun StatusStripeCard(stripe: Color, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = SurfaceVariantDark, shape = CardShape) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(stripe))
            Column(Modifier.padding(start = 12.dp, top = 10.dp, end = 4.dp, bottom = 10.dp).weight(1f), content = content)
        }
    }
}

/** Chip status kecil dengan ikon: Aman / Segera / Lewat. */
@Composable
fun StatusChip(status: ServiceStatus) {
    val colors = serviceColor(status)
    val icon = when (status) {
        ServiceStatus.NORMAL -> Icons.Default.CheckCircle
        ServiceStatus.SOON, ServiceStatus.DUE -> Icons.Default.Warning
        ServiceStatus.LATE -> Icons.Default.Error
        ServiceStatus.NO_ODOMETER -> Icons.Default.Info
    }
    val label = if (status == ServiceStatus.NO_ODOMETER) "Update KM" else status.label
    Surface(color = colors.first, shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = colors.second, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = colors.second, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Titik tiga: tap dulu, baru muncul pilihan Edit dan Hapus. */
@Composable
fun ItemMenu(onEdit: () -> Unit, onDelete: () -> Unit, editLabel: String = "Edit", deleteLabel: String = "Hapus") {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.MoreVert, "Menu", tint = TextGray, modifier = Modifier.size(18.dp)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(MenuBg)) {
            DropdownMenuItem(
                text = { Text(editLabel, color = TextWhite, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Edit, null, tint = TextWhite, modifier = Modifier.size(18.dp)) },
                onClick = { open = false; onEdit() }
            )
            DropdownMenuItem(
                text = { Text(deleteLabel, color = DangerText, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = DangerText, modifier = Modifier.size(18.dp)) },
                onClick = { open = false; onDelete() }
            )
        }
    }
}

/** Tombol tambah (+) persegi membulat, seragam di semua layar. */
@Composable
fun AddFab(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick, modifier = Modifier.size(48.dp), shape = RoundedCornerShape(14.dp), containerColor = LavenderPrimary, contentColor = Color(0xFF0D1B3E)) {
        Icon(Icons.Default.Add, "Tambah", modifier = Modifier.size(22.dp))
    }
}

/** Judul layar seragam (ringkas). */
@Composable
fun ScreenTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = TextGray)
        }
        actions()
    }
}

/** Judul bagian + aksi kecil di kanan (mis. "Lihat semua"). */
@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextWhite, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), modifier = Modifier.height(28.dp)) {
                Text(action, color = LavenderPrimary, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Tampilan kosong: ikon dalam lingkaran bertingkat + judul + keterangan (+ tombol opsional).
 * compact = true untuk bagian kecil di dalam halaman (mis. section di Home).
 */
@Composable
fun EmptyState(
    icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier,
    compact: Boolean = false, actionLabel: String? = null, onAction: (() -> Unit)? = null
) {
    if (compact) {
        Surface(color = SurfaceVariantDark, shape = CardShape, modifier = modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(PillSelected), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = LavenderPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                    Text(message, fontSize = 11.sp, color = TextGray)
                }
                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 8.dp)) { Text(actionLabel, fontSize = 12.sp, color = LavenderPrimary) }
                }
            }
        }
    } else {
        Column(modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(SurfaceVariantDark), contentAlignment = Alignment.Center) {
                Box(Modifier.size(68.dp).clip(CircleShape).background(PillSelected), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = LavenderPrimary, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextWhite, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(message, fontSize = 12.sp, color = TextGray, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAction, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp), modifier = Modifier.height(40.dp)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(actionLabel, fontSize = 13.sp)
                }
            }
        }
    }
}

/** Baris aksi minimalis: ikon + label + panah. */
@Composable
fun ActionRow(icon: ImageVector, label: String, enabled: Boolean = true, subtitle: String? = null, onClick: () -> Unit) {
    val c = if (enabled) TextWhite else TextGray.copy(alpha = 0.5f)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).let { if (enabled) it.clickable(onClick = onClick) else it }.padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (enabled) LavenderPrimary else c, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, color = c)
            if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = TextGray)
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextGray, modifier = Modifier.size(18.dp))
    }
}

/** Chip filter status (Semua / Lewat / Segera / Aman) dengan jumlah. */
@Composable
fun StatusFilterRow(labels: List<String>, counts: List<Int>, colors: List<Pair<Color, Color>>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { i, label ->
            val on = selected == i
            val (bg, fg) = colors[i]
            Surface(
                onClick = { onSelect(i) }, modifier = Modifier.weight(1f).height(34.dp), shape = RoundedCornerShape(10.dp),
                color = if (on) bg else Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (on) fg.copy(alpha = 0.6f) else Color(0xFF3A3B47))
            ) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(label, fontSize = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, color = if (on) fg else TextGray, maxLines = 1)
                    Spacer(Modifier.width(4.dp))
                    Text("${counts[i]}", fontSize = 11.sp, color = if (on) fg else TextGray.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
