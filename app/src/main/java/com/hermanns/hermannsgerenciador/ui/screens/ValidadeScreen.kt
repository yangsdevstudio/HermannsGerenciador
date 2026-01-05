package com.hermanns.hermannsgerenciador.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hermanns.hermannsgerenciador.BuildConfig
import com.hermanns.hermannsgerenciador.ui.MedicationRow
import com.hermanns.hermannsgerenciador.viewmodel.ValidadeViewModel
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValidadeScreen(viewModel: ValidadeViewModel) {
    val context = LocalContext.current
    val meds by viewModel.medications.collectAsState()
    val labs by viewModel.labs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var selectedLab by remember { mutableStateOf("Todos") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSort by remember { mutableStateOf("Validade Asc") }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val listState = rememberLazyListState()  // ← Track scroll state
    val scope = rememberCoroutineScope()     // ← For scrolling

    // Filtered + Sorted list
    val filteredAndSorted = meds
        .filter { selectedLab == "Todos" || it.lab == selectedLab }
        .filter { it.name.contains(searchQuery, ignoreCase = true) }
        .sortedWith(
            when (selectedSort) {
                "Nome A → Z" -> compareBy { it.name }
                "Nome Z → A" -> compareByDescending { it.name }
                "Código A → Z" -> compareBy { it.code }
                "Código Z → A" -> compareByDescending { it.code }
                "Validade Mais Longe" -> compareByDescending { it.expiryDate }
                else -> compareBy { it.expiryDate } // Validade Mais Próxima
            }
        )

    // Auto-scroll to top when filter/search/sort changes
    LaunchedEffect(selectedLab, searchQuery, selectedSort) {
        scope.launch {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Validade de Medicamentos") },
                navigationIcon = {
                    IconButton(onClick = { /* No back in drawer version */ }) {
                        Icon(Icons.Filled.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = "Ordenar")
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            listOf(
                                "Nome A → Z", "Nome Z → A",
                                "Código A → Z", "Código Z → A",
                                "Validade Mais Próxima", "Validade Mais Longe"
                            ).forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedSort = option
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.refreshData(context) }) {
                        Icon(Icons.Filled.Refresh, "Atualizar")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // Search + Lab filter
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar produto") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                var labExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = labExpanded, onExpandedChange = { labExpanded = !labExpanded }) {
                    OutlinedTextField(
                        value = selectedLab,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Laboratório") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = labExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = labExpanded, onDismissRequest = { labExpanded = false }) {
                        labs.forEach { lab ->
                            DropdownMenuItem(
                                text = { Text(lab) },
                                onClick = {
                                    selectedLab = lab
                                    labExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    meds.isEmpty() -> Text(
                        text = "Nenhum dado carregado. Tente atualizar.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    filteredAndSorted.isEmpty() -> Text(
                        text = "Nenhum item encontrado para o filtro.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    else -> {
                        LazyColumn(state = listState) {  // ← Use the remembered state
                            items(
                                items = filteredAndSorted,
                                key = { "${it.code}_${it.lab}_${it.expiryDate}" }
                            ) { med ->
                                MedicationRow(med, formatter)
                                Divider()
                            }
                        }
                    }
                }

                error?.let { errMsg ->
                    Snackbar(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        action = {
                            TextButton(onClick = { viewModel.refreshData(context) }) {
                                Text("Tentar Novamente")
                            }
                        }
                    ) {
                        Text(errMsg)
                    }
                }
            }
        }
    }
}