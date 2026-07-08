package com.orb.hubble.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun TwoDimensionalLazyLayout(
    rowCount: Int = 4,
    columnCount: Int = 4,
    itemSize: Dp = 100.dp
) {
    val state = remember { TwoDimensionalScrollState() }
    val itemProvider = remember(rowCount, columnCount) {
        object : LazyLayoutItemProvider {
            override val itemCount: Int = rowCount * columnCount

            @Composable
            override fun Item(index: Int, key: Any) {
                val row = index / columnCount
                val col = index % columnCount
                Box(
                    modifier = Modifier
                        .size(itemSize)
                        .background(if ((row + col) % 2 == 0) Color.Gray else Color.LightGray)
                        .border(1.dp, Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$row,$col")
                }
            }
        }
    }

    LazyLayout(
        itemProvider = { itemProvider },
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    state.onScroll(dragAmount.x, dragAmount.y)
                }
            }
    ) { constraints ->
        val itemSizePx = itemSize.roundToPx()

        // 1. Calculate which items are visible
        val viewportWidth = constraints.maxWidth
        val viewportHeight = constraints.maxHeight

        val firstVisibleColumn = (state.scrollX.toInt() / itemSizePx).coerceAtLeast(0)
        val lastVisibleColumn = ((state.scrollX.toInt() + viewportWidth) / itemSizePx)
            .coerceAtMost(columnCount - 1)

        val firstVisibleRow = (state.scrollY.toInt() / itemSizePx).coerceAtLeast(0)
        val lastVisibleRow = ((state.scrollY.toInt() + viewportHeight) / itemSizePx)
            .coerceAtMost(rowCount - 1)

        val placeablesCache = mutableListOf<Triple<Placeable, Int, Int>>()

        // 2. Measure only the visible items
        for (row in firstVisibleRow..lastVisibleRow) {
            for (col in firstVisibleColumn..lastVisibleColumn) {
                val index = row * columnCount + col
                val itemConstraints = Constraints.fixed(itemSizePx, itemSizePx)
                val placeables = compose(index).map { it.measure(itemConstraints) }

                for (placeable in placeables) {
                    // Calculate relative position based on grid and scroll offset
                    val xPos = (col * itemSizePx) - state.scrollX.toInt()
                    val yPos = (row * itemSizePx) - state.scrollY.toInt()
                    placeablesCache.add(Triple(placeable, xPos, yPos))
                }
            }
        }

        // 3. Layout
        layout(viewportWidth, viewportHeight) {
            placeablesCache.forEach { (placeable, x, y) ->
                placeable.placeRelative(x, y)
            }
        }
    }
}

@Stable
class TwoDimensionalScrollState {
    var scrollX by mutableFloatStateOf(0f)
    var scrollY by mutableFloatStateOf(0f)

    fun onScroll(deltaX: Float, deltaY: Float) {
        scrollX -= deltaX
        scrollY -= deltaY
        // You would add boundary constraints here (0..maxScroll)
    }
}

