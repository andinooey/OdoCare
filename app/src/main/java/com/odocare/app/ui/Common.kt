@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.rotate
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
/** Kartu utama (odometer) paling bulat, petak bento sedang, kartu biasa paling kecil: variasi radius ala Expressive. */
val HeroShape = RoundedCornerShape(28.dp)
val TileShape = RoundedCornerShape(22.dp)

/**
 * Efek tekan berpegas: elemen mengecil sedikit saat ditekan lalu memantul kembali.
 * Pakai bersama Surface(onClick = ..., interactionSource = source).
 */
fun Modifier.pressScale(source: MutableInteractionSource, pressed: Float = 0.97f): Modifier = composed {
    val isPressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) pressed else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "press"
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

@Composable
fun StatusBadge(text: String, colors: Pair<Color, Color>) {
    Surface(color = colors.first, shape = RoundedCornerShape(50)) {
        Text(text, modifier = Modifier.padding(horizontal = Spacing.S, vertical = Spacing.XS), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.second)
    }
}

@Composable
fun SectionCard(title: String, icon: ImageVector? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = CardShape, color = MaterialTheme.colorScheme.surfaceVariant, border = CardBorder) {
        Column(Modifier.padding(Spacing.L)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) { Icon(icon, null, tint = LavenderPrimary, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(Spacing.S)) }
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
            }
            Spacer(Modifier.height(Spacing.M))
            content()
        }
    }
}

/**
 * Kartu yang bisa dilipat. Judul + ringkasan status selalu tampak; isi muncul saat diketuk.
 * Posisi buka/tutup tidak disimpan: tiap layar dibuka kembali ke [initiallyExpanded].
 */
@Composable
fun CollapsibleSectionCard(title: String, icon: ImageVector? = null, summary: String? = null, summaryColor: Color = TextGray, initiallyExpanded: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val arrow by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")
    Surface(modifier = Modifier.fillMaxWidth().animateContentSize(), shape = CardShape, color = MaterialTheme.colorScheme.surfaceVariant, border = CardBorder) {
        Column {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(Spacing.L), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) { Icon(icon, null, tint = LavenderPrimary, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(Spacing.S)) }
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                    if (summary != null) Text(summary, fontSize = 12.sp, color = summaryColor)
                }
                Icon(Icons.Default.ExpandMore, if (expanded) "Tutup" else "Buka", tint = TextGray, modifier = Modifier.size(22.dp).rotate(arrow))
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(start = Spacing.L, end = Spacing.L, bottom = Spacing.L), content = content)
            }
        }
    }
}

/** Kartu dengan garis status di sisi kiri. */
@Composable
fun StatusStripeCard(stripe: Color, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = SurfaceVariantDark, shape = CardShape, border = CardBorder) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(stripe))
            Column(Modifier.padding(start = Spacing.M, top = Spacing.M, end = Spacing.XS, bottom = Spacing.M).weight(1f), content = content)
        }
    }
}

@Composable
private fun StatusChipBase(colors: Pair<Color, Color>, icon: ImageVector, label: String) {
    Surface(color = colors.first, shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = Spacing.S, vertical = Spacing.XS), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = colors.second, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(Spacing.XS))
            Text(label, color = colors.second, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Chip status kecil dengan ikon: Aman / Segera / Lewat. */
@Composable
fun StatusChip(status: ServiceStatus) {
    val icon = when (status) {
        ServiceStatus.NORMAL -> Icons.Default.CheckCircle
        ServiceStatus.SOON, ServiceStatus.DUE -> Icons.Default.Warning
        ServiceStatus.LATE -> Icons.Default.Error
        ServiceStatus.NO_ODOMETER -> Icons.Default.Info
    }
    StatusChipBase(serviceColor(status), icon, if (status == ServiceStatus.NO_ODOMETER) "Update KM" else status.label)
}

/** Chip status dokumen dengan ikon yang sama seperti komponen servis. */
@Composable
fun DocStatusChip(status: DocStatus) {
    val icon = when (status) {
        DocStatus.SAFE -> Icons.Default.CheckCircle
        DocStatus.SOON -> Icons.Default.Warning
        DocStatus.DUE_TODAY, DocStatus.LATE -> Icons.Default.Error
    }
    StatusChipBase(docColor(status), icon, status.label)
}

/** Titik tiga: tap dulu, baru muncul pilihan Edit dan Hapus. */
@Composable
fun ItemMenu(onEdit: () -> Unit, onDelete: () -> Unit, editLabel: String = "Edit", deleteLabel: String = "Hapus") {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.MoreVert, "Menu", tint = TextGray, modifier = Modifier.size(18.dp)) }
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
    FloatingActionButton(onClick = onClick, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp), containerColor = LavenderPrimary, contentColor = OnAccent) {
        Icon(Icons.Default.Add, "Tambah", modifier = Modifier.size(24.dp))
    }
}

/** Judul layar seragam (ringkas). */
@Composable
fun ScreenTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = AppType.ScreenTitle, color = TextWhite)
            if (subtitle != null) Text(subtitle, style = AppType.Caption, color = TextGray)
        }
        actions()
    }
}

/** Judul bagian + aksi kecil di kanan (mis. "Lihat semua"). */
@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = AppType.Section, color = TextWhite, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = Spacing.S, vertical = 0.dp), modifier = Modifier.height(40.dp)) {
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
        Surface(color = SurfaceVariantDark, shape = CardShape, border = CardBorder, modifier = modifier.fillMaxWidth()) {
            Row(Modifier.padding(Spacing.M), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(PillSelected), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = LavenderPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(Spacing.M))
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                    Text(message, fontSize = 12.sp, color = TextGray)
                }
                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = Spacing.S)) { Text(actionLabel, fontSize = 12.sp, color = LavenderPrimary) }
                }
            }
        }
    } else {
        Column(modifier.fillMaxWidth().padding(horizontal = Spacing.XL, vertical = Spacing.XL), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(SurfaceVariantDark), contentAlignment = Alignment.Center) {
                Box(Modifier.size(68.dp).clip(CircleShape).background(PillSelected), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = LavenderPrimary, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(Spacing.L))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextWhite, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Spacing.XS))
            Text(message, fontSize = 12.sp, color = TextGray, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(Spacing.L))
                Button(onClick = onAction, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = Spacing.L, vertical = 0.dp), modifier = Modifier.height(40.dp)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(Spacing.S))
                    Text(actionLabel, fontSize = 14.sp)
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).let { if (enabled) it.clickable(onClick = onClick) else it }.padding(vertical = Spacing.S, horizontal = Spacing.XS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (enabled) LavenderPrimary else c, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.M))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, color = c)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = TextGray)
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextGray, modifier = Modifier.size(18.dp))
    }
}

/** Chip filter status (Semua / Lewat / Segera / Aman) dengan jumlah. */
@Composable
fun StatusFilterRow(labels: List<String>, counts: List<Int>, colors: List<Pair<Color, Color>>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
        labels.forEachIndexed { i, label ->
            val on = selected == i
            val (bg, fg) = colors[i]
            Surface(
                onClick = { onSelect(i) }, modifier = Modifier.weight(1f).height(34.dp), shape = RoundedCornerShape(10.dp),
                color = if (on) bg else Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (on) fg.copy(alpha = 0.6f) else FieldBorder)
            ) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(label, fontSize = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, color = if (on) fg else TextGray, maxLines = 1)
                    Spacer(Modifier.width(Spacing.XS))
                    Text("${counts[i]}", fontSize = 12.sp, color = if (on) fg else TextGray.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
