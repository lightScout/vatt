package org.lightscout.vatt.presentation.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.koin.compose.koinInject
import org.lightscout.vatt.domain.repository.AuthRepository
import org.lightscout.vatt.presentation.booking.ClassDetailScreen
import org.lightscout.vatt.presentation.home.HomeScreen
import org.lightscout.vatt.presentation.login.LoginScreen
import org.lightscout.vatt.presentation.timetable.TimetableScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(authRepository: AuthRepository = koinInject()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val onLogin = currentDestination?.hasRoute(LoginRoute::class) == true
    val showClassDetail = currentDestination?.hasRoute(ClassDetailRoute::class) == true

    // If a token refresh fails, the session is over — route back to login, clearing the stack.
    val sessionExpired by authRepository.sessionExpired.collectAsStateWithLifecycle(initialValue = null)
    androidx.compose.runtime.LaunchedEffect(sessionExpired) {
        if (sessionExpired != null) {
            navController.navigate(LoginRoute) { popUpTo(0) { inclusive = true } }
        }
    }

    Scaffold(
        topBar = {
            if (!onLogin) {
                TopAppBar(
                    title = { Text(titleFor(currentDestination)) },
                    navigationIcon = {
                        if (showClassDetail) {
                            TextButton(onClick = { navController.popBackStack() }) { Text("Back") }
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (!onLogin && !showClassDetail) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute(HomeRoute::class) == true,
                        onClick = { navController.navigateSingleTop(HomeRoute) },
                        icon = {},
                        label = { Text("Home") },
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute(TimetableRoute::class) == true,
                        onClick = { navController.navigateSingleTop(TimetableRoute) },
                        icon = {},
                        label = { Text("Classes") },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LoginRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<LoginRoute> {
                LoginScreen(onLoggedIn = {
                    navController.navigate(HomeRoute) { popUpTo(LoginRoute) { inclusive = true } }
                })
            }
            composable<HomeRoute> {
                HomeScreen(onOpenClass = { classId -> navController.navigate(ClassDetailRoute(classId)) })
            }
            composable<TimetableRoute> {
                TimetableScreen(onOpenClass = { classId -> navController.navigate(ClassDetailRoute(classId)) })
            }
            composable<ClassDetailRoute> { entry ->
                val route = entry.toRoute<ClassDetailRoute>()
                ClassDetailScreen(
                    classId = route.classId,
                    onBookingCancelled = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun titleFor(destination: androidx.navigation.NavDestination?): String = when {
    destination?.hasRoute(HomeRoute::class) == true -> "Home"
    destination?.hasRoute(TimetableRoute::class) == true -> "Classes This Week"
    destination?.hasRoute(ClassDetailRoute::class) == true -> "Class Details"
    else -> "Virgin Active"
}

private fun <T : Any> androidx.navigation.NavController.navigateSingleTop(route: T) {
    navigate(route) {
        launchSingleTop = true
        popUpTo(HomeRoute) { saveState = true }
        restoreState = true
    }
}
