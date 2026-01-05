package com.hermanns.hermannsgerenciador.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.hermanns.hermannsgerenciador.ui.theme.MaterialTheme

@Composable
fun PromoScreen() {
    var showDialog by remember { mutableStateOf(true) }  // Auto-show pop-up on open

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Promoções Atuais") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data("https://drive.google.com/uc?id=YOUR_FILE_ID")  // ← Replace with your Drive direct URL
                            .crossfade(true)
                            .error(R.drawable.ic_error)  // Add 24dp error icon in drawable if needed
                            .placeholder(R.drawable.ic_loading)  // Add placeholder icon
                            .build()
                    )
                    Image(
                        painter = painter,
                        contentDescription = "Imagem de Promoções",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),  // Adjustable height
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Veja as promoções atuais da Hermanns!")
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    } else {
        // Optional: Screen content if not just pop-up (e.g., "No promoções" or back)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Promoções fechadas")
        }
    }
}