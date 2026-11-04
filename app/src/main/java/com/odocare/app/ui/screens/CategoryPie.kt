package com.odocare.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odocare.app.ui.*
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Biaya satu kategori servis. [categoryCount] > 1 hanya untuk irisan gabungan "Lainnya". */
data class CategoryCost(val name: String, val total: Long, val count: Int, val lastDate: Long?, val categoryCount: Int = 1)

private val SlicePalette = listOf(
    Color(0xFF8E7CFF), Color(0xFF4FC3F7), Color(0xFF66BB6A), Color(0xFFFFB74D),
    Color(0xFFF06292), Color(0xFF4DB6AC), Color(0xFFBA68C8)
)
private val OtherSliceColor = Color(0xFF90A4AE)
private const val MaxSlices = 7

private fun pctText(value: Long, total: Long): String =
    String.format(Locale("id", "ID"), "%.1f%%", if (total > 0) value * 100.0 / total else 0.0)

/**
 * Diagram pie biaya per kategori. Ketuk irisan (atau barisnya di daftar) untuk memilihnya:
 * irisan timbul sedikit dan detailnya tampil di bawah diagram. Ketuk lagi / ketuk di luar diagram untuk membatalkan.
 * Kategori di luar [MaxSlices] - 1 teratas digabung jadi "Lainnya".
 */
@Composable
fun CategoryPieSection(categories: List<CategoryCost>) {
    val positive = categories.filter { it.total > 0 }.sortedByDescending { it.total }
    if (positive.isEmpty()) return

    val slices: List<CategoryCost> = if (positive.size <= MaxSlices) positive else {
        val head = positive.take(MaxSlices - 1)
        val rest = positive.drop(MaxSlices - 1)
        head + CategoryCost("Lainnya", rest.sumOf { it.total }, rest.sumOf { it.count }, rest.mapNotNull { it.lastDate }.maxOrNull(), categoryCount = rest.size)
    }
    val sum = slices.sumOf { it.total }
    fun colorOf(i: Int) = if (slices[i].categoryCount > 1) OtherSliceColor else SlicePalette[i % SlicePalette.size]

    var selected by remember(slices) { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current
    fun select(i: Int?) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selected = if (i == selected) null else i }

    val popDp = 10.dp
    val popPx = with(LocalDensity.current) { popDp.toPx() }
    val separatorPx = with(LocalDensity.current) { 2.dp.toPx() }
    val separatorColor = SurfaceVariantDark
    val pops = slices.indices.map { i -> animateFloatAsState(if (selected == i) 1f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "pop$i").value }

    Surface(color = SurfaceVariantDark, shape = CardShape, border = CardBorder, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.M), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(
                Modifier.size(220.dp).pointerInput(slices) {
                    detectTapGestures { tap ->
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val radius = min(size.width, size.height) / 2f - popPx
                        val dx = tap.x - c.x; val dy = tap.y - c.y
                        if (sqrt(dx * dx + dy * dy) > radius + popPx) { if (selected != null) selected = null; return@detectTapGestures }
                        // sudut dari jam 12 searah jarum jam, 0..360
                        val angle = ((Math.toDegrees(atan2(dy, dx).toDouble()) + 90.0) % 360.0 + 360.0) % 360.0
                        var acc = 0.0
                        for (i in slices.indices) {
                            acc += slices[i].total * 360.0 / sum
                            if (angle < acc || i == slices.lastIndex) { select(i); return@detectTapGestures }
                        }
                    }
                }
            ) {
                val radius = min(size.width, size.height) / 2f - popPx
                val center = Offset(size.width / 2f, size.height / 2f)
                // Irisan terpilih digambar terakhir supaya berada di atas irisan lain
                val order = slices.indices.sortedBy { pops[it] }
                for (i in order) {
                    val start = -90f + (slices.take(i).sumOf { it.total } * 360.0 / sum).toFloat()
                    val sweep = (slices[i].total * 360.0 / sum).toFloat()
                    val mid = Math.toRadians((start + sweep / 2f).toDouble())
                    val shift = Offset((cos(mid) * popPx * pops[i]).toFloat(), (sin(mid) * popPx * pops[i]).toFloat())
                    val topLeft = Offset(center.x - radius, center.y - radius) + shift
                    drawArc(colorOf(i), start, sweep, useCenter = true, topLeft = topLeft, size = Size(radius * 2, radius * 2))
                    if (slices.size > 1) drawArc(separatorColor, start, sweep, useCenter = true, topLeft = topLeft, size = Size(radius * 2, radius * 2), style = Stroke(separatorPx))
                }
            }

            Spacer(Modifier.height(Spacing.M))
            val sel = selected
            if (sel == null) {
                Text("Ketuk irisan untuk melihat detail", color = TextGray, fontSize = 12.sp)
            } else {
                CategoryDetail(slices[sel], colorOf(sel), sum)
            }

            Spacer(Modifier.height(Spacing.S))
            slices.forEachIndexed { i, c ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { select(i) }
                        .background(if (selected == i) PillSelected else Color.Transparent)
                        .padding(horizontal = Spacing.S, vertical = Spacing.S),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(10.dp).background(colorOf(i), CircleShape))
                    Spacer(Modifier.width(Spacing.S))
                    Text(c.name, Modifier.weight(1f), color = TextWhite, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(pctText(c.total, sum), color = TextGray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CategoryDetail(c: CategoryCost, color: Color, sum: Long) {
    Surface(color = PillSelected, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.M)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).background(color, CircleShape))
                Spacer(Modifier.width(Spacing.S))
                Text(c.name, Modifier.weight(1f), color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(pctText(c.total, sum), color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(Spacing.XS))
            Text(rupiah(c.total), color = LavenderPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(Spacing.XS))
            val info = buildList {
                if (c.categoryCount > 1) add("${c.categoryCount} kategori")
                add("${c.count} servis")
                if (c.count > 0) add("rata-rata ${rupiah(c.total / c.count)}")
            }.joinToString(" · ")
            Text(info, color = TextGray, fontSize = 12.sp)
            c.lastDate?.let { Text("Terakhir ${fmtDate(it)}", color = TextGray, fontSize = 12.sp) }
        }
    }
}
