package com.odocare.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.odocare.app.domain.DocStatus
import com.odocare.app.domain.ServiceStatus

fun serviceColor(status: ServiceStatus): Pair<Color, Color> = when (status) {
    ServiceStatus.NORMAL -> StatusGreenBg to StatusGreenText
    ServiceStatus.SOON -> StatusGrayBg to StatusGrayText
    ServiceStatus.DUE, ServiceStatus.LATE -> StatusRedBg to StatusRedText
    ServiceStatus.NO_ODOMETER -> StatusGrayBg to StatusGrayText
}

fun docColor(status: DocStatus): Pair<Color, Color> = when (status) {
    DocStatus.SAFE -> StatusGreenBg to StatusGreenText
    DocStatus.SOON -> StatusGrayBg to StatusGrayText
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
