package com.hermanns.hermannsgerenciador.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hermanns.hermannsgerenciador.data.Medication
import com.hermanns.hermannsgerenciador.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun MedicationRow(item: Medication, formatter: DateTimeFormatter) {
    val daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), item.expiryDate)
    val indicatorColor = when {
        daysUntilExpiry < 14 -> ExpiryRed
        daysUntilExpiry <= 30 -> ExpiryOrange
        daysUntilExpiry <= 60 -> ExpiryYellow
        daysUntilExpiry <= 90 -> ExpiryLime
        else -> ExpiryGreen
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${item.name} (${item.quantity}x)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when {
                    daysUntilExpiry < 0 -> "Venceu ${-daysUntilExpiry}d atrás"
                    daysUntilExpiry == 0L -> "Vence hoje"
                    daysUntilExpiry == 1L -> "Vence amanhã"
                    daysUntilExpiry <= 30 -> "20% desc. - ${daysUntilExpiry} dias"
                    daysUntilExpiry <= 60 -> "15% desc. - ${daysUntilExpiry} dias"
                    daysUntilExpiry <= 90 -> "10% desc. - ${daysUntilExpiry} dias"
                    else -> "$daysUntilExpiry dias"
                },
                color = if (daysUntilExpiry <= 15) ExpiryTextRed else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Lab: ${item.lab}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(indicatorColor, shape = androidx.compose.foundation.shape.CircleShape)
                .align(Alignment.Top)
        )
    }
    // Note: Caller should add Divider if needed
}