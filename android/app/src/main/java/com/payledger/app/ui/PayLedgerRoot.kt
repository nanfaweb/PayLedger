package com.payledger.app.ui

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.payledger.app.R
import com.payledger.app.data.AppContainer
import com.payledger.app.notification.NotificationIngestBus
import com.payledger.app.ui.navigation.Routes
import com.payledger.app.ui.screens.ActivateScreen
import com.payledger.app.ui.screens.AddTransactionScreen
import com.payledger.app.ui.screens.HomeScreen
import com.payledger.app.ui.screens.LoginScreen
import com.payledger.app.ui.screens.RegisterScreen
import com.payledger.app.ui.screens.ReportsScreen
import com.payledger.app.ui.screens.ResetPasswordScreen
import com.payledger.app.ui.screens.SettingsScreen
import com.payledger.app.ui.screens.TransactionDetailScreen
import com.payledger.app.ui.screens.TransactionsScreen
import com.payledger.app.ui.screens.WelcomeScreen
import com.payledger.app.ui.screens.isNotificationAccessEnabled
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun PayLedgerRoot(container: AppContainer) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var start by remember {
        mutableStateOf(
            when {
                !container.tokenStore.welcomeSeen && !container.tokenStore.isLoggedIn() -> Routes.WELCOME
                !container.tokenStore.isLoggedIn() -> Routes.LOGIN
                else -> Routes.ACTIVATE
            }
        )
    }
    var licenseChecked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        applyLanguage(context, container.tokenStore.language)
        if (container.tokenStore.isLoggedIn()) {
            try {
                val status = container.licenseRepository.status()
                start = when {
                    status.active -> Routes.MAIN
                    status.status == "expired" -> Routes.RENEW
                    else -> Routes.ACTIVATE
                }
            } catch (_: Exception) {
                start = Routes.ACTIVATE
            }
        }
        licenseChecked = true
    }

    // Upload parsed notifications when logged in + licensed
    LaunchedEffect(Unit) {
        NotificationIngestBus.events.collectLatest { request ->
            if (!container.tokenStore.isLoggedIn()) return@collectLatest
            try {
                container.transactionRepository.create(request)
                NotificationIngestBus.publishStatus("saved")
            } catch (_: Exception) {
                NotificationIngestBus.publishStatus("save_failed")
            }
        }
    }

    if (!licenseChecked && container.tokenStore.isLoggedIn()) {
        Text("…")
        return
    }

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onRegister = {
                    container.tokenStore.welcomeSeen = true
                    navController.navigate(Routes.REGISTER)
                },
                onLogin = {
                    container.tokenStore.welcomeSeen = true
                    navController.navigate(Routes.LOGIN)
                },
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                authRepository = container.authRepository,
                onSuccess = {
                    scope.launch {
                        routeAfterAuth(container, navController)
                    }
                },
                onRegister = { navController.navigate(Routes.REGISTER) },
                onForgot = { navController.navigate(Routes.RESET) },
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                authRepository = container.authRepository,
                onSuccess = {
                    scope.launch { routeAfterAuth(container, navController) }
                },
                onLogin = { navController.navigate(Routes.LOGIN) },
            )
        }
        composable(Routes.RESET) {
            ResetPasswordScreen(container.authRepository) {
                navController.navigate(Routes.LOGIN) { popUpTo(Routes.WELCOME) }
            }
        }
        composable(Routes.ACTIVATE) {
            ActivateScreen(container.licenseRepository, renew = false) {
                navController.navigate(Routes.MAIN) {
                    popUpTo(Routes.WELCOME) { inclusive = true }
                }
            }
        }
        composable(Routes.RENEW) {
            ActivateScreen(container.licenseRepository, renew = true) {
                navController.navigate(Routes.MAIN) {
                    popUpTo(Routes.WELCOME) { inclusive = true }
                }
            }
        }
        composable(Routes.MAIN) {
            MainShell(container = container, onLoggedOut = {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            }, onRenew = {
                navController.navigate(Routes.RENEW)
            })
        }
    }
}

@Composable
private fun MainShell(
    container: AppContainer,
    onLoggedOut: () -> Unit,
    onRenew: () -> Unit,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val context = LocalContext.current
    val monitoring = isNotificationAccessEnabled(context)
    val showBottom = route in listOf(Routes.HOME, Routes.TRANSACTIONS, Routes.REPORTS, Routes.SETTINGS)

    Scaffold(
        bottomBar = {
            if (showBottom) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == Routes.HOME,
                        onClick = { navController.navigate(Routes.HOME) { popUpTo(navController.graph.findStartDestination().id); launchSingleTop = true } },
                        icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_home)) },
                    )
                    NavigationBarItem(
                        selected = route == Routes.TRANSACTIONS,
                        onClick = { navController.navigate(Routes.TRANSACTIONS) { popUpTo(navController.graph.findStartDestination().id); launchSingleTop = true } },
                        icon = { Icon(Icons.Outlined.List, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_transactions)) },
                    )
                    NavigationBarItem(
                        selected = route == Routes.REPORTS,
                        onClick = { navController.navigate(Routes.REPORTS) { popUpTo(navController.graph.findStartDestination().id); launchSingleTop = true } },
                        icon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_reports)) },
                    )
                    NavigationBarItem(
                        selected = route == Routes.SETTINGS,
                        onClick = { navController.navigate(Routes.SETTINGS) { popUpTo(navController.graph.findStartDestination().id); launchSingleTop = true } },
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_settings)) },
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    transactionRepository = container.transactionRepository,
                    monitoringEnabled = monitoring,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenTransaction = { navController.navigate(Routes.transactionDetail(it)) },
                    onAddTransaction = { navController.navigate(Routes.ADD_TRANSACTION) },
                )
            }
            composable(Routes.TRANSACTIONS) {
                TransactionsScreen(container.transactionRepository) {
                    navController.navigate(Routes.transactionDetail(it))
                }
            }
            composable(
                Routes.TRANSACTION_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id") ?: return@composable
                TransactionDetailScreen(
                    id = id,
                    transactionRepository = container.transactionRepository,
                    onDeleted = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ADD_TRANSACTION) {
                AddTransactionScreen(
                    transactionRepository = container.transactionRepository,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.REPORTS) {
                ReportsScreen(container.transactionRepository, container.tokenStore)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    container = container,
                    onLoggedOut = onLoggedOut,
                    onLanguageChanged = { lang ->
                        applyLanguage(context, lang)
                    },
                )
            }
        }
    }
}

private suspend fun routeAfterAuth(
    container: AppContainer,
    navController: androidx.navigation.NavHostController,
) {
    try {
        val status = container.licenseRepository.status()
        val dest = when {
            status.active -> Routes.MAIN
            status.status == "expired" -> Routes.RENEW
            else -> Routes.ACTIVATE
        }
        navController.navigate(dest) {
            popUpTo(Routes.WELCOME) { inclusive = true }
        }
    } catch (_: Exception) {
        navController.navigate(Routes.ACTIVATE) {
            popUpTo(Routes.WELCOME) { inclusive = true }
        }
    }
}

fun applyLanguage(context: Context, language: String) {
    val tag = if (language == "ur") "ur" else "en"
    if (Build.VERSION.SDK_INT >= 33) {
        context.getSystemService(LocaleManager::class.java)
            ?.applicationLocales = LocaleList.forLanguageTags(tag)
    } else {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }
}
