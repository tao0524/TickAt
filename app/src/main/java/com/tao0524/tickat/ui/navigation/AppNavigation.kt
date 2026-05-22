package com.tao0524.tickat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tao0524.tickat.data.repository.TaskRepository
import com.tao0524.tickat.ui.ViewModelFactory
import com.tao0524.tickat.ui.screen.expanded.ExpandedScreen
import androidx.compose.ui.platform.LocalContext
import com.tao0524.tickat.ui.screen.help.HelpScreen
import com.tao0524.tickat.ui.screen.settings.SettingsScreen
import com.tao0524.tickat.ui.screen.settings.SettingsViewModel
import com.tao0524.tickat.ui.screen.onboarding.OnboardingScreen
import com.tao0524.tickat.ui.screen.taskedit.TaskEditScreen
import com.tao0524.tickat.ui.screen.tasklist.TaskListScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object TaskList   : Screen("task_list")
    object TaskEdit   : Screen("task_edit")
    object Expanded   : Screen("expanded")
    object Help       : Screen("help")
    object Settings   : Screen("settings")
}

@Composable
fun AppNavigation(
    repository: TaskRepository,
    startWithOnboarding: Boolean = false,
    onOnboardingComplete: () -> Unit = {},
    openExpandedOnStart: Boolean = false,
    onExpandedConsumed: () -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val factory = ViewModelFactory(repository, context)
    val startDestination = if (startWithOnboarding) Screen.Onboarding.route
    else Screen.TaskList.route

    LaunchedEffect(openExpandedOnStart) {
        if (openExpandedOnStart) {
            navController.navigate(Screen.Expanded.route)
            onExpandedConsumed()
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    onOnboardingComplete()
                    navController.navigate(Screen.TaskList.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.TaskList.route) {
            TaskListScreen(
                viewModel      = viewModel(factory = factory),
                onAddTask      = { navController.navigate(Screen.TaskEdit.route) },
                onEditTask     = { taskId ->
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("taskId", taskId)
                    navController.navigate(Screen.TaskEdit.route)
                },
                onOpenHelp     = { navController.navigate(Screen.Help.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.TaskEdit.route) {
            val taskId: String? = rememberSaveable {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>("taskId") ?: ""
            }.ifEmpty { null }
            TaskEditScreen(
                taskId    = taskId,
                viewModel = viewModel(factory = factory),
                onDone    = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.remove<String>("taskId")
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Expanded.route) {
            ExpandedScreen(
                viewModel = viewModel(factory = factory),
                onDismiss = { navController.popBackStack() }
            )
        }
        composable(Screen.Help.route) {
            HelpScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel(factory = factory),
                onBack    = { navController.popBackStack() }
            )
        }
    }
}