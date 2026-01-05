package com.orb.hubble.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.error
import com.orb.hubble.R
import com.orb.hubble.data.Post
import com.orb.hubble.data.User
import com.orb.hubble.ui.theme.Blue2
import com.orb.hubble.ui.theme.Grey3
import com.orb.hubble.ui.theme.RobotoFontFamily
import com.orb.hubble.ui.theme.Typography
import com.orb.hubble.ui.theme.White1
import com.orb.hubble.ui.theme.White2
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Composable
fun StatusItem(
    modifier: Modifier = Modifier,
    posts: List<Post>,
    username: String,
    onStatusClicked: (Int) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 15.dp)
            .padding(end = 22.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onStatusClicked(posts[0].userId) }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusView(
            posts = posts,
            interactionSource = interactionSource
        )
        Spacer(Modifier.width(15.dp))
        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = username,
                    style = Typography.bodyMedium,
                    color = White1
                )
                Time(insertedAt = posts.last().insertedAt)
            }
            Text(
                text = posts.last().user.username,
                style = Typography.labelSmall.copy(fontSize = 11.sp),
                color = White2
            )
        }
        Spacer(modifier.weight(1f))
        ProfilePic(user = posts[0].user)
    }
}

@Composable
fun StatusView(
    posts: List<Post>,
    modifier: Modifier = Modifier,
    innerCircleSize: Dp = 54.dp,
    strokeWidth: Dp = 2.25.dp,
    activeColor: Color = Blue2,
    inactiveColor: Color = Color(0xB9606073), // Gray for inactive segments
    interactionSource: MutableInteractionSource? = null
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
//        Canvas(
//            modifier = Modifier
//                .size(innerCircleSize)
//                .clickable(
//                    interactionSource = interactionSource,
//                    indication = ripple(bounded = false, radius = innerCircleSize / 2),
//                    onClick = {}
//                )
//        ) {
//            drawCircle(
//                color = Color(0xFF666666), // Placeholder for user's status image
//                radius = size.minDimension / 2
//            )
//        }
        Box(
            modifier = Modifier
                .size(innerCircleSize)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(posts.last().thumbnailUrl)
                    .size(with(LocalDensity.current) { innerCircleSize.roundToPx() })
                    .crossfade(true)
                    .error(R.drawable.thumbnail_load_error)
                    .build(),
                contentDescription = "Selected image",
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = false, radius = innerCircleSize / 2),
                        onClick = {}
                    ),
                contentScale = ContentScale.Crop
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
        val gapAngle = if (segmentCount <= 1) 0f else 12f
        val totalGapAngle = gapAngle * segmentCount
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
                    cap = if (segmentCount <= 1) StrokeCap.Butt else StrokeCap.Round
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
        modifier = modifier.size(45.dp),
        color = Color(0xFF3F3F3F),
        shape = CircleShape
    ) {

    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun Time(
    modifier: Modifier = Modifier,
    insertedAt: Instant
) {
    val nowInstant = Clock.System.now()
    val duration = nowInstant - insertedAt
    val localDateTime = insertedAt.toLocalDateTime(TimeZone.currentSystemDefault())

    val time = when {
        duration.inWholeSeconds < 60 -> "Just now"
        duration.inWholeMinutes < 60 -> "${duration.inWholeMinutes} mins ago"
        duration.inWholeHours > 12 -> "Yesterday"
        else -> buildString {
            append(localDateTime.hour)
            append(":")
            append(localDateTime.minute.toString().padStart(2, '0'))
        }
    }

    Text(
        text = time,
        color = Grey3,
        style = Typography.labelSmall.copy(fontSize = 10.sp),
        modifier = Modifier.padding(bottom = 3.dp)
    )
}
