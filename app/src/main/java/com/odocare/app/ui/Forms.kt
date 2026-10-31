@file:OptIn(ExperimentalMaterial3Api::class)

package com.odocare.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.odocare.app.domain.IntervalUnit

val FieldBorder = Color(0xFF3A3B47)
private val FieldShape = RoundedCornerShape(12.dp)

/**
 * Kerangka dialog form seragam: header (ikon + judul), isi yang bisa di-scroll, dan tombol Batal / Simpan.
 * Menggantikan AlertDialog polos agar form terlihat rapi dan konsisten.
 */
@Composable
fun FormDialog(
    title: String, icon: ImageVector, onDismiss: () -> Unit, onConfirm: () -> Unit,
    confirmLabel: String = "Simpan", confirmEnabled: Boolean = true, subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val maxH = (LocalConfiguration.current.screenHeightDp * 0.86f).dp
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().heightIn(max = maxH).imePadding(),
            shape = RoundedCornerShape(22.dp), color = SurfaceDark, border = BorderStroke(1.dp, Color(0xFF2E2F3B))
        ) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(PillSelected), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = LavenderPrimary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = TextGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Close, "Tutup", tint = TextGray, modifier = Modifier.size(18.dp)) }
                }
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp), content = content
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(42.dp), shape = FieldShape, border = BorderStroke(1.dp, FieldBorder)) { Text("Batal", color = TextGray) }
                    Button(onClick = onConfirm, enabled = confirmEnabled, modifier = Modifier.weight(1f).height(42.dp), shape = FieldShape) { Text(confirmLabel) }
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = FieldBorder, focusedBorderColor = LavenderPrimary,
    unfocusedContainerColor = SurfaceVariantDark.copy(alpha = 0.45f), focusedContainerColor = SurfaceVariantDark.copy(alpha = 0.45f),
    unfocusedLabelColor = TextGray, focusedLabelColor = LavenderPrimary,
    unfocusedLeadingIconColor = TextGray, focusedLeadingIconColor = LavenderPrimary,
    unfocusedPrefixColor = TextGray, focusedPrefixColor = TextWhite
)

/** Field teks seragam: sudut membulat, font kecil, ikon di kiri. */
@Composable
fun AppTextField(
    value: String, onChange: (String) -> Unit, label: String, icon: ImageVector? = null,
    hint: String? = null, singleLine: Boolean = true, minLines: Int = 1, modifier: Modifier = Modifier.fillMaxWidth()
) {
    val iconSlot: (@Composable () -> Unit)? = if (icon != null) { { Icon(icon, null, modifier = Modifier.size(18.dp)) } } else null
    val hintSlot: (@Composable () -> Unit)? = if (hint != null) { { Text(hint, fontSize = 11.sp) } } else null
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = modifier,
        label = { Text(label, fontSize = 12.sp) }, singleLine = singleLine, minLines = minLines,
        leadingIcon = iconSlot, supportingText = hintSlot,
        textStyle = TextStyle(fontSize = 14.sp, color = TextWhite),
        shape = FieldShape, colors = fieldColors()
    )
}

/** Field angka bulat dengan pemisah ribuan. currency = true menambahkan awalan "Rp". */
@Composable
fun NumberField(value: String, onChange: (String) -> Unit, label: String, currency: Boolean = false, hint: String? = null, icon: ImageVector? = null, allowZero: Boolean = false, error: String? = null) {
    val prefixSlot: (@Composable () -> Unit)? = if (currency) { { Text("Rp ", fontSize = 14.sp) } } else null
    val iconSlot: (@Composable () -> Unit)? = if (icon != null) { { Icon(icon, null, modifier = Modifier.size(18.dp)) } } else null
    // Pesan error menggantikan hint dan mewarnai kotak merah
    val hintSlot: (@Composable () -> Unit)? = if (error != null) { { Text(error, fontSize = 11.sp) } } else if (hint != null) { { Text(hint, fontSize = 11.sp) } } else null
    OutlinedTextField(
        isError = error != null,
        value = value, onValueChange = { raw ->
            val digits = raw.filter(Char::isDigit).take(12)
            // allowZero: nol tunggal ("0") dipertahankan, selain itu nol di depan dibuang
            onChange(if (allowZero && digits.isNotEmpty() && digits.all { it == '0' }) "0" else digits.trimStart('0'))
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, fontSize = 12.sp) }, singleLine = true, prefix = prefixSlot,
        leadingIcon = iconSlot, supportingText = hintSlot,
        textStyle = TextStyle(fontSize = 14.sp, color = TextWhite),
        visualTransformation = ThousandsTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = FieldShape, colors = fieldColors()
    )
}

