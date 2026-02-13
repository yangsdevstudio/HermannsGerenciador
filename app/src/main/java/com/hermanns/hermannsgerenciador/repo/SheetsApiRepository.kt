package com.hermanns.hermannsgerenciador.repo

import android.content.Context
import android.util.Log
import com.hermanns.hermannsgerenciador.data.Medication
import com.hermanns.hermannsgerenciador.net.CsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class SheetsApiRepository(private val context: Context) {

    companion object {
        private const val CACHE_FILE = "medications_cache.csv"
        private const val EXPORT_URL = "https://docs.google.com/spreadsheets/d/%s/export?format=csv&gid=%s"
        private const val METADATA_URL = "https://sheets.googleapis.com/v4/spreadsheets/%s?fields=sheets.properties&key=%s"
        private const val TAG = "SheetsApiRepo"

        private fun csvEscape(field: String): String {
            return if (field.contains(',') || field.contains('"') || field.contains('\n')) {
                "\"${field.replace("\"", "\"\"")}\""
            } else {
                field
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun fetchAllAndCache(spreadsheetId: String, apiKey: String?): Result<List<Medication>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting fetch for spreadsheetId: $spreadsheetId")

            val tabs = mutableListOf<Pair<String, String>>()

            if (!apiKey.isNullOrBlank()) {
                try {
                    val url = METADATA_URL.format(spreadsheetId, apiKey)
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        val bodyStr = response.body?.string() ?: ""
                        if (response.isSuccessful && bodyStr.isNotBlank()) {
                            val json = JSONObject(bodyStr)
                            val sheets = json.getJSONArray("sheets")
                            for (i in 0 until sheets.length()) {
                                val props = sheets.getJSONObject(i).getJSONObject("properties")
                                val gid = props.getString("sheetId")
                                val title = props.getString("title")
                                tabs.add(gid to title)
                            }
                            Log.d(TAG, "Metadata fetched: ${tabs.size} tabs")
                        } else {
                            Log.e(TAG, "Metadata failed: HTTP ${response.code}, body: $bodyStr")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Metadata exception: ${e.message}", e)
                }
            } else {
                Log.d(TAG, "No API key, skipping metadata")
            }

            if (tabs.isEmpty()) {
                tabs.add("0" to "Principal")
                Log.d(TAG, "Using fallback tab: gid=0")
            }

            val allMeds = mutableListOf<Medication>()
            for ((gid, title) in tabs) {
                Log.d(TAG, "Downloading CSV for tab: $title (gid=$gid)")
                val csvUrl = EXPORT_URL.format(spreadsheetId, gid)
                val request = Request.Builder().url(csvUrl).build()
                try {
                    client.newCall(request).execute().use { response ->
                        val bodyStr = response.body?.string() ?: ""
                        if (response.isSuccessful && bodyStr.isNotBlank()) {
                            Log.d(TAG, "CSV downloaded for $title, size: ${bodyStr.length}")
                            val meds = CsvParser.parseMedications(bodyStr, title)
                            allMeds.addAll(meds)
                            Log.d(TAG, "Parsed ${meds.size} meds for $title")
                        } else {
                            Log.e(TAG, "CSV failed for $title: HTTP ${response.code}, body: $bodyStr")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "CSV exception for $title: ${e.message}", e)
                }
            }

            if (allMeds.isNotEmpty()) {
                cacheMedications(allMeds)
                Log.d(TAG, "Cached ${allMeds.size} meds")
                Result.success(allMeds.sortedBy { it.expiryDate })
            } else {
                Log.w(TAG, "No meds fetched from any tab – possible permission issue")
                Result.failure(Exception("No data fetched from any tab"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Overall fetch exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun loadFromCache(): List<Medication> = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, CACHE_FILE)
        if (!file.exists()) {
            Log.d(TAG, "No cache file")
            return@withContext emptyList()
        }

        Log.d(TAG, "Loading cache file")
        val csv = file.readText()
        val rows = CsvParser.parse(csv)
        if (rows.isEmpty()) {
            Log.w(TAG, "Empty cache rows")
            return@withContext emptyList()
        }

        rows.drop(1).mapNotNull { cols ->
            if (cols.size < 5) return@mapNotNull null
            val lab = cols[0].trim()
            val code = cols[1].trim()
            val name = cols[2].trim()
            val qty = cols[3].trim().toIntOrNull() ?: 0
            val dateStr = cols[4].trim()
            val date = CsvParser.parseDateLenient(dateStr) ?: return@mapNotNull null
            Medication(code, name, qty, date, lab)
        }.sortedBy { it.expiryDate }.also {
            Log.d(TAG, "Cache parsed: ${it.size} items")
        }
    }

    private suspend fun cacheMedications(meds: List<Medication>) {
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, CACHE_FILE)
            val csv = buildString {
                appendLine("lab,code,name,quantity,expiry")
                meds.forEach {
                    appendLine("${csvEscape(it.lab)},${csvEscape(it.code)},${csvEscape(it.name)},${it.quantity},${it.expiryDate}")
                }
            }
            file.writeText(csv)
            Log.d(TAG, "Cache written")
        }
    }
}