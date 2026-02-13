package com.hermanns.hermannsgerenciador.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hermanns.hermannsgerenciador.repo.SheetsApiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class DailyExpiryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val repository = SheetsApiRepository(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val meds = repository.loadFromCache()
                val count = meds.count {
                    val days = ChronoUnit.DAYS.between(LocalDate.now(), it.expiryDate)
                    days in 1..90
                }
                NotificationHelper.sendDailySummary(context, count)
            } finally {
                pendingResult.finish()
            }
        }
    }
}