@Composable
fun CrossLayout1(
    itemCount: Int,
    beyondViewportItemCount: Int = 1,
    itemContent: @Composable (Int) -> Unit = { index ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "index $index")
        }
    }
) {
    // 1. Track the current page and the fractional offset (-0.5 to 0.5)
    // For a production version, you'd use an Animatable or PagerState
    var currentIndex by remember { mutableIntStateOf(0) }
    var rawOffsetX by remember { mutableFloatStateOf(0f) }
    var rawOffsetY by remember { mutableFloatStateOf(0f) }

    val itemProvider = remember(itemCount) {
        object : LazyLayoutItemProvider {
            override val itemCount = itemCount

            @Composable
            override fun Item(index: Int, key: Any) {
                itemContent(index)
            }
        }
    }

    LazyLayout(
        itemProvider = { itemProvider },
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Update offsets (simplified logic)
                    rawOffsetX += dragAmount.x
                    rawOffsetY += dragAmount.y
                }
            }
    ) { constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight

        // In a real pager, you'd calculate which page is "dominant"
        // based on the largest offset and update currentIndex.

        val placeables = mutableListOf<Pair<Placeable, IntOffset>>()

        // Function to safely compose and add to list
        fun enqueueItem(index: Int, xOffset: Int, yOffset: Int) {

//            val existingPlaceable = placeables.firstOrNull {
//                it.third == index
//            }
//
//            if (existingPlaceable == null) {
//                if (index in 0 until itemCount) {
//                    compose(index).forEach { measurable ->
//                        val placeable = measurable.measure(constraints)
//
//                        placeables.add(Triple(placeable, IntOffset(xOffset + rawOffsetX.toInt(), yOffset + rawOffsetY.toInt()), index))
//                    }
//                }
//            } else {
//                if (index in 0 until itemCount) {
//                    placeables.forEach { triple ->
//                        if (triple.third == index) {
//                            placeables.add(Triple(triple.first, IntOffset(xOffset + rawOffsetX.toInt(), yOffset + rawOffsetY.toInt()), index))
//                        }
//                    }
//                }
//            }

            if (index in 0 until itemCount) {
                compose(index).forEach { measurable ->
                    val placeable = measurable.measure(constraints)

                    placeables.add(
                        placeable to IntOffset(
                            xOffset + rawOffsetX.toInt(),
                            yOffset + rawOffsetY.toInt()
                        )
                    )
                }
            }
        }

        // --- THE CROSS LOGIC ---

        // 1. Center Item
        enqueueItem(currentIndex, 0, 0)

        // 2. Beyond Viewport / Neighbor Logic
        for (distance in 1..beyondViewportItemCount) {
            val prevIndex = currentIndex - distance
            val nextIndex = currentIndex + distance

            // Previous Item (Top and Left)
            enqueueItem(prevIndex, -width * distance, 0) // Left slot
            enqueueItem(prevIndex, 0, -height * distance) // Top slot

            // Next Item (Bottom and Right)
            enqueueItem(nextIndex, width * distance, 0)  // Right slot
            enqueueItem(nextIndex, 0, height * distance)  // Bottom slot
        }

        layout(width, height) {
            placeables.forEach { (placeable, offset) ->
                placeable.placeRelative(offset.x, offset.y)
            }
        }
    }
}

@Composable
fun CrossLayout2(
    itemCount: Int,
    beyondViewportItemCount: Int = 3,
    itemContent: @Composable (Int) -> Unit = { index ->
        Text(
            text = "index $index",
            modifier = Modifier.size(40.dp)
        )
    }
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var rawOffsetX by remember { mutableFloatStateOf(0f) }
    var rawOffsetY by remember { mutableFloatStateOf(0f) }

    val itemProvider = remember(itemCount) {
        object : LazyLayoutItemProvider {
            // We double the count: one for Horizontal identity, one for Vertical
            override val itemCount = itemCount * 2

            @Composable
            override fun Item(index: Int, key: Any) {
                // Map the virtual index back to the real data index
                val realIndex = index / 2
                itemContent(realIndex)
            }

            // Crucial: Provide unique keys so Compose doesn't recycle the
            // horizontal version into the vertical version incorrectly.
            override fun getKey(index: Int): Any = "slot_${index}"
        }
    }

    LazyLayout(
        itemProvider = { itemProvider },
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    rawOffsetX += dragAmount.x
                    rawOffsetY += dragAmount.y
                }
            }
    ) { constraints ->
        val w = constraints.maxWidth / 6
        val h = constraints.maxHeight / 6
        val placeables = mutableListOf<Pair<Placeable, IntOffset>>()

        // Helper to compose using our virtual indexing
        fun enqueueItem(realIndex: Int, isVerticalSlot: Boolean, x: Int, y: Int) {
            if (realIndex in 0 until itemCount) {
                // Map to virtual index: Even for horizontal, Odd for vertical
                val virtualIndex = (realIndex * 2) + (if (isVerticalSlot) 1 else 0)

                compose(virtualIndex).forEach { measurable ->
                    val p = measurable.measure(constraints)
                    placeables.add(p to IntOffset(x + rawOffsetX.toInt(), y + rawOffsetY.toInt()))
                }
            }
        }

        // --- THE CROSS PLACEMENT ---

        // 1. Center Item (Horizontal Slot)
        enqueueItem(currentIndex, isVerticalSlot = false, 0, 0)

        val prev = currentIndex - 1
        val next = currentIndex + 1

        // Previous Item
        enqueueItem(prev, isVerticalSlot = false, -w, 0) // Left (Horizontal)
        enqueueItem(prev, isVerticalSlot = true, 0, -h)  // Top (Vertical)

        // Next Item
        enqueueItem(next, isVerticalSlot = false, w, 0)  // Right (Horizontal)
        enqueueItem(next, isVerticalSlot = true, 0, h)   // Bottom (Vertical)

        layout(w, h) {
            placeables.forEach { (placeable, offset) ->
                placeable.placeRelative(offset.x, offset.y)
            }
        }
    }
}

