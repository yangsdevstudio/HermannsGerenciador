package com.hermanns.hermannsgerenciador.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.hermanns.hermannsgerenciador.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File

private val httpClient = OkHttpClient()

private const val REPO_API_URL = "https://api.github.com/repos/yangsdevstudio/HermannsGerenciador/contents/"
private const val RAW_BASE_URL = "https://raw.githubusercontent.com/yangsdevstudio/HermannsGerenciador/refs/heads/master/"

@Composable
fun PromoPopUp(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSharing by remember { mutableStateOf(false) }

    var promoUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableIntStateOf(0) }

    // Fetch promo image list from GitHub API
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(REPO_API_URL)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(body)
                    val urls = mutableListOf<String>()
                    val numberedFiles = mutableListOf<Triple<Int, String, String>>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val name = obj.getString("name")
                        val sha = obj.optString("sha", "")
                        if (name.equals("promo.png", ignoreCase = true)) {
                            // promo.png goes first
                            urls.add(0, "${RAW_BASE_URL}$name?sha=$sha")
                        } else {
                            val match = Regex("""^promo(\d+)\.png$""", RegexOption.IGNORE_CASE).matchEntire(name)
                            if (match != null) {
                                val number = match.groupValues[1].toInt()
                                numberedFiles.add(Triple(number, name, sha))
                            }
                        }
                    }
                    urls.addAll(numberedFiles.sortedBy { it.first }.map { (_, name, sha) ->
                        "${RAW_BASE_URL}$name?sha=$sha"
                    })
                    promoUrls = urls.ifEmpty {
                        val cacheBust = System.currentTimeMillis()
                        listOf("${RAW_BASE_URL}promo.png?t=$cacheBust")
                    }
                } else {
                    // API failed, fallback to legacy single image
                    val cacheBust = System.currentTimeMillis()
                    promoUrls = listOf("${RAW_BASE_URL}promo.png?t=$cacheBust")
                }
            } catch (e: Exception) {
                // Network error, fallback
                val cacheBust = System.currentTimeMillis()
                promoUrls = listOf("${RAW_BASE_URL}promo.png?t=$cacheBust")
                loadError = e.message
            } finally {
                isLoading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (!isLoading && promoUrls.size > 1) "Promoções Atuais (${promoUrls.size})"
                else "Promoções Atuais"
            )
        },
        text = {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                isSharing -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                promoUrls.isNotEmpty() -> {
                    PromoImagePager(promoUrls, onPageChanged = { currentPage = it })
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text("Fechar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        if (!isSharing && promoUrls.isNotEmpty()) {
                            scope.launch {
                                isSharing = true
                                try {
                                    val url = promoUrls[currentPage]
                                    val request = Request.Builder().url(url).build()
                                    val response = withContext(Dispatchers.IO) {
                                        httpClient.newCall(request).execute()
                                    }
                                    if (response.isSuccessful) {
                                        val bytes = response.body?.bytes()
                                        if (bytes != null) {
                                            val file = withContext(Dispatchers.IO) {
                                                val tempFile = File(context.cacheDir, "promo.png")
                                                tempFile.writeBytes(bytes)
                                                tempFile
                                            }
                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "image/png"
                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartilhar Promoção"))
                                            file.deleteOnExit()
                                        } else {
                                            context.toast("Falha ao baixar a imagem: dados vazios")
                                        }
                                    } else {
                                        context.toast("Falha ao baixar a imagem: código ${response.code}")
                                    }
                                } catch (e: Exception) {
                                    context.toast("Erro ao compartilhar: ${e.message}")
                                } finally {
                                    isSharing = false
                                }
                            }
                        }
                    },
                    enabled = !isSharing && !isLoading
                ) {
                    Text("Compartilhar")
                }
            }
        }
    )
}

@Composable
private fun PromoImagePager(imageUrls: List<String>, onPageChanged: (Int) -> Unit) {
    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentIndex) {
        onPageChanged(currentIndex)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        ) {
            ZoomablePromoImage(imageUrl = imageUrls[currentIndex], context = context)

            if (imageUrls.size > 1) {
                // Left arrow
                if (currentIndex > 0) {
                    IconButton(
                        onClick = { currentIndex-- },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Anterior",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Right arrow
                if (currentIndex < imageUrls.size - 1) {
                    IconButton(
                        onClick = { currentIndex++ },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Próxima",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Page indicator dots
        if (imageUrls.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                imageUrls.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (currentIndex == index) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (currentIndex == index)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomablePromoImage(imageUrl: String, context: android.content.Context) {
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val containerWidthPx = with(density) { 300.dp.toPx() }
    val imageHeightPx = with(density) { 400.dp.toPx() }

    Image(
        painter = rememberAsyncImagePainter(
            ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build()
        ),
        contentDescription = "Promo Image",
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    val maxX = ((containerWidthPx * scale - containerWidthPx) / 2f).coerceAtLeast(0f)
                    val maxY = ((imageHeightPx * scale - imageHeightPx) / 2f).coerceAtLeast(0f)
                    offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                    offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                    if (scale <= 1.01f) {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentScale = ContentScale.Fit
    )
}
