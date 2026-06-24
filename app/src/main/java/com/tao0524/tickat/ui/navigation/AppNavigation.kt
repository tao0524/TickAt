package com.tao0524.tickat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.tao0524.tickat.ui.screen.widgetlist.WidgetListScreen

sealed class Screen(val route: String) {
    object Onboarding     : Screen("onboarding")
    object TaskList       : Screen("task_list")
    object TaskEdit       : Screen("task_edit")
    object Expanded       : Screen("expanded")
    object Help           : Screen("help")
    object Settings       : Screen("settings")
    object WidgetSettings : Screen("widget_settings/{widgetId}") {
        fun createRoute(widgetId: Int) = "widget_settings/$widgetId"
    }
    object WidgetList : Screen("widget_list")
}

@Composable
fun AppNavigation(
    repository: TaskRepository,
    startWithOnboarding: Boolean = false,
    onOnboardingComplete: () -> Unit = {},
    openExpandedOnStart: Boolean = false,
    expandedTaskId: String? = null,
    onExpandedConsumed: () -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val factory = ViewModelFactory(repository, context)
    val startDestination = if (startWithOnboarding) Screen.Onboarding.route
    else Screen.TaskList.route

    LaunchedEffect(openExpandedOnStart) {
        if (openExpandedOnStart) {
            val route = if (expandedTaskId != null) {
                "${Screen.Expanded.route}?taskId=$expandedTaskId"
            } else {
                Screen.Expanded.route
            }
            navController.navigate(route)
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
                onOpenSettings = { navController.navigate(Screen.WidgetList.route) },
                onOpenWidgetList = { navController.navigate(Screen.WidgetList.route) }
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
        composable(
            route = "${Screen.Expanded.route}?taskId={taskId}",
            arguments = listOf(navArgument("taskId") { defaultValue = ""; type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")?.ifEmpty { null }
            ExpandedScreen(
                viewModel = viewModel(factory = factory),
                targetTaskId = taskId,
                onDismiss = { navController.popBackStack() },
                onOpenWidgetList = { navController.navigate(Screen.WidgetList.route) }
            )
        }
        composable(Screen.Help.route) {
            HelpScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.WidgetList.route) {
            WidgetListScreen(
                onBack = { navController.popBackStack() },
                onOpenWidgetSettings = { widgetId ->
                    navController.navigate(Screen.WidgetSettings.createRoute(widgetId))
                }
            )
        }
        composable(
            route = Screen.WidgetSettings.route,
            arguments = listOf(navArgument("widgetId") { type = NavType.IntType })
        ) { backStackEntry ->
            val extras = androidx.lifecycle.viewmodel.MutableCreationExtras(
                backStackEntry.defaultViewModelCreationExtras
            ).apply {
                set(SettingsViewModel.CONTEXT_KEY,    context)
                set(SettingsViewModel.REPOSITORY_KEY, repository)
            }
            SettingsScreen(
                viewModel = viewModel(
                    factory = SettingsViewModel.Factory,
                    extras  = extras
                ),
                onBack = { navController.popBackStack() }
            )
        }
    }
}