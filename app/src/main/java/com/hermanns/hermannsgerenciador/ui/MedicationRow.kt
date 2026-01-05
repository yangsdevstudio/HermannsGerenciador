package com.hermanns.hermannsgerenciador.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hermanns.hermannsgerenciador.data.Medication
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.LocalDate

@Composable
fun MedicationRow(item: Medication, formatter: DateTimeFormatter) {
    val daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), item.expiryDate)
    val indicatorColor = when {
        daysUntilExpiry < 14 -> Color(0xFFB00020)      // Red
        daysUntilExpiry <= 30 -> Color(0xFFFFA000)    // Orange
        daysUntilExpiry <= 60 -> Color(0xFDFBE200)    // Yellow
        daysUntilExpiry <= 90 -> Color(0xFFbbdb44)    // lime
        else -> Color(0xFF2E7D32)                     // Green
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left side: Code + Quantity + Name
        Column(modifier = Modifier.weight(1f)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Código: ${item.code}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Quantidade: ${item.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        // Right side: Expiry date + days + Lab
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = item.expiryDate.format(formatter),
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
                color = if (daysUntilExpiry <= 15) Color(0xFFFF6B6A) else MaterialTheme.typography.bodyMedium.color,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Lab: ${item.lab}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        // Color indicator dot on the far left
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(indicatorColor, shape = androidx.compose.foundation.shape.CircleShape)
                .align(Alignment.Top)
        )
    }
    Divider()
}