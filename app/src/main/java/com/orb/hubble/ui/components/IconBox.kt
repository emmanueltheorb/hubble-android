package com.orb.hubble.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.orb.hubble.R
import com.orb.hubble.ui.theme.White1

@Composable
fun IconBox(
    modifier: Modifier = Modifier,
    @DrawableRes iconId: Int,
    iconSize: Int = 18,
    iconColor: Color = Color.White,
    boxSize: Int = 40,
    contentDescription: String,
    onIconClicked: () -> Unit
) {
    Box(
        modifier = modifier
            .size(boxSize.dp)
            .clip(CircleShape)
            .clickable(onClick = onIconClicked),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(iconId),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(iconColor),
            modifier = Modifier
                .size(iconSize.dp)
        )
    }
}