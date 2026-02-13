package com.hermanns.hermannsgerenciador.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hermanns.hermannsgerenciador.R
import com.hermanns.hermannsgerenciador.data.Medication
import com.hermanns.hermannsgerenciador.ui.MainActivity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object NotificationHelper {

    private const val CHANNEL_ID = "expiry_channel"
    private const val DAILY_NOTIFICATION_ID = 100
    private const val SINGLE_ENTRY_NOTIFICATION_ID = 101

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Validade de Medicamentos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações sobre vencimento de medicamentos"
            }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel) // Safe-call to avoid NPE
        }
    }

    fun checkAndNotifyNewEntry(context: Context, medications: List<Medication>) {
        val nearExpiry = medications.filter {
            val days = ChronoUnit.DAYS.between(LocalDate.now(), it.expiryDate)
            days in 1..90
        }

        if (nearExpiry.isEmpty()) return

        val prefs = context.getSharedPreferences("notified_meds", Context.MODE_PRIVATE)
        val newlyEntered = nearExpiry.filter { med ->
            val key = "${med.code}_${med.lab}"
            !prefs.getBoolean(key, false)
        }

        if (newlyEntered.isNotEmpty()) {
            val editor = prefs.edit()
            newlyEntered.forEach { med ->
                editor.putBoolean("${med.code}_${med.lab}", true)
            }
            editor.apply()
            sendSingleEntryNotification(context, newlyEntered.first())
        }
    }

    private fun sendSingleEntryNotification(context: Context, medication: Medication) {
        val days = ChronoUnit.DAYS.between(LocalDate.now(), medication.expiryDate)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Medicamento perto do vencimento")
            .setContentText("${medication.name} vence em $days dias")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${medication.name}\nCódigo: ${medication.code}\nLab: ${medication.lab}\nVence em $days dias"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        context.getSystemService(NotificationManager::class.java)
            ?.notify(SINGLE_ENTRY_NOTIFICATION_ID, builder.build())
    }

    fun scheduleDailyNotification(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return  // Safe null check

        val intent = Intent(context, DailyExpiryReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = LocalDateTime.now()
            .withHour(8)
            .withMinute(15)
            .withSecond(0)
            .withNano(0)

        val trigger = if (triggerTime.isBefore(LocalDateTime.now())) {
            triggerTime.plusDays(1)
        } else {
            triggerTime
        }

        val triggerMillis = trigger.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } else {
            // Fallback to inexact — no crash
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }

    fun sendDailySummary(context: Context, count: Int) {
        if (count == 0) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (count == 1) "1 medicamento perto do vencimento" else "$count medicamentos perto do vencimento"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Lembrete diário de vencimento")
            .setContentText("$text. Abra o Gerenciador para mais informações.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        context.getSystemService(NotificationManager::class.java)
            ?.notify(DAILY_NOTIFICATION_ID, builder.build())
    }
}