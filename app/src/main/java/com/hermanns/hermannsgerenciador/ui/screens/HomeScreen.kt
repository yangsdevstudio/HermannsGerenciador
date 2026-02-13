package com.hermanns.hermannsgerenciador.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hermanns.hermannsgerenciador.salesman.ui.SalesmanLoginActivity

@Composable
fun HomeScreen(navController: NavHostController, onShowPromo: () -> Unit) {
    val context = LocalContext.current

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

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            onClick = onShowPromo
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Promoções", style = MaterialTheme.typography.titleMedium)
                Text("Veja as promoções do dia", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            onClick = {
                context.startActivity(Intent(context, SalesmanLoginActivity::class.java))
            }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Relatórios", style = MaterialTheme.typography.titleMedium)
                Text("Relatórios de visitas e gastos", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}