@Composable
fun CrossLayout(
    itemCount: Int,
    beyondViewportItemCount: Int = 1,
    itemContent: @Composable (Int) -> Unit = { index ->
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "index $index")
        }
    }
) {
    val state = remember { CrossLayoutState() }

    val itemProvider = remember(itemCount) {
        object : LazyLayoutItemProvider {
            override val itemCount = itemCount

            @Composable
            override fun Item(index: Int, key: Any) {
                itemContent(index)
            }
        }
    }

    LazyLayout(
        itemProvider = { itemProvider },
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        state.swipeDirection = SwipeDirection.None
                    },
                    onDragEnd = {
                        //state.swipeDirection = SwipeDirection.None
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        state.rawOffsetX += dragAmount.x
                        state.rawOffsetY += dragAmount.y

                        //lock in the swipe direction on the first drag event
                        if (state.swipeDirection == SwipeDirection.None) {
                            state.swipeDirection = if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                if (dragAmount.x > 0) {
                                    SwipeDirection.Left
                                } else {
                                    SwipeDirection.Right
                                }
                            } else {
                                if (dragAmount.y > 0) {
                                    SwipeDirection.Up
                                } else {
                                    SwipeDirection.Down
                                }
                            }
                        }
                    }
                )
            }
    ) { constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight

        // In a real pager, you'd calculate which page is "dominant"
        // based on the largest offset and update currentIndex.

        val placeables = mutableListOf<Pair<Placeable, IntOffset>>()

        // Function to safely compose and add to list
        fun enqueueItem(index: Int, xOffset: Int, yOffset: Int) {
            if (index in 0 until itemCount) {
                compose(index).forEach { measurable ->
                    val placeable = measurable.measure(constraints)
                    placeables.add(
                        placeable to IntOffset(
                            xOffset + state.rawOffsetX.toInt(),
                            yOffset + state.rawOffsetY.toInt()
                        )
                    )
                }
            }
        }

        // --- THE CROSS LOGIC ---

        // 1. Center Item
        enqueueItem(state.currentIndex, 0, 0)

        val prevIndex = state.currentIndex - 1
        val nextIndex = state.currentIndex + 1

        when (state.swipeDirection) {
            SwipeDirection.Up -> {
                // Previous item
                enqueueItem(prevIndex, 0, -height)
            }

            SwipeDirection.Down -> {
                // Next item
                enqueueItem(nextIndex, 0, height)
            }

            SwipeDirection.Left -> {
                //Previous item
                enqueueItem(prevIndex, -width, 0)
            }

            SwipeDirection.Right -> {
                //Next item
                enqueueItem(nextIndex, width, 0)
            }

            SwipeDirection.None -> {

            }
        }

        layout(width, height) {
            placeables.forEach { (placeable, offset) ->
                placeable.placeRelative(offset.x, offset.y)
            }
        }
    }
}

@Stable
class CrossLayoutState {
    var currentIndex by mutableIntStateOf(0)
    var rawOffsetX by mutableFloatStateOf(0f)
    var rawOffsetY by mutableFloatStateOf(0f)
    var swipeDirection by mutableStateOf(SwipeDirection.None)
}

enum class SwipeDirection { None, Up, Down, Left, Right }