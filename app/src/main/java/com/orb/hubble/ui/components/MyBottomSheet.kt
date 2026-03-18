package com.orb.hubble.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.orb.hubble.ui.theme.Black2
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBottomSheet(
    modifier: Modifier = Modifier,
    sheetHeight: Dp,
    bottomPadding: Int,
    sheetColor: Color = Black2,
    onDismiss: () -> Unit,
    dragHandle: @Composable (() -> Unit),
    content: @Composable (() -> Unit)
) {
    val density = LocalDensity.current
    val sheetHeightPx = with(density) { sheetHeight.toPx() }

    val dragState = remember {
        AnchoredDraggableState(
            initialValue = SheetPosition.Expanded,
            anchors = DraggableAnchors {
                SheetPosition.Expanded at 0f
                SheetPosition.Hidden at sheetHeightPx
            }
        )
    }

    LaunchedEffect(dragState) {
        snapshotFlow { dragState.settledValue }
            .collectLatest { settledState ->
                if (settledState != SheetPosition.Expanded) {
                    onDismiss()
                }
            }
    }

    BackHandler {
        onDismiss()
    }

    Surface(
        modifier = modifier
            .height(sheetHeight)
            .offset {
                IntOffset(
                    x = 0,
                    y = dragState.requireOffset().toInt().coerceAtLeast(0)
                )
            }
            .anchoredDraggable(
                state = dragState,
                orientation = Orientation.Vertical
            ),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = sheetColor
    ) {
        Box(
            modifier = modifier
                .navigationBarsPadding()
        ) {
            content()
            dragHandle()
        }
    }
}

enum class SheetPosition { Expanded, Hidden }