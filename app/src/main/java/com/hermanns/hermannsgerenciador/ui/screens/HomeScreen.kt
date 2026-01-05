package com.hermanns.hermannsgerenciador.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun HomeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Bem-vindo ao Gerenciador Hermanns",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Expiry Tracker Option
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            onClick = { navController.navigate("validade") }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Validade de Medicamentos", style = MaterialTheme.typography.titleMedium)
                Text("Acompanhe vencimentos de produtos", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Placeholder for Estoque
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            onClick = { navController.navigate("promoções") }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Promoções", style = MaterialTheme.typography.titleMedium)
                Text("Veja as promoções do dia", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Placeholder for Relatórios
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            onClick = { navController.navigate("relatorios") }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Relatórios", style = MaterialTheme.typography.titleMedium)
                Text("Gere relatórios de vendas e estoque", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}