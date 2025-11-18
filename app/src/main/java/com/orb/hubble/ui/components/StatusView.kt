package com.orb.hubble.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.orb.hubble.data.Post
import com.orb.hubble.ui.theme.Blue2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Alternative implementation with progress indication
@Composable
fun WhatsAppStatusIndicatorWithProgress(
    postCount: Int,
    currentPostIndex: Int,
    progress: Float, // Progress of current post (0f to 1f)
    modifier: Modifier = Modifier,
    innerCircleSize: Dp = 60.dp,
    strokeWidth: Dp = 4.dp,
    activeColor: Color = Color(0xFF25D366),
    viewedColor: Color = Color(0xFF666666), // Gray for viewed posts
    upcomingColor: Color = Color(0xFFCCCCCC) // Light gray for upcoming posts
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Outer segmented progress circle with animated progress
        AnimatedSegmentedCircularProgress(
            segmentCount = postCount,
            currentSegmentIndex = currentPostIndex,
            progress = progress,
            strokeWidth = strokeWidth,
            activeColor = activeColor,
            viewedColor = viewedColor,
            upcomingColor = upcomingColor,
            modifier = Modifier.size(innerCircleSize + strokeWidth * 2)
        )

        // Inner circle
        Canvas(
            modifier = Modifier.size(innerCircleSize)
        ) {
            drawCircle(
                color = Color.LightGray,
                radius = size.minDimension / 2
            )
        }
    }
}

@Composable
fun AnimatedSegmentedCircularProgress(
    segmentCount: Int,
    currentSegmentIndex: Int,
    progress: Float,
    strokeWidth: Dp,
    activeColor: Color,
    viewedColor: Color,
    upcomingColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidthPx = strokeWidth.toPx()
        val radius = (size.minDimension - strokeWidthPx) / 2
        val center = Offset(size.width / 2, size.height / 2)
        val segmentAngle = 360f / segmentCount

        for (i in 0 until segmentCount) {
            val startAngle = i * segmentAngle - 90f
            val sweepAngle = when {
                i < currentSegmentIndex -> segmentAngle - 2f // Viewed posts - full segment
                i == currentSegmentIndex -> (segmentAngle - 2f) * progress // Current post - partial progress
                else -> 0f // Upcoming posts - not visible yet
            }

            if (sweepAngle > 0) {
                val color = when {
                    i < currentSegmentIndex -> viewedColor
                    i == currentSegmentIndex -> activeColor
                    else -> upcomingColor
                }

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
        }
    }
}

// Usage examples
@Composable
fun StatusIndicatorExamples() {
    Column {
        // Simple indicator showing user has 3 posts
//        StatusView(
//            postCount = 8,
//            modifier = Modifier.size(80.dp)
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Indicator with 4 posts
//        StatusView(
//            postCount = 4,
//            modifier = Modifier.size(80.dp)
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Animated indicator showing progress on second post (index 1)
//        WhatsAppStatusIndicatorWithProgress(
//            postCount = 4,
//            currentPostIndex = 2,
//            progress = 0.7f, // 70% progress on current post
//            modifier = Modifier.size(80.dp)
//        )
    }
}