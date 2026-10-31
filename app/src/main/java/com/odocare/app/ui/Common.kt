package com.odocare.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

@Composable
fun StatusBadge(text: String, colors: Pair<Color, Color>) {
    Surface(color = colors.first, shape = RoundedCornerShape(50)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = colors.second)
    }
}

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextWhite)
            Spacer(Modifier.padding(4.dp))
            content()
        }
    }
}

/** Kartu dengan garis status di sisi kiri. */
@Composable
fun StatusStripeCard(stripe: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark)) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(stripe))
            Column(Modifier.padding(16.dp).weight(1f), content = content)
        }
    }
}

/** Chip status dengan ikon: Aman / Segera / Lewat. */
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
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = colors.second, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = colors.second, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Titik tiga: tap dulu, baru muncul pilihan Edit dan Hapus. */
@Composable
fun ItemMenu(onEdit: () -> Unit, onDelete: () -> Unit, editLabel: String = "Edit", deleteLabel: String = "Hapus") {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Default.MoreVert, "Menu", tint = TextWhite) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(MenuBg)) {
            DropdownMenuItem(
                text = { Text(editLabel, color = TextWhite, fontSize = 17.sp) },
                leadingIcon = { Icon(Icons.Default.Edit, null, tint = TextWhite) },
                onClick = { open = false; onEdit() }
            )
            DropdownMenuItem(
                text = { Text(deleteLabel, color = DangerText, fontSize = 17.sp) },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = DangerText) },
                onClick = { open = false; onDelete() }
            )
        }
    }
}

/** Tombol tambah (+) persegi membulat, seragam di semua layar. */
@Composable
fun AddFab(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick, shape = RoundedCornerShape(20.dp), containerColor = LavenderPrimary, contentColor = Color(0xFF0D1B3E)) {
        Icon(Icons.Default.Add, "Tambah")
    }
}
