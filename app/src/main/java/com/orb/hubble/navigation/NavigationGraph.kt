package com.orb.hubble.navigation

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.orb.hubble.data.HomeScreenViewModel
import com.orb.hubble.data.PostIsFrom
import com.orb.hubble.ui.screens.CreatePostScreen
import com.orb.hubble.ui.screens.ExploreScreen
import com.orb.hubble.ui.screens.HomeScreen
import com.orb.hubble.ui.screens.PickMediaScreen
import com.orb.hubble.ui.screens.PostViewerScreen
import com.orb.hubble.ui.screens.ProfileScreen
import com.orb.hubble.ui.screens.ReplyLogScreen
import com.orb.hubble.ui.screens.ViewStatusScreen
import com.orb.hubble.ui.theme.Black3
import com.orb.hubble.ui.theme.Black4
import com.orb.hubble.ui.theme.Blue2
import com.orb.hubble.ui.theme.White0
import com.orb.hubble.ui.theme.White1
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun MyNavigationHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
//    val currentDestination = navBackStackEntry?.destination

//    val bottomBarRoutes = listOf(
//        Dest.ExploreScreen::class.qualifiedName,
//        Dest.HomeScreen::class.qualifiedName,
//        Dest.ReplyLogScreen::class.qualifiedName,
//        Dest.ProfileScreen::class.qualifiedName
//    )
//
//    val showBottomBar = currentDestination?.route in bottomBarRoutes

    val pagerState = rememberPagerState(1) { bottomNavIcons.size }

    Scaffold(
        modifier = modifier
            .background(Black3),
//        bottomBar = {
////            if (showBottomBar) {
////                MyBottomBar(
////                    currentRoute = currentDestination?.route!!,
////                    navController = navController
////                )
////            }
//            if (currentDestination?.route == Dest.MainTabsScreen::class.qualifiedName) {
//                BottomBar(
//                    pagerState = pagerState,
//                    screens = bottomNavIcons
//                )
//            }
//        },
        containerColor = Black3,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->

        NavHost(
            modifier = Modifier
                .padding(innerPadding),
            navController = navController,
            startDestination = Dest.MainTabsScreen,
            enterTransition = {
                scaleIn(
                    initialScale = .9f
                ) + fadeIn()
            },
            exitTransition = {
                scaleOut(
                    targetScale = 1.1f
                ) + fadeOut()
            },
            popEnterTransition = {
                scaleIn(initialScale = .9f) + fadeIn()
            },
            popExitTransition = {
                scaleOut(targetScale = .9f) + fadeOut()
            }
        ) {

            composable<Dest.MainTabsScreen> {
                BottomNavGraph(pagerState = pagerState, navController = navController)
            }

            homeGraph(navController)
        }
    }

}

@UnstableApi
@Composable
fun BottomNavGraph(
    pagerState: PagerState,
    navController: NavController
) {

    val coroutineScope = rememberCoroutineScope()

    BackHandler(
        enabled = pagerState.currentPage != 1
    ) {
        coroutineScope.launch {
            pagerState.scrollToPage(1)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> ExploreScreen()
                1 -> HomeScreen(
                    userId = "3",
                    navToViewStatusScreen = { pageId, postIsFrom ->
                        navController.navigate(
                            Dest.ViewStatusScreen(
                                pageId = pageId,
                                isFrom = postIsFrom
                            )
                        )
                    },
                    navToAddPostScreen = {
                        navController.navigate(Dest.PickMediaScreen)
                    },
                    navToReplyLogScreenTemporary = {
                        navController.navigate(Dest.ReplyLogScreen)
                    }
                )

                2 -> ReplyLogScreen(
                    onMessageClicked = { postId, messageId ->
                        navController.navigate(
                            Dest.ViewStatusScreen(
                                postId = postId,
                                messageId = messageId,
                                isFrom = PostIsFrom.SINGLE_POST
                            )
                        )
                    }
                )

                3 -> ProfileScreen()
            }
        }
        BottomBar(
            pagerState = pagerState,
            screens = bottomNavIcons
        )
    }
}

