package com.hermanns.hermannsgerenciador.salesman.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hermanns.hermannsgerenciador.salesman.data.SalesmanDatabase
import com.hermanns.hermannsgerenciador.salesman.util.SalesmanPrefs
import com.hermanns.hermannsgerenciador.ui.theme.HermannsGerenciadorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SalesmanLoginActivity : ComponentActivity() {

    private val credentials = mapOf(
        "joao" to "a",
        "teste" to "teste",
        "george" to "1234",
        "juliano" to "1919",
        "deividi" to "123",
        "marcos" to "123",
        "carlos" to "123",
        "jonas" to "12345",
        "alex" to "123",
        "claudio" to "123",
        "franco" to "123",
        "marcelo" to "123",
        "rafael" to "123",
        "marcio" to "123",
        "vander" to "123",
        "bruno" to "123",
        "raquel" to "123"
    )

    private val vehiclePlates = listOf(
        "Veículos", "IUS1207", "JBK1H35", "IYP1575", "IYP2256", "IUI2F96", "KCR3D12",
        "AQA3D14", "IBI3315", "IJW4D45", "IWU4650", "JDJ4F52", "QJH4I95", "JAV6D48",
        "IUT6C54", "IXZ6878", "JCK8H58", "ITZ9265", "RLI9B75", "CCP9B91", "JDI9H93"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SalesmanPrefs.isSessionActive(this) || SalesmanPrefs.isTrackingEnabled(this)) {
            val salesmanId = SalesmanPrefs.getSalesmanId(this) ?: "UNKNOWN"
            val vehiclePlate = SalesmanPrefs.getVehiclePlate(this) ?: "NO_VEHICLE"
            navigateToTracking(salesmanId, vehiclePlate)
            return
        }

        if (hasOngoingReport()) {
            val salesmanId = SalesmanPrefs.getSalesmanId(this) ?: "UNKNOWN"
            val vehiclePlate = SalesmanPrefs.getVehiclePlate(this) ?: "NO_VEHICLE"
            navigateToTracking(salesmanId, vehiclePlate)
            return
        }

        setContent {
            HermannsGerenciadorTheme {
                LoginScreen()
            }
        }
    }

    private fun hasOngoingReport(): Boolean {
        val database = SalesmanDatabase.getDatabase(this)
        val locationDao = database.locationDao()
        val expenseDao = database.expenseDao()
        return runBlocking(Dispatchers.IO) {
            val manualLocations = locationDao.getManualLocations().first().any { !it.isExported }
            val autoLocations = locationDao.getAutoLocations().first().any { !it.isExported }
            val expenses = expenseDao.getUnexportedExpenses().first().isNotEmpty()
            manualLocations || autoLocations || expenses
        }
    }

    private fun navigateToTracking(salesmanId: String, vehiclePlate: String) {
        val intent = Intent(this, SalesmanTrackingActivity::class.java).apply {
            putExtra("salesman_id", salesmanId)
            putExtra("vehicle_plate", vehiclePlate)
        }
        startActivity(intent)
        finish()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun LoginScreen() {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var selectedVehicle by remember { mutableStateOf(vehiclePlates[0]) }
        var vehicleDropdownExpanded by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Vendedor Hermanns") }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Login do Vendedor",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuário") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = vehicleDropdownExpanded,
                    onExpandedChange = { vehicleDropdownExpanded = !vehicleDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedVehicle,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Veículo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = vehicleDropdownExpanded,
                        onDismissRequest = { vehicleDropdownExpanded = false }
                    ) {
                        vehiclePlates.forEach { plate ->
                            DropdownMenuItem(
                                text = { Text(plate) },
                                onClick = {
                                    selectedVehicle = plate
                                    vehicleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { attemptLogin(username.trim(), password.trim(), selectedVehicle) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Entrar", modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }

    private fun attemptLogin(username: String, password: String, vehiclePlate: String) {
        when {
            username.isEmpty() -> showToast("Por favor, insira o usuário")
            password.isEmpty() -> showToast("Por favor, insira a senha")
            vehiclePlate == "Veículos" -> showToast("Por favor, selecione um veículo")
            credentials[username] != password -> showToast("Credenciais inválidas")
            else -> {
                SalesmanPrefs.setSessionActive(this, true)
                SalesmanPrefs.setSalesmanId(this, username)
                SalesmanPrefs.setVehiclePlate(this, vehiclePlate)
                SalesmanPrefs.setTrackingEnabled(this, true)
                navigateToTracking(username, vehiclePlate)
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
