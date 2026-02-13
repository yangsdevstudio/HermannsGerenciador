package com.hermanns.hermannsgerenciador.net

import com.hermanns.hermannsgerenciador.data.Medication
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object CsvParser {

    private val DATE_FORMATTERS = listOf(
        "dd/MM/yyyy", "d/M/yyyy", "dd-MM-yyyy", "d-M-yyyy",
        "MM/dd/yyyy", "M/d/yyyy", "yyyy-MM-dd", "yyyy/MM/dd", "dd.MM.yyyy",
        "MM/yyyy", "M/yyyy"
    ).map { DateTimeFormatter.ofPattern(it) }

    fun parse(csvText: String): List<List<String>> {
        return csvText.lines()
            .filter { it.isNotBlank() }
            .map { splitCsvLine(it) }
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
            i++
        }
        if (current.isNotEmpty()) {
            result.add(current.toString())
        }
        return result
    }

    fun parseDateLenient(text: String): LocalDate? {
        if (text.isBlank()) return null
        val trimmed = text.trim()

        for (formatter in DATE_FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, formatter)
            } catch (_: DateTimeParseException) { }
        }

        // Fallback manual parse
        val parts = trimmed.split("/", "-").map { it.trim() }
        try {
            when (parts.size) {
                3 -> {  // dd/mm/yyyy or similar
                    var day = parts[0].toInt()
                    var month = parts[1].toInt()
                    var year = parts[2].toInt()
                    if (year < 100) year += 2000
                    return LocalDate.of(year, month, day)
                }
                2 -> {  // mm/yyyy, assume last day of month for expiry (conservative)
                    var month = parts[0].toInt()
                    var year = parts[1].toInt()
                    if (year < 100) year += 2000
                    val lastDay = LocalDate.of(year, month, 1).lengthOfMonth()
                    return LocalDate.of(year, month, lastDay)
                }
            }
        } catch (_: Exception) { }
        return null
    }

    fun parseMedications(csv: String, labName: String): List<Medication> {
        val rows = parse(csv)
        if (rows.isEmpty()) return emptyList()

        val header = rows.first().map { it.lowercase().trim() }
        val nameIdx = header.indexOfFirst { it.contains("descr") || it.contains("nome") || it.contains("produto") }
        val codeIdx = header.indexOfFirst { it.contains("cód") || it.contains("cod") || it.contains("code") }  // Added "cod" for no accent
        val qtyIdx = header.indexOfFirst { it.contains("estoque") || it.contains("qtd") || it.contains("qty") || it.contains("quantidade") }  // Added "quantidade"
        val dateIdx = header.indexOfFirst { it.contains("valid") || it.contains("venc") || it.contains("exp") }

        if (nameIdx == -1 || dateIdx == -1) return emptyList()

        return rows.drop(1).mapNotNull { cols ->
            val name = cols.getOrNull(nameIdx)?.trim() ?: return@mapNotNull null
            val code = cols.getOrNull(codeIdx)?.trim() ?: "N/A"
            val qty = cols.getOrNull(qtyIdx)?.trim()?.toIntOrNull() ?: 0
            val dateStr = cols.getOrNull(dateIdx)?.trim() ?: return@mapNotNull null
            val date = parseDateLenient(dateStr) ?: return@mapNotNull null

            Medication(code, name, qty, date, labName)
        }
    }
}