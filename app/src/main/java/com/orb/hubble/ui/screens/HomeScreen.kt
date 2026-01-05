package com.orb.hubble.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orb.hubble.R
import com.orb.hubble.data.HomeScreenViewModel
import com.orb.hubble.data.Post
import com.orb.hubble.data.PostFilterTab
import com.orb.hubble.data.User
import com.orb.hubble.data.mockPosts
//import com.orb.hubble.data.postsList10
//import com.orb.hubble.data.postsList4
import com.orb.hubble.ui.components.StatusItem
import com.orb.hubble.ui.theme.Black1
import com.orb.hubble.ui.theme.Blue3
import com.orb.hubble.ui.theme.Grey3
import com.orb.hubble.ui.theme.Typography
import com.orb.hubble.ui.theme.White1
import com.orb.hubble.ui.theme.White2
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    userId: Int = 3,
    viewModel: HomeScreenViewModel = hiltViewModel(),
    navToViewStatusScreen: (Int) -> Unit,
    navToAddPostScreen: () -> Unit
) {
    val uiState by viewModel.state.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val filteredPosts by viewModel.filteredPosts.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = selectedTab.ordinal,
        pageCount = { PostFilterTab.entries.size }
    )

    val bottomPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value.toInt()
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().value.toInt()

//    LaunchedEffect(userId) {
//        viewModel.connect(userId)
//        delay(2000)
//        viewModel.loadUserRelations(userId)
//    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collectLatest {
            viewModel.setSelectedTab(PostFilterTab.entries.get(pagerState.currentPage))
        }
    }

    Box(
        modifier = Modifier
            .background(color = Black1)
            .padding(top = topPadding.dp)
            .padding(bottom = bottomPadding.dp)
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(top = 50.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                Text(
                    text = "Posts",
                    style = Typography.bodyLarge,
                    color = White1,
                    modifier = Modifier
                        .padding(start = 15.dp)
                        .padding(end = 22.dp)
                        .padding(bottom = 2.dp)
                        .padding(top = 15.dp)
                        .padding(start = 3.dp)
                )
            }
            item {
                MyStatus(
                    user = mockPosts.entries.toList().first().value.first().user,
                    onAddPostClicked = {},
                    onViewStatusClicked = { userId ->
                        navToViewStatusScreen(userId)
                    },
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .padding(start = 15.dp)
                        .padding(end = 22.dp)
                        .padding(start = 15.dp),
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    PostFilterTab.entries.forEach { tab ->

                        SelectableText(
                            text = when (tab) {
                                PostFilterTab.ALL -> "All"
                                PostFilterTab.FOLLOWING -> "Following"
                                PostFilterTab.CONTACTS -> "Contacts"
                            },
                            isSelected = selectedTab == tab,
                            onClick = {
                                viewModel.setSelectedTab(tab)
                                scope.launch {
                                    pagerState.animateScrollToPage(tab.ordinal)
                                }
                            }
                        )
                    }
                }
            }
            item {
                Posts(
                    state = pagerState,
                    filteredPosts = filteredPosts,
                    navToViewStatusScreen = { userId ->
                        navToViewStatusScreen(userId)
                    }
                )
            }
        }
        TopBar(
            topPadding = topPadding,
            onSearchClicked = navToAddPostScreen
        )
    }
}

@Composable
fun MyStatus(
    modifier: Modifier = Modifier,
    user: User,
    posts: List<Post> = emptyList(),
    onAddPostClicked: () -> Unit,
    onViewStatusClicked: (Int) -> Unit
) {
    if (posts == emptyList<Post>()) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 15.dp)
                .padding(end = 22.dp)
                .padding(start = 3.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 5.dp)
                        .padding(end = 5.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(54.dp),
                        color = Color(0xFF3F3F3F),
                        shape = CircleShape
                    ) {

                    }
                }
                Surface(
                    modifier = Modifier.size(27.dp),
                    color = Blue3,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    border = BorderStroke(width = 2.dp, color = Black1)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.plus_icon_thick),
                        contentDescription = null,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Add post",
                    style = Typography.bodyMedium,
                    color = White1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Disappears after 24 hours",
                    style = Typography.bodySmall,
                    color = Grey3
                )
            }
        }
    } else {
        StatusItem(
            posts = posts,
            username = "My Status",
            onStatusClicked = { userId ->
                onViewStatusClicked(userId)
            }
        )
    }
}

@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    topPadding: Int,
    onSearchClicked: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(start = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "hubble",
            style = Typography.headlineLarge.copy(color = White1),
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onSearchClicked,
            shape = RectangleShape,
            modifier = Modifier.wrapContentSize()
        ) {
            Icon(
                painter = painterResource(R.drawable.search_icon),
                contentDescription = "Status menu",
                tint = White1,
                modifier = Modifier
                    .height(24.dp)
            )
        }
        Spacer(Modifier.width(1.dp))
        IconButton(
            onClick = {},
            shape = RectangleShape,
            modifier = Modifier.wrapContentSize()
        ) {
            Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = "Status menu",
                tint = White1,
                modifier = Modifier
                    .height(24.dp)
            )
        }
    }
}

@Composable
fun SelectableText(
    modifier: Modifier = Modifier,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier
            .clickable { onClick() },
        color = if (isSelected) White2 else Grey3,
        fontSize = 13.sp
    )
}

@Composable
fun Posts(
    modifier: Modifier = Modifier,
    state: PagerState,
    filteredPosts: Map<Int, List<Post>>,
    navToViewStatusScreen: (Int) -> Unit
) {
    HorizontalPager(
        modifier = modifier.fillMaxSize(),
        beyondViewportPageCount = 0,
        state = state
    ) { page ->
        when (page) {
            0 -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                filteredPosts.entries.forEach { (_, postList) ->
                    StatusItem(
                        posts = postList,
                        username = postList[0].user.username,
                        onStatusClicked = { userId ->
                            navToViewStatusScreen(userId)
                        }
                    )
                }
            }
            1 -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                filteredPosts.entries.forEach { (_, postList) ->
                    StatusItem(
                        posts = postList,
                        username = postList[0].user.username,
                        onStatusClicked = { userId ->
                            navToViewStatusScreen(userId)
                        }
                    )
                }
            }
            2 -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                filteredPosts.entries.forEach { (_, postList) ->
                    StatusItem(
                        posts = postList,
                        username = postList[0].user.username,
                        onStatusClicked = { userId ->
                            navToViewStatusScreen(userId)
                        }
                    )
                }
            }
        }
    }
}