package com.hermanns.hermannsgerenciador.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Medication(
    val code: String,
    val name: String,
    val quantity: Int,
    val expiryDate: LocalDate,
    val lab: String
) {
    fun daysUntilExpiry(reference: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(reference, expiryDate)
}