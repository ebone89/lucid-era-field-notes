package com.lucidera.investigations.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lucidera.investigations.data.AppContainer
import com.lucidera.investigations.ui.screens.ArchiveLookupScreen
import com.lucidera.investigations.ui.screens.CaseDetailScreen
import com.lucidera.investigations.ui.screens.CasesScreen
import com.lucidera.investigations.ui.screens.DashboardScreen
import com.lucidera.investigations.ui.viewmodel.ArchiveViewModel
import com.lucidera.investigations.ui.viewmodel.ArchiveViewModelFactory
import com.lucidera.investigations.ui.viewmodel.CaseDetailViewModel
import com.lucidera.investigations.ui.viewmodel.CaseDetailViewModelFactory
import com.lucidera.investigations.ui.viewmodel.CasesViewModel
import com.lucidera.investigations.ui.viewmodel.CasesViewModelFactory
import com.lucidera.investigations.ui.viewmodel.DashboardViewModel
import com.lucidera.investigations.ui.viewmodel.DashboardViewModelFactory

private sealed class Destination(val route: String, val label: String) {
    data object Dashboard : Destination("dashboard", "Dashboard")
    data object Cases : Destination("cases", "Cases")
    data object Archive : Destination("archive", "Archive")
    data object CaseDetail : Destination("case/{caseId}", "Case Detail") {
        fun create(caseId: Long): String = "case/$caseId"
    }
}

@Composable
fun LucidEraApp(container: AppContainer) {
    val navController = rememberNavController()
    val destinations = listOf(Destination.Dashboard, Destination.Cases, Destination.Archive)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    fun navigateToTopLevel(route: String) {
        val popped = navController.popBackStack(route, false)
        if (!popped && currentDestination?.route != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    LaunchedEffect(Unit) {
        container.repository.seedIfEmpty()
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    val selected = when (destination) {
                        Destination.Cases -> {
                            currentDestination?.hierarchy?.any { it.route == destination.route } == true ||
                                currentDestination?.route == Destination.CaseDetail.route
                        }
                        else -> currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            when (destination) {
                                Destination.Dashboard -> navigateToTopLevel(Destination.Dashboard.route)
                                Destination.Cases -> navigateToTopLevel(Destination.Cases.route)
                                Destination.Archive -> navigateToTopLevel(Destination.Archive.route)
                                Destination.CaseDetail -> Unit
                            }
                        },
                        label = { Text(destination.label) },
                        icon = {
                            val imageVector = when (destination) {
                                Destination.Dashboard -> Icons.Outlined.Dashboard
                                Destination.Cases -> Icons.Outlined.Folder
                                Destination.Archive -> Icons.Outlined.Public
                                Destination.CaseDetail -> Icons.Outlined.Folder
                            }
                            Icon(imageVector = imageVector, contentDescription = destination.label)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.Dashboard.route) {
                val viewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModelFactory(container.repository)
                )
                DashboardScreen(
                    viewModel = viewModel,
                    onOpenCases = { navController.navigate(Destination.Cases.route) },
                    onOpenArchive = { navController.navigate(Destination.Archive.route) },
                    onCaseSelected = { navController.navigate(Destination.CaseDetail.create(it)) }
                )
            }
            composable(Destination.Cases.route) {
                val viewModel: CasesViewModel = viewModel(
                    factory = CasesViewModelFactory(container.repository)
                )
                CasesScreen(
                    viewModel = viewModel,
                    onCaseSelected = { navController.navigate(Destination.CaseDetail.create(it)) }
                )
            }
            composable(
                route = Destination.CaseDetail.route,
                arguments = listOf(navArgument("caseId") { type = NavType.LongType })
            ) { entry ->
                val caseId = entry.arguments?.getLong("caseId") ?: 0L
                val viewModel: CaseDetailViewModel = viewModel(
                    factory = CaseDetailViewModelFactory(
                        repository = container.repository,
                        caseId = caseId
                    )
                )
                CaseDetailScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenHome = { navigateToTopLevel(Destination.Dashboard.route) },
                    onOpenCases = { navigateToTopLevel(Destination.Cases.route) },
                    onDelete = {
                        navController.navigate(Destination.Cases.route) {
                            popUpTo(Destination.Cases.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Destination.Archive.route) {
                val viewModel: ArchiveViewModel = viewModel(
                    factory = ArchiveViewModelFactory(container.repository)
                )
                ArchiveLookupScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
