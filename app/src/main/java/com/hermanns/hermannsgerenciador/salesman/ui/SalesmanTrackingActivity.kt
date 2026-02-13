package com.hermanns.hermannsgerenciador.salesman.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.hermanns.hermannsgerenciador.salesman.data.ExpenseEntity
import com.hermanns.hermannsgerenciador.salesman.data.LocationEntity
import com.hermanns.hermannsgerenciador.salesman.data.SalesmanDatabase
import com.hermanns.hermannsgerenciador.salesman.services.LocationTrackingService
import com.hermanns.hermannsgerenciador.salesman.util.SalesmanPrefs
import com.hermanns.hermannsgerenciador.ui.theme.HermannsGerenciadorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.util.Units
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class SalesmanTrackingActivity : ComponentActivity() {

    private lateinit var salesmanId: String
    private lateinit var vehiclePlate: String
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val database by lazy { SalesmanDatabase.getDatabase(this) }
    private val locationDao by lazy { database.locationDao() }
    private val expenseDao by lazy { database.expenseDao() }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private val storagePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.all { it }) {
            showExportDialog = true
        } else {
            showToast("Permissões de armazenamento negadas.")
        }
    }

    private val openSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        checkBackgroundLocationPermission()
    }

    private var showExportDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        salesmanId = intent.getStringExtra("salesman_id") ?: "UNKNOWN"
        vehiclePlate = intent.getStringExtra("vehicle_plate") ?: "NO_VEHICLE"
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (checkAndRequestLocationPermissions()) {
            startTrackingService()
            requestBatteryOptimizationExemption()
            checkBackgroundLocationPermission()
        }

        setContent {
            HermannsGerenciadorTheme {
                TrackingScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TrackingScreen() {
        val locations by locationDao.getUnexportedLocations().collectAsState(initial = emptyList())
        var showLocationDialog by remember { mutableStateOf(false) }
        var showExpenseDialog by remember { mutableStateOf(false) }
        var showLogoutDialog by remember { mutableStateOf(false) }
        var currentLocation by remember { mutableStateOf<android.location.Location?>(null) }

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(-27.8708, -54.4808), 14f)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Vendedor Hermanns") },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Text(
                    text = "Bem-vindo(a), $salesmanId",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Google Map
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = hasLocationPermission())
                ) {
                    locations.forEach { location ->
                        Marker(
                            state = MarkerState(position = LatLng(location.latitude, location.longitude)),
                            title = "${location.local} - ${dateFormat.format(Date(location.timestamp))}"
                        )
                    }
                }

                // Action Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!SalesmanPrefs.isSessionActive(this@SalesmanTrackingActivity)) {
                                    SalesmanPrefs.setSessionActive(this@SalesmanTrackingActivity, true)
                                    SalesmanPrefs.setTrackingEnabled(this@SalesmanTrackingActivity, true)
                                    LocationTrackingService.start(this@SalesmanTrackingActivity)
                                }
                                SalesmanPrefs.updateLastSnapshotTime(this@SalesmanTrackingActivity)
                                takeLocationSnapshot { location ->
                                    currentLocation = location
                                    showLocationDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Gravar Local")
                        }
                        Button(
                            onClick = { showExpenseDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Registrar Gasto")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { checkStoragePermissionsAndExport() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Gerar Relatório")
                        }
                        Button(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Encerrar Sessão")
                        }
                    }
                }
            }
        }

        // Dialogs
        if (showLocationDialog && currentLocation != null) {
            LocationInputDialog(
                onDismiss = { showLocationDialog = false },
                onSave = { local, motivo, km, comentario ->
                    val loc = currentLocation!!
                    val entity = LocationEntity(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracy = loc.accuracy,
                        timestamp = System.currentTimeMillis(),
                        local = local,
                        motivo = motivo,
                        km = km,
                        comentario = comentario,
                        salesmanId = salesmanId,
                        vehiclePlate = vehiclePlate
                    )
                    lifecycleScope.launch {
                        locationDao.insert(entity)
                        showToast("Visita registrada com sucesso!")
                    }
                }
            )
        }

        if (showExpenseDialog) {
            ExpenseInputDialog(
                onDismiss = { showExpenseDialog = false },
                onSubmit = { expenseData ->
                    lifecycleScope.launch {
                        val expense = ExpenseEntity(
                            expenseType = expenseData.expenseType,
                            paymentType = expenseData.paymentType,
                            issuer = expenseData.issuer,
                            issueDate = expenseData.issueDate,
                            description = expenseData.description,
                            totalAmount = expenseData.totalAmount,
                            documentNumber = expenseData.documentNumber,
                            documentPhotoPath = expenseData.documentPhotoPath,
                            km = expenseData.km,
                            salesmanId = salesmanId,
                            timestamp = System.currentTimeMillis()
                        )
                        expenseDao.insert(expense)
                        showToast("Gasto registrado com sucesso!")
                    }
                }
            )
        }

        if (showLogoutDialog) {
            var hasData by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                hasData = withContext(Dispatchers.IO) { hasOngoingReport() }
            }
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Encerrar Sessão") },
                text = {
                    Text(
                        if (hasData) "Há dados não exportados (localizações ou gastos). Deseja encerrar a sessão e excluir esses dados?"
                        else "Deseja encerrar a sessão e parar o rastreamento?"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    locationDao.deleteUnexportedLocations()
                                    expenseDao.deleteUnexportedExpenses()
                                }
                                SalesmanPrefs.clearSession(this@SalesmanTrackingActivity)
                                stopService(Intent(this@SalesmanTrackingActivity, LocationTrackingService::class.java))
                                val intent = Intent(this@SalesmanTrackingActivity, SalesmanLoginActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                finish()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Sim")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Não")
                    }
                }
            )
        }

        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Exportar Como") },
                text = {
                    Column {
                        TextButton(onClick = {
                            showExportDialog = false
                            lifecycleScope.launch { exportAndShare("xcl") }
                        }) {
                            Text("XLSX (Armazenamento Interno)")
                        }
                        TextButton(onClick = {
                            showExportDialog = false
                            lifecycleScope.launch { exportAndShare("txt") }
                        }) {
                            Text("Texto (Armazenamento Interno)")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun takeLocationSnapshot(onLocationObtained: (android.location.Location) -> Unit) {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    onLocationObtained(location)
                } else {
                    showToast("Não foi possível obter a localização atual")
                }
            }.addOnFailureListener { e ->
                showToast("Erro ao obter localização: ${e.localizedMessage}")
            }
        } catch (e: SecurityException) {
            showToast("Permissão de localização necessária")
        }
    }

    private fun checkAndRequestLocationPermissions(): Boolean {
        val permissionsNeeded = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), 1003)
            false
        } else {
            true
        }
    }

    private fun checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Permissão de Localização em Segundo Plano")
                .setMessage("Por favor selecione 'Permitir o tempo todo' nas configurações do aplicativo.")
                .setPositiveButton("Ir para Configurações") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    openSettingsLauncher.launch(intent)
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss()
                    showToast("O relatório não será gerado com sucesso sem essa permissão.")
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun startTrackingService() {
        if (hasLocationPermission()) {
            SalesmanPrefs.setTrackingEnabled(this, true)
            LocationTrackingService.start(this)
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Otimização de Bateria")
                    .setMessage("Para garantir que o rastreamento funcione em segundo plano, desative a otimização de bateria para este aplicativo.")
                    .setPositiveButton("Ir para Configurações") { _, _ ->
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }

    private fun checkStoragePermissionsAndExport() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val permissionsToRequest = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (permissionsToRequest.isNotEmpty()) {
                storagePermissionLauncher.launch(permissionsToRequest.toTypedArray())
            } else {
                showExportDialog = true
            }
        } else {
            showExportDialog = true
        }
    }

    private suspend fun hasOngoingReport(): Boolean {
        val manualLocations = locationDao.getManualLocations().first().any { !it.isExported }
        val autoLocations = locationDao.getAutoLocations().first().any { !it.isExported }
        val expenses = expenseDao.getUnexportedExpenses().first().isNotEmpty()
        return manualLocations || autoLocations || expenses
    }

    private suspend fun exportAndShare(format: String) {
        showToast("Gerando relatório...")
        val file = withContext(Dispatchers.IO) { generateReport(format) }
        file?.let {
            withContext(Dispatchers.Main) {
                showToast("Relatório gerado com sucesso!")
                shareReportFile(it)
            }
        } ?: showToast("Falha ao gerar relatório")
    }

    private suspend fun generateReport(format: String): File? {
        return try {
            val manualLocations = locationDao.getManualLocations().first().filter { !it.isExported }
            val autoLocations = locationDao.getAutoLocations().first().filter { !it.isExported }
            val expenses = expenseDao.getUnexportedExpenses().first()

            if (manualLocations.isEmpty() && autoLocations.isEmpty() && expenses.isEmpty()) {
                withContext(Dispatchers.Main) { showToast("Nenhum dado novo para exportar") }
                return null
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return null

            val file = if (format == "xcl") {
                generateXlsxReport(manualLocations, autoLocations, expenses, dir, timestamp)
            } else {
                val txtFile = File(dir, "Relatorio_${salesmanId}_${timestamp}.txt")
                FileOutputStream(txtFile).use { fos ->
                    java.io.OutputStreamWriter(fos, java.nio.charset.StandardCharsets.UTF_8).use { writer ->
                        writer.write("Data e Hora|Latitude|Longitude|Precisão|Local|Motivo|KM|Comentário\n")
                        (manualLocations + autoLocations).forEach { location ->
                            writer.write("${dateFormat.format(Date(location.timestamp))}|${location.latitude}|${location.longitude}|" +
                                    "${String.format(Locale.US, "%.1f", location.accuracy)}|${location.local}|${location.motivo}|" +
                                    "${location.km}|${location.comentario}\n")
                        }
                    }
                }
                txtFile
            }

            file?.let {
                locationDao.markAllAsExported()
                expenseDao.markAllAsExported()
            }
            file
        } catch (e: Exception) {
            Log.e("SalesmanTracking", "Failed to generate report", e)
            withContext(Dispatchers.Main) { showToast("Erro ao gerar relatório: ${e.localizedMessage}") }
            null
        }
    }

    private fun generateXlsxReport(
        manualLocations: List<LocationEntity>,
        autoLocations: List<LocationEntity>,
        expenses: List<ExpenseEntity>,
        dir: File,
        timestamp: String
    ): File? {
        try {
            val filename = "Relatorio_${salesmanId}_$timestamp.xlsx"
            val file = File(dir, filename)
            val workbook = XSSFWorkbook()
            val creationHelper = workbook.creationHelper

            // Sheet 1: Gravações Manuais
            val manualSheet = workbook.createSheet("Gravações Manuais")
            val manualHeaderRow = manualSheet.createRow(0)
            val manualHeaders = arrayOf("Vendedor", "Veículo", "Data e Hora", "Latitude", "Longitude", "Precisão (m)", "Local", "Motivo", "KM", "Comentário")
            manualHeaders.forEachIndexed { index, header -> manualHeaderRow.createCell(index).setCellValue(header) }
            manualLocations.forEachIndexed { index, location ->
                val row = manualSheet.createRow(index + 1)
                row.createCell(0).setCellValue(salesmanId)
                row.createCell(1).setCellValue(vehiclePlate)
                row.createCell(2).setCellValue(dateFormat.format(Date(location.timestamp)))
                row.createCell(3).setCellValue(location.latitude)
                row.createCell(4).setCellValue(location.longitude)
                row.createCell(5).setCellValue(location.accuracy.toDouble())
                row.createCell(6).setCellValue(location.local)
                row.createCell(7).setCellValue(location.motivo)
                row.createCell(8).setCellValue(location.km)
                row.createCell(9).setCellValue(location.comentario)
            }

            // Sheet 2: Gastos
            val expenseSheet = workbook.createSheet("Gastos")
            val expenseHeaderRow = expenseSheet.createRow(0)
            val expenseHeaders = arrayOf("Vendedor", "Veículo", "Tipo de despesa", "Tipo de pagamento", "Emissor", "Data", "Número", "Valor", "Descrição", "KM", "Foto")
            expenseHeaders.forEachIndexed { index, header -> expenseHeaderRow.createCell(index).setCellValue(header) }
            val drawing = expenseSheet.createDrawingPatriarch()

            expenses.forEachIndexed { index, expense ->
                val row = expenseSheet.createRow(index + 1)
                row.createCell(0).setCellValue(salesmanId)
                row.createCell(1).setCellValue(vehiclePlate)
                row.createCell(2).setCellValue(expense.expenseType)
                row.createCell(3).setCellValue(expense.paymentType)
                row.createCell(4).setCellValue(expense.issuer)
                row.createCell(5).setCellValue(expense.issueDate)
                row.createCell(6).setCellValue(expense.documentNumber)
                row.createCell(7).setCellValue(expense.totalAmount)
                row.createCell(8).setCellValue(expense.description)
                row.createCell(9).setCellValue(expense.km ?: "")

                expense.documentPhotoPath?.let { path ->
                    val bitmap = try {
                        val uri = Uri.parse(path)
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            android.graphics.BitmapFactory.decodeStream(inputStream)
                        }
                    } catch (e: Exception) {
                        Log.e("SalesmanTracking", "Failed to load photo: $path", e)
                        null
                    }
                    if (bitmap != null) {
                        val baos = java.io.ByteArrayOutputStream()
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos)
                        val imageBytes = baos.toByteArray()
                        val pictureIdx = workbook.addPicture(imageBytes, XSSFWorkbook.PICTURE_TYPE_JPEG)
                        val anchor = creationHelper.createClientAnchor().apply {
                            setCol1(10)
                            setRow1(index + 1)
                            setCol2(10)
                            setRow2(index + 1)
                            dx1 = 0
                            dy1 = 0
                            dx2 = Units.toEMU(25.0)
                            dy2 = Units.toEMU(25.0)
                        }
                        drawing.createPicture(anchor, pictureIdx)
                    }
                }
            }

            // Sheet 3: Gravações Automáticas
            val autoSheet = workbook.createSheet("Gravações Automáticas")
            val autoHeaderRow = autoSheet.createRow(0)
            val autoHeaders = arrayOf("Vendedor", "Veículo", "Data e Hora", "Latitude", "Longitude", "Precisão (m)", "Local", "Motivo", "KM", "Comentário")
            autoHeaders.forEachIndexed { index, header -> autoHeaderRow.createCell(index).setCellValue(header) }
            autoLocations.forEachIndexed { index, location ->
                val row = autoSheet.createRow(index + 1)
                row.createCell(0).setCellValue(salesmanId)
                row.createCell(1).setCellValue(vehiclePlate)
                row.createCell(2).setCellValue(dateFormat.format(Date(location.timestamp)))
                row.createCell(3).setCellValue(location.latitude)
                row.createCell(4).setCellValue(location.longitude)
                row.createCell(5).setCellValue(location.accuracy.toDouble())
                row.createCell(6).setCellValue(location.local)
                row.createCell(7).setCellValue(location.motivo)
                row.createCell(8).setCellValue(location.km)
                row.createCell(9).setCellValue(location.comentario)
            }

            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
                outputStream.flush()
            }
            workbook.close()
            return file
        } catch (e: Exception) {
            Log.e("SalesmanTracking", "Failed to generate XLSX report", e)
            return null
        }
    }

    private fun shareReportFile(file: File) {
        try {
            val fileUri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = when (file.extension) {
                    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    "txt" -> "text/plain"
                    else -> "*/*"
                }
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Relatório de Vendas - ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Compartilhar relatório").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e("SalesmanTracking", "Failed to share report", e)
            showToast("Erro ao compartilhar relatório. Tente novamente.")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1003 -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    startTrackingService()
                    requestBatteryOptimizationExemption()
                    checkBackgroundLocationPermission()
                } else {
                    showToast("Permissões de localização necessárias")
                }
            }
            1001 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, user can retry
                }
            }
        }
    }

    private fun showToast(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    }
}
