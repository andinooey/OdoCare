package com.odocare.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odocare.app.domain.DocStatus
import com.odocare.app.domain.ServiceStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OdoCareCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Card(modifier, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Odo.colors.surface),
        border = BorderStroke(1.dp, Odo.colors.border)) { content() }

@Composable
fun PrimaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) =
    Button(onClick, modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Odo.colors.accent, contentColor = Odo.colors.onAccent), content = content)

@Composable
fun SecondaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) =
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Odo.colors.border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Odo.colors.textPrimary), content = content)

enum class Tone { SUCCESS, WARNING, ERROR, INFO, NEUTRAL }

fun statusTone(s: ServiceStatus) = when (s) {
    ServiceStatus.NORMAL, ServiceStatus.SELESAI -> Tone.SUCCESS
    ServiceStatus.SEGERA_SERVIS, ServiceStatus.JATUH_TEMPO -> Tone.WARNING
    ServiceStatus.TERLAMBAT -> Tone.ERROR
    ServiceStatus.KM_BELUM_DIPERBARUI -> Tone.INFO
}

fun docTone(s: DocStatus) = when (s) {
    DocStatus.AMAN -> Tone.SUCCESS
    DocStatus.SEGERA, DocStatus.HARI_INI -> Tone.WARNING
    DocStatus.TERLAMBAT -> Tone.ERROR
    DocStatus.BELUM_DIATUR -> Tone.NEUTRAL
}

/** Selalu memuat label teks (dengan simbol), bukan hanya warna. */
@Composable
fun StatusBadge(label: String, tone: Tone) {
    val c = Odo.colors
    val color: Color = when (tone) { Tone.SUCCESS -> c.success; Tone.WARNING -> c.warning; Tone.ERROR -> c.error; Tone.INFO -> c.blue; Tone.NEUTRAL -> c.textSecondary }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.16f), border = BorderStroke(1.dp, color.copy(alpha = 0.4f))) {
        Text(label, color = color, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
fun OdometerValue(value: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, color = Odo.colors.accent,
            style = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
        Text(" km", color = Odo.colors.textSecondary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))
    }
}

@Composable
fun SectionHeader(title: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (actionLabel != null) TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
fun EmptyState(icon: ImageVector = Icons.Default.Info, title: String, desc: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = Odo.colors.accent, modifier = Modifier.size(40.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(desc, color = Odo.colors.textSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        if (actionLabel != null) PrimaryButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
fun ConfirmationDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) =
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })

private val showFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("id", "ID"))

/** value berformat yyyy-MM-dd (atau kosong). Memakai Material DatePicker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val shown = runCatching { LocalDate.parse(value).format(showFmt) }.getOrDefault("")
    Box {
        OutlinedTextField(shown, {}, readOnly = true, singleLine = true, label = { Text(label) },
            placeholder = { Text("Pilih tanggal") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Pilih tanggal") })
        Box(Modifier.matchParentSize().clickable { open = true })
    }
    if (open) {
        val init = runCatching { LocalDate.parse(value).toEpochDay() * 86_400_000L }.getOrNull()
        val st = rememberDatePickerState(initialSelectedDateMillis = init)
        DatePickerDialog(onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = {
                st.selectedDateMillis?.let { onChange(LocalDate.ofEpochDay(it / 86_400_000L).toString()) }; open = false }) { Text("Pilih") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Batal") } }) { DatePicker(st) }
    }
}

/** Format 24 jam. hour/minute berupa teks angka. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(label: String, hour: String, minute: String, onChange: (String, String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField("${hour.padStart(2, '0')}.${minute.padStart(2, '0')}", {}, readOnly = true, singleLine = true,
            label = { Text(label) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
        Box(Modifier.matchParentSize().clickable { open = true })
    }
    if (open) {
        val st = rememberTimePickerState(hour.toIntOrNull() ?: 8, minute.toIntOrNull() ?: 0, true)
        AlertDialog(onDismissRequest = { open = false }, title = { Text(label) }, text = { TimePicker(st) },
            confirmButton = { TextButton(onClick = { onChange(st.hour.toString(), st.minute.toString()); open = false }) { Text("Pilih") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Batal") } })
    }
}