@OptIn(UnstableApi::class)
fun NavGraphBuilder.homeGraph(
    navController: NavController
) {

    composable<Dest.ViewStatusScreen> {
        // Get the SAME ViewModel instance scoped to the navigation graph
//            val parentEntry = remember(it) {
//                navController.getBackStackEntry(SubGraph.Home)
//            }
//            val viewModel: HomeScreenViewModel = hiltViewModel(parentEntry)

        val viewStatusScreen: Dest.ViewStatusScreen = it.toRoute<Dest.ViewStatusScreen>()
        ViewStatusScreen(
            pageId = viewStatusScreen.pageId,
            messageId = viewStatusScreen.messageId,
            onGoBack = {
                navController.navigateUp()
            }
        )
    }

    composable<Dest.PostViewerScreen> { backStackEntry ->
        val route: Dest.PostViewerScreen = backStackEntry.toRoute()

        PostViewerScreen(
            initialPageId = route.pageId,
            onGoBack = { navController.navigateUp() }
        )
    }

    composable<Dest.PickMediaScreen> {
        PickMediaScreen(
            onMediaSelected = { uriString ->
                navController.navigate(Dest.CreatePostScreen(mediaUri = uriString))
            },
            onNavigate = {
                navController.navigateUp()
            }
        )
    }



    composable<Dest.CreatePostScreen> {
        val createPostScreen: Dest.CreatePostScreen = it.toRoute<Dest.CreatePostScreen>()
        CreatePostScreen(
            mediaUri = createPostScreen.mediaUri
        )
    }
}

@Composable
fun MyBottomBar(
    modifier: Modifier = Modifier,
    currentRoute: String,
    navController: NavController
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Black3)
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = Black4
        )
        NavigationBar(
            modifier = Modifier.windowInsetsBottomHeight(
                WindowInsets.navigationBars.add(WindowInsets(bottom = 65.dp))
            ),
            containerColor = Black3
        ) {
            bottomNavIcons.forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = currentRoute == item.dest::class.qualifiedName,
                    onClick = {
                        if (item.dest::class.qualifiedName != currentRoute) {
                            navController.navigate(item.dest::class.qualifiedName!!) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        //navController.navigateToBottomBarDest(item.dest)
                    },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (item.hasNews) {
                                    Badge(containerColor = Blue2)
                                }
                            }
                        ) {
                            Image(
                                modifier = modifier
                                    .size(item.size.dp),
                                painter = painterResource(item.icon),
                                contentDescription = "${item.name} icon on bottom bar",
                                colorFilter = ColorFilter.tint(White1)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = item.name,
                            color = White1,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Black4//Blue2.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

@Composable
fun BottomBar(
    pagerState: PagerState,
    screens: List<BottomNavItem>,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Black3)
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = Black4
        )
        NavigationBar(
            modifier = Modifier.windowInsetsBottomHeight(
                WindowInsets.navigationBars.add(WindowInsets(bottom = 65.dp))
            ),
            containerColor = Black3
        ) {
            screens.forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            //pagerState.animateScrollToPage(index)
                            pagerState.scrollToPage(index)
                        }
                    },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (item.hasNews) {
                                    Badge(containerColor = Blue2)
                                }
                            }
                        ) {
                            Image(
                                modifier = modifier
                                    .size(item.size.dp),
                                painter = painterResource(item.icon),
                                contentDescription = "${item.name} icon on bottom bar",
                                colorFilter = ColorFilter.tint(White0)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = item.name,
                            color = White1,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Black4//Blue2.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

fun NavController.navigateToBottomBarDest(dest: Dest) {
    val currentRoute = currentBackStackEntry?.destination?.route

    val destRoute = when (dest) {
        Dest.ExploreScreen -> Dest.ExploreScreen::class.qualifiedName
        Dest.HomeScreen -> Dest.HomeScreen::class.qualifiedName
        Dest.ReplyLogScreen -> Dest.ReplyLogScreen::class.qualifiedName
        Dest.ProfileScreen -> Dest.ProfileScreen::class.qualifiedName
        else -> throw IllegalArgumentException("Invalid bottom bar destination")
    }
    if (currentRoute != destRoute) {
        navigate(dest) {
            launchSingleTop = true
            restoreState = true
            popUpTo(graph.startDestinationId) {
                saveState = true
            }
        }
    }
}