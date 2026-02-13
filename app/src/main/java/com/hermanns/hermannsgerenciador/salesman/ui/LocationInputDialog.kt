package com.hermanns.hermannsgerenciador.salesman.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LocationInputDialog(
    onDismiss: () -> Unit,
    onSave: (local: String, motivo: String, km: String, comentario: String) -> Unit
) {
    var local by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    var km by remember { mutableStateOf("") }
    var comentario by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Visita") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = local,
                    onValueChange = { local = it; showError = false },
                    label = { Text("Local *") },
                    singleLine = true,
                    isError = showError && local.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it; showError = false },
                    label = { Text("Motivo *") },
                    singleLine = true,
                    isError = showError && motivo.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = km,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() }) km = newValue
                    },
                    label = { Text("KM") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = comentario,
                    onValueChange = { comentario = it },
                    label = { Text("Comentário") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Preencha pelo menos local e motivo",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (local.isBlank() || motivo.isBlank()) {
                    showError = true
                } else {
                    onSave(local.trim(), motivo.trim(), km.trim(), comentario.trim())
                    onDismiss()
                }
            }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
