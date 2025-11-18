package com.orb.hubble.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.orb.hubble.data.HomeScreenViewModel
import com.orb.hubble.ui.screens.HomeScreen
import com.orb.hubble.ui.screens.ViewStatusScreen

@Composable
fun MyNavigationHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = SubGraph.Home
    ) {
        homeGraph(navController)
    }
}

fun NavGraphBuilder.homeGraph(
    navController: NavController
) {
    navigation<SubGraph.Home>(startDestination = Dest.HomeScreen) {
        composable<Dest.HomeScreen> {
            // Get the ViewModel scoped to the navigation graph
            val parentEntry = remember(it) {
                navController.getBackStackEntry(SubGraph.Home)
            }
            val viewModel: HomeScreenViewModel = hiltViewModel(parentEntry)

            HomeScreen(
                userId = 3,
                viewModel = viewModel,
                navToViewStatusScreen = { userId ->
                    navController.navigate(Dest.ViewStatusScreen(userId = userId))
                }
            )
        }

        composable<Dest.ViewStatusScreen> {
            // Get the SAME ViewModel instance scoped to the navigation graph
            val parentEntry = remember(it) {
                navController.getBackStackEntry(SubGraph.Home)
            }
            val viewModel: HomeScreenViewModel = hiltViewModel(parentEntry)

            val viewStatusScreen: Dest.ViewStatusScreen = it.toRoute<Dest.ViewStatusScreen>()
            ViewStatusScreen(
                userId = viewStatusScreen.userId,
                viewModel = viewModel
            )
        }
    }
}