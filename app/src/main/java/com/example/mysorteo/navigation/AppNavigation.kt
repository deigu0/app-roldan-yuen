package com.example.mysorteo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mysorteo.data.DrawRepository
import com.example.mysorteo.ui.auth.AuthViewModel
import com.example.mysorteo.ui.auth.LoginScreen
import com.example.mysorteo.ui.auth.RegisterScreen
import com.example.mysorteo.ui.draw.DrawListScreen
import com.example.mysorteo.ui.draw.DrawTrackingScreen
import com.example.mysorteo.ui.draw.DrawViewModel
import com.example.mysorteo.ui.draw.EntryRegistrationScreen

object Screen {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val DRAW_LIST = "draw_list"
    const val ENTRY_REGISTRATION = "entry_registration/{drawId}"
    const val DRAW_TRACKING = "draw_tracking/{drawId}"

    fun entryRegistration(drawId: String) = "entry_registration/$drawId"
    fun drawTracking(drawId: String) = "draw_tracking/$drawId"
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel = viewModel(),
    drawViewModel: DrawViewModel = viewModel()
) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsState()
    val currentUser = authState.currentUser

    val startDestination = if (currentUser != null) Screen.DRAW_LIST else Screen.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    authViewModel.clearError()
                    navController.navigate(Screen.REGISTER)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.DRAW_LIST) {
                        popUpTo(Screen.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    authViewModel.clearError()
                    navController.navigate(Screen.LOGIN) {
                        popUpTo(Screen.REGISTER) { inclusive = true }
                    }
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.DRAW_LIST) {
                        popUpTo(Screen.REGISTER) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.DRAW_LIST) {
            val user = authViewModel.uiState.collectAsState().value.currentUser
            val userEmail = user?.email ?: "usuario@ejemplo.com"

            DrawListScreen(
                userEmail = userEmail,
                drawViewModel = drawViewModel,
                onSelectDrawForRegistration = { drawId ->
                    navController.navigate(Screen.entryRegistration(drawId))
                },
                onSelectDrawForTracking = { drawId ->
                    navController.navigate(Screen.drawTracking(drawId))
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.ENTRY_REGISTRATION,
            arguments = listOf(navArgument("drawId") { type = NavType.StringType })
        ) { backStackEntry ->
            val drawId = backStackEntry.arguments?.getString("drawId") ?: DrawRepository.DEFAULT_DRAW_ID
            val user = authViewModel.uiState.collectAsState().value.currentUser
            val userId = user?.uid ?: ""
            val userEmail = user?.email ?: ""

            EntryRegistrationScreen(
                drawId = drawId,
                userId = userId,
                userEmail = userEmail,
                drawViewModel = drawViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToTracking = { dId ->
                    navController.navigate(Screen.drawTracking(dId))
                }
            )
        }

        composable(
            route = Screen.DRAW_TRACKING,
            arguments = listOf(navArgument("drawId") { type = NavType.StringType })
        ) { backStackEntry ->
            val drawId = backStackEntry.arguments?.getString("drawId") ?: DrawRepository.DEFAULT_DRAW_ID
            val user = authViewModel.uiState.collectAsState().value.currentUser
            val userId = user?.uid ?: ""
            val userEmail = user?.email ?: ""

            DrawTrackingScreen(
                drawId = drawId,
                userId = userId,
                userEmail = userEmail,
                drawViewModel = drawViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
