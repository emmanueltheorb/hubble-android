package com.orb.hubble.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.orb.hubble.R
import com.orb.hubble.data.HomeScreenViewModel
import com.orb.hubble.data.Post
import com.orb.hubble.data.User
import com.orb.hubble.data.mockPosts
//import com.orb.hubble.data.postsList10
//import com.orb.hubble.data.postsList4
import com.orb.hubble.ui.components.StatusItem
import com.orb.hubble.ui.theme.Black1
import com.orb.hubble.ui.theme.Blue2
import com.orb.hubble.ui.theme.Blue3
import com.orb.hubble.ui.theme.Grey2
import com.orb.hubble.ui.theme.Grey3
import com.orb.hubble.ui.theme.Grey4
import com.orb.hubble.ui.theme.Typography
import com.orb.hubble.ui.theme.White1

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    userId: Int = 4,
    viewModel: HomeScreenViewModel = hiltViewModel(),
    navToViewStatusScreen: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val bottomPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value.toInt()
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().value.toInt()

    LaunchedEffect(Unit) {
        Log.d("DEBUG", "HomeScreen ViewModel: $viewModel")
    }

    LaunchedEffect(userId) {
        viewModel.connect(userId)
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
                .padding(start = 18.dp)
                .padding(end = 22.dp)
                .padding(top = 65.dp),
            verticalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            item {
                Text(
                    text = "Posts",
                    style = Typography.bodyLarge,
                    color = White1,
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .padding(top = 15.dp)
                        .padding(start = 3.dp)
                )
            }
            item {
                MyStatus(
                    user = mockPosts.entries.toList().first().value.first().user,
                    onAddPostClicked = {},
                    onViewStatusClicked = {},
                )
            }
            item {
                Text(
                    text = "Recent updates",
                    style = Typography.bodySmall.copy(fontSize = 21.sp),
                    color = Grey3,
                    modifier = Modifier.padding(start = 3.dp)
                )
            }
            items(
                items = mockPosts.entries.toList(),
                key = { it.key }
            ) { (_, postList) ->
                StatusItem(
                    posts = postList,
                    onStatusClicked = { userId ->
                        navToViewStatusScreen(userId)
                    }
                )
            }
        }
        TopBar(
            topPadding = topPadding,
            onSearchClicked = {  }
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
                        modifier = Modifier.size(60.dp),
                        color = Color(0xFF3F3F3F),
                        shape = CircleShape
                    ) {

                    }
                }
                Surface(
                    modifier = Modifier.size(30.dp),
                    color = Blue3,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    border = BorderStroke(width = 2.dp, color = Black1)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.plus_icon_thick),
                        contentDescription = null,
                        modifier = Modifier.padding(7.dp)
                    )
                }
            }
            Spacer(Modifier.width(15.dp))
            Column {
                Text(
                    text = "Add post",
                    style = Typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = White1
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "Disappears after 24 hours",
                    style = Typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.1.sp
                    ),
                    color = Grey3
                )
            }
        }
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
            .height(65.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "hubble",
            style = Typography.headlineLarge.copy(color = White1),
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = {}
        ) {

        }
    }
}