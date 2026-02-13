package com.hermanns.hermannsgerenciador.salesman.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.hermanns.hermannsgerenciador.R
import com.hermanns.hermannsgerenciador.salesman.data.LocationEntity
import com.hermanns.hermannsgerenciador.salesman.data.SalesmanDatabase
import com.hermanns.hermannsgerenciador.salesman.util.SalesmanPrefs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.*

class LocationTrackingService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var database: SalesmanDatabase
    private lateinit var notificationManager: NotificationManager
    private val handler = Handler(Looper.getMainLooper())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private lateinit var locationRequest: LocationRequest
    private var updateInterval: Long = 1800000L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            locationResult.lastLocation?.let { location ->
                saveLocationToDatabase(
                    LocationEntity(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        timestamp = System.currentTimeMillis(),
                        local = "Auto-coletado",
                        motivo = "Atualização periódica",
                        km = "",
                        comentario = "Coletado automaticamente pelo app",
                        salesmanId = SalesmanPrefs.getSalesmanId(this@LocationTrackingService) ?: "",
                        vehiclePlate = SalesmanPrefs.getVehiclePlate(this@LocationTrackingService) ?: "",
                        isExported = false
                    )
                )
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        initializeComponents()
        setupLocationRequest()
    }

    private fun initializeComponents() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        database = SalesmanDatabase.getDatabase(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun setupLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, updateInterval).apply {
            setMinUpdateIntervalMillis(updateInterval)
            setWaitForAccurateLocation(false)
        }.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!SalesmanPrefs.isTrackingEnabled(this)) {
            Log.i("LocationTracking", "Tracking disabled, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()
        scheduleAutoStopCheck()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e("LocationTracking", "Failed to start location updates", e)
        }
    }

    private fun scheduleAutoStopCheck() {
        handler.postDelayed({
            if (System.currentTimeMillis() - SalesmanPrefs.getLastSnapshotTime(this) > AUTO_STOP_DELAY) {
                generateEmergencyReportAndStop()
            } else {
                scheduleAutoStopCheck()
            }
        }, CHECK_INTERVAL)
    }

    private fun generateEmergencyReportAndStop() {
        serviceScope.launch {
            try {
                val locations = withContext(Dispatchers.IO) { database.locationDao().getUnexportedLocations().first() }
                saveEmergencyReport(locations)
            } catch (e: Exception) {
                Log.e("LocationTracking", "Emergency report failed", e)
            } finally {
                stopSelf()
            }
        }
    }

    private suspend fun saveEmergencyReport(locations: List<LocationEntity>) {
        val filename = "Emergency_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.xlsx"

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri == null) {
                withContext(Dispatchers.Main) { showToast("Erro ao criar arquivo de emergência no Downloads") }
                return
            }

            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Emergency Locations")
            val headerRow = sheet.createRow(0)
            val headers = arrayOf("Vendedor", "Veículo", "Data e Hora", "Latitude", "Longitude", "Precisão (m)", "Local", "Motivo", "KM", "Comentário")
            headers.forEachIndexed { index, header -> headerRow.createCell(index).setCellValue(header) }

            locations.forEachIndexed { index, location ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(location.salesmanId)
                row.createCell(1).setCellValue(location.vehiclePlate)
                row.createCell(2).setCellValue(dateFormat.format(Date(location.timestamp)))
                row.createCell(3).setCellValue(location.latitude)
                row.createCell(4).setCellValue(location.longitude)
                row.createCell(5).setCellValue(location.accuracy.toDouble())
                row.createCell(6).setCellValue(location.local)
                row.createCell(7).setCellValue(location.motivo)
                row.createCell(8).setCellValue(location.km)
                row.createCell(9).setCellValue(location.comentario)
            }

            contentResolver.openOutputStream(uri)?.use { outputStream ->
                workbook.write(outputStream)
                outputStream.flush()
            } ?: run {
                withContext(Dispatchers.Main) { showToast("Erro ao escrever arquivo de emergência no Downloads") }
                return
            }
            workbook.close()

            if (locations.isNotEmpty()) {
                database.locationDao().markAllAsExported()
            }

            withContext(Dispatchers.Main) {
                showToast("Relatório de emergência gerado em Downloads: $filename")
            }
        } catch (e: Exception) {
            Log.e("LocationTracking", "Failed to save emergency report", e)
            withContext(Dispatchers.Main) {
                showToast("Erro ao gerar relatório de emergência: ${e.localizedMessage}")
            }
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun saveLocationToDatabase(entity: LocationEntity) {
        serviceScope.launch { database.locationDao().insert(entity) }
    }

    private fun createNotification(): Notification {
        createNotificationChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Rastreamento Ativo")
            .setContentText("Coletando localizações periodicamente")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(CHANNEL_ID, "Location Tracking", NotificationManager.IMPORTANCE_LOW).apply {
                notificationManager.createNotificationChannel(this)
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1234
        private const val CHANNEL_ID = "tracking_channel"
        private const val AUTO_STOP_DELAY = 5 * 60 * 60 * 1000L
        private const val CHECK_INTERVAL = 30 * 60 * 1000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, LocationTrackingService::class.java))
        }
    }
}
