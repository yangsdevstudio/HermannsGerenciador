// kotlin
package com.hermanns.hermannsgerenciador.ui.navigation



import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home

import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Menu
import android.content.Intent
import com.hermanns.hermannsgerenciador.salesman.ui.SalesmanLoginActivity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hermanns.hermannsgerenciador.repo.SheetsApiRepository
import com.hermanns.hermannsgerenciador.ui.screens.HomeScreen
import com.hermanns.hermannsgerenciador.ui.screens.PromoPopUp
import com.hermanns.hermannsgerenciador.ui.screens.ValidadeScreen

import com.hermanns.hermannsgerenciador.viewmodel.ValidadeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppWithDrawer() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination?.route ?: "home"
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showPromoDialog by remember { mutableStateOf(false) }  // Hoisted state for promo pop-up
    val context = LocalContext.current  // For launching external app

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerHeader()
                val items = listOf(
                    DrawerItem("home", "Home", Icons.Default.Home),
                    DrawerItem("validade", "Validade", Icons.Default.DateRange),
                    DrawerItem("promocoes", "Promoções", Icons.Default.ShoppingCart),
                    DrawerItem("vendedor", "Relatórios", Icons.Default.Person)
                )
                items.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentDestination == item.route,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            when (item.route) {
                                "promocoes" -> {
                                    showPromoDialog = true
                                }
                                "vendedor" -> {
                                    context.startActivity(Intent(context, SalesmanLoginActivity::class.java))
                                }
                                else -> {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Gerenciador Hermanns") },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("home") {
                    HomeScreen(
                        navController = navController,
                        onShowPromo = { showPromoDialog = true }  // Pass callback to trigger pop-up from HomeScreen card
                    )
                }
                composable("validade") {
                    val repository = SheetsApiRepository(LocalContext.current)
                    val viewModel: ValidadeViewModel = viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                return ValidadeViewModel(repository) as T
                            }
                        }
                    )
                    ValidadeScreen(viewModel = viewModel, navController = navController)
                }
            }
        }
    }

    // Global pop-up trigger: Shows over any current page
    if (showPromoDialog) {
        PromoPopUp(onDismiss = { showPromoDialog = false })
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
            text = "Hermanns",
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