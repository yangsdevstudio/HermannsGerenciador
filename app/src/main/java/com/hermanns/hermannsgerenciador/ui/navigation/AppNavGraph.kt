package com.hermanns.hermannsgerenciador.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hermanns.hermannsgerenciador.repo.SheetsApiRepository
import com.hermanns.hermannsgerenciador.ui.screens.HomeScreen
import com.hermanns.hermannsgerenciador.ui.screens.PromoScreen
import com.hermanns.hermannsgerenciador.ui.screens.ValidadeScreen
import com.hermanns.hermannsgerenciador.viewmodel.ValidadeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppWithDrawer() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val items = listOf(
        DrawerItem("home", "Home", Icons.Filled.Home),
        DrawerItem("validade", "Validade de Medicamentos", Icons.Filled.DateRange),
        DrawerItem("promocoes", "Promoções", Icons.Filled.ShoppingCart),
        DrawerItem("relatorios", "Relatórios", Icons.Filled.Report)
    )

    val currentRoute by navController.currentBackStackEntryAsState()
    val selectedItem = items.find { it.route == currentRoute?.destination?.route } ?: items[0]

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerHeader()
                items.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.title) },
                        selected = item.route == selectedItem.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(selectedItem.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                composable("home") {
                    HomeScreen(navController)
                }
                composable("validade") {
                    val context = LocalContext.current
                    val repository = SheetsApiRepository(context)
                    val viewModel: ValidadeViewModel = viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                return ValidadeViewModel(repository) as T
                            }
                        }
                    )
                    ValidadeScreen(viewModel = viewModel)
                }
                composable("promocoes") {
                    PromoScreen()
                }
                composable("relatorios") {
                    PlaceholderScreen("Relatórios em Desenvolvimento")
                }
            }
        }
    }
}

@Composable
private fun DrawerHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Medicamentos Hermanns",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Divider(modifier = Modifier.padding(top = 8.dp))
    }
}

data class DrawerItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun PlaceholderScreen(title: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(text = "Em breve!", style = MaterialTheme.typography.bodyLarge)
    }
}