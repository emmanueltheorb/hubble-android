package com.orb.hubble.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.orb.hubble.data.Post
import com.orb.hubble.data.User
import com.orb.hubble.ui.theme.Blue2

@Composable
fun StatusItem(
    modifier: Modifier = Modifier,
    posts: List<Post>,
    onStatusClicked: (Int) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = {
                onStatusClicked(posts[0].userId)
            }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusView(posts = posts)
        Spacer(modifier.weight(1f))
        ProfilePic(user = posts[0].user)
    }
}

@Composable
fun StatusView(
    posts: List<Post>,
    modifier: Modifier = Modifier,
    innerCircleSize: Dp = 60.dp,
    strokeWidth: Dp = 2.5.dp,
    activeColor: Color = Blue2,
    inactiveColor: Color = Color(0xB9606073) // Gray for inactive segments
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Outer segmented progress circle
        SegmentedCircularProgress(
            posts = posts,
            strokeWidth = strokeWidth,
            activeColor = activeColor,
            inactiveColor = inactiveColor,
            modifier = Modifier.size(innerCircleSize + strokeWidth * 3)
        )

        // Inner circle (where the user's latest status/image would be displayed)
        // In a real implementation, you would put an Image composable here
        Canvas(
            modifier = Modifier.size(innerCircleSize)
        ) {
            drawCircle(
                color = Color(0xFF666666), // Placeholder for user's status image
                radius = size.minDimension / 2
            )
        }
    }
}

@Composable
fun SegmentedCircularProgress(
    posts: List<Post>,
    strokeWidth: Dp,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidthPx = strokeWidth.toPx()
        val radius = (size.minDimension - strokeWidthPx) / 2
        val center = Offset(size.width / 2, size.height / 2)

        val segmentCount = posts.size
        val totalGapAngle = 12f * segmentCount
        val availableAngle = 360f - totalGapAngle
        val segmentAngle = availableAngle / segmentCount

        posts.forEachIndexed { index, post ->
            val startAngle = index * (segmentAngle + 12f) - 84.5f

            val color = if (post.viewed) inactiveColor else activeColor

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = segmentAngle,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = Size(radius * 2, radius * 2),
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

@Composable
fun ProfilePic(
    modifier: Modifier = Modifier,
    user: User
    ) {
    Surface(
        modifier = modifier.size(50.dp),
        color = Color(0xFF3F3F3F),
        shape = CircleShape
    ) {

    }
}
