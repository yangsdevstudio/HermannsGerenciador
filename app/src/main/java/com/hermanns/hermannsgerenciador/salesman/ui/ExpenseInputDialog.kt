package com.hermanns.hermannsgerenciador.salesman.ui

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class ExpenseData(
    val expenseType: String,
    val paymentType: String,
    val issuer: String,
    val issueDate: String,
    val description: String,
    val totalAmount: Double,
    val documentNumber: String,
    val documentPhotoPath: String?,
    val km: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseInputDialog(
    onDismiss: () -> Unit,
    onSubmit: (ExpenseData) -> Unit
) {
    val context = LocalContext.current
    val expenseTypes = listOf(
        "Alimentação", "Hospedagem", "Combustível", "Pedágio", "Balsa", "Peças",
        "Mão de Obra", "Pneu(s)", "Balanceamento", "Geometria", "Guincho", "Cambagem"
    )
    val paymentTypes = listOf("Dinheiro/PIX", "Cartão", "Boleto")
    val kmRequiredTypes = listOf("Combustível", "Peças", "Mão de Obra", "Pneu(s)", "Balanceamento", "Geometria", "Cambagem")

    var selectedExpenseType by remember { mutableStateOf("") }
    var expenseTypeExpanded by remember { mutableStateOf(false) }
    var selectedPaymentType by remember { mutableStateOf("") }
    var paymentTypeExpanded by remember { mutableStateOf(false) }
    var issuer by remember { mutableStateOf("") }
    var issueDate by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var documentNumber by remember { mutableStateOf("") }
    var km by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoButtonText by remember { mutableStateOf("Foto do Documento") }

    val showKm = kmRequiredTypes.contains(selectedExpenseType)

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoUri != null) {
            photoUri = tempPhotoUri
            photoButtonText = "Foto capturada"
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri
            photoButtonText = "Foto selecionada"
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val photoFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
            tempPhotoUri = uri
            takePictureLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Registrar Gasto") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Expense Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = expenseTypeExpanded,
                        onExpandedChange = { expenseTypeExpanded = !expenseTypeExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedExpenseType.ifEmpty { "Tipo de gasto" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de gasto *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expenseTypeExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expenseTypeExpanded,
                            onDismissRequest = { expenseTypeExpanded = false }
                        ) {
                            expenseTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        selectedExpenseType = type
                                        expenseTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Payment Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = paymentTypeExpanded,
                        onExpandedChange = { paymentTypeExpanded = !paymentTypeExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedPaymentType.ifEmpty { "Tipo de pagamento" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de pagamento *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentTypeExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = paymentTypeExpanded,
                            onDismissRequest = { paymentTypeExpanded = false }
                        ) {
                            paymentTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        selectedPaymentType = type
                                        paymentTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = issuer,
                        onValueChange = { issuer = it },
                        label = { Text("Emissor da nota fiscal *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = issueDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Data de emissão *") },
                        modifier = Modifier.fillMaxWidth(),
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also { interactionSource ->
                            LaunchedEffect(interactionSource) {
                                interactionSource.interactions.collect { interaction ->
                                    if (interaction is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                        val calendar = Calendar.getInstance()
                                        DatePickerDialog(
                                            context,
                                            { _, year, month, day ->
                                                issueDate = String.format(Locale("pt", "BR"), "%02d/%02d/%04d", day, month + 1, year)
                                            },
                                            calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH),
                                            calendar.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição do gasto *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = totalAmount,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() || it == ',' || it == '.' }) {
                                totalAmount = newValue
                            }
                        },
                        label = { Text("Valor total *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = documentNumber,
                        onValueChange = { documentNumber = it },
                        label = { Text("Número do documento *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showKm) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = km,
                            onValueChange = { km = it },
                            label = { Text("KM *") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            val options = arrayOf("Tirar Foto", "Escolher da Galeria")
                            android.app.AlertDialog.Builder(context)
                                .setTitle("Foto do documento")
                                .setItems(options) { _, which ->
                                    when (which) {
                                        0 -> cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                        1 -> pickImageLauncher.launch("image/*")
                                    }
                                }
                                .show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(photoButtonText)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Submit button at bottom
                Button(
                    onClick = {
                        val amountParsed = totalAmount.replace(",", ".").toDoubleOrNull()
                        when {
                            selectedExpenseType.isEmpty() || selectedPaymentType.isEmpty() ->
                                Toast.makeText(context, "Selecione tipo de gasto e pagamento", Toast.LENGTH_SHORT).show()
                            issuer.isBlank() || issueDate.isBlank() || description.isBlank() || amountParsed == null || documentNumber.isBlank() ->
                                Toast.makeText(context, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
                            showKm && km.isBlank() ->
                                Toast.makeText(context, "KM é obrigatório para $selectedExpenseType", Toast.LENGTH_SHORT).show()
                            else -> {
                                onSubmit(
                                    ExpenseData(
                                        expenseType = selectedExpenseType,
                                        paymentType = selectedPaymentType,
                                        issuer = issuer.trim(),
                                        issueDate = issueDate.trim(),
                                        description = description.trim(),
                                        totalAmount = amountParsed,
                                        documentNumber = documentNumber.trim(),
                                        documentPhotoPath = photoUri?.toString(),
                                        km = km.trim().takeIf { it.isNotEmpty() }
                                    )
                                )
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Registrar Gasto", modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}