/** Kotak pilihan (tanggal / jam / dropdown): label kecil di atas, nilai di bawah, ikon di kiri. */
@Composable
fun PickerField(label: String, value: String?, icon: ImageVector, placeholder: String = "Pilih", trailing: ImageVector? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = FieldShape,
        color = SurfaceVariantDark.copy(alpha = 0.45f), border = BorderStroke(1.dp, FieldBorder)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = TextGray, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 11.sp, color = TextGray)
                Text(value ?: placeholder, fontSize = 14.sp, color = if (value != null) TextWhite else TextGray)
            }
            if (trailing != null) Icon(trailing, null, tint = TextGray, modifier = Modifier.size(18.dp))
        }
    }
}

/** Baris switch on/off dengan ikon, judul, dan keterangan. */
@Composable
fun SwitchRow(icon: ImageVector, title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(color = SurfaceVariantDark.copy(alpha = 0.45f), shape = FieldShape, border = BorderStroke(1.dp, FieldBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (checked) LavenderPrimary else TextGray, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, color = TextWhite)
                if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = TextGray, lineHeight = 14.sp)
            }
            Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.scale(0.8f))
        }
    }
}

/** Judul kelompok isian dalam form. */
@Composable
fun FormGroupLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LavenderPrimary, modifier = Modifier.padding(top = 4.dp))
}

/** Catatan kecil di bawah isian. */
@Composable
fun FormNote(text: String, color: Color = TextGray) {
    Text(text, fontSize = 11.sp, color = color, lineHeight = 15.sp)
}

/** Baris pilihan tunggal berbentuk segmen (mis. Motor / Mobil, Hari / Bulan / Tahun). */
@Composable
fun <T> SegmentedChoice(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, icon: ((T) -> ImageVector?)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { o ->
            val on = o == selected
            Surface(
                onClick = { onSelect(o) }, modifier = Modifier.weight(1f).height(38.dp), shape = RoundedCornerShape(10.dp),
                color = if (on) PillSelected else Color.Transparent,
                border = BorderStroke(1.dp, if (on) LavenderPrimary else FieldBorder)
            ) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    icon?.invoke(o)?.let { Icon(it, null, tint = if (on) LavenderPrimary else TextGray, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
                    Text(label(o), fontSize = 13.sp, color = if (on) TextWhite else TextGray, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

/** Pilihan satuan Hari / Bulan / Tahun */
@Composable
fun UnitChips(selected: IntervalUnit, onSelect: (IntervalUnit) -> Unit) {
    SegmentedChoice(IntervalUnit.values().toList(), selected, { it.label }, onSelect)
}

/** Dropdown berbentuk PickerField. items = pasangan (kunci, label). */
@Composable
fun DropdownField(label: String, valueLabel: String?, icon: ImageVector, items: List<Pair<String, String>>, placeholder: String = "Pilih", onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        PickerField(label, valueLabel, icon, placeholder, Icons.Default.ArrowDropDown) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(MenuBg)) {
            items.forEach { (k, l) -> DropdownMenuItem(text = { Text(l, fontSize = 14.sp, color = TextWhite) }, onClick = { open = false; onSelect(k) }) }
        }
    }
}
