package com.orb.hubble.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orb.hubble.ui.theme.NunitoFontFamily
import kotlinx.coroutines.delay

@Composable
fun OverlayText(
    modifier: Modifier = Modifier,
    text: String,
    textFieldStyle: FieldStyle = FieldStyle.Filled,
    textStyle: TextStyle,
    containerColor: Color = Color.Blue,
    roundedCorners: Dp = 13.dp,
    maxHeight: Dp,
    maxWidth: Dp,
    onClick: () -> Unit
) {
    val textPadding = 8.dp
    val maxWidth = maxWidth - (textPadding * 2)
    val maxHeight = maxHeight - (textPadding * 2)
    val measurer = rememberTextMeasurer()
    val constraints = calculateTextConstraints(maxWidth, maxHeight)
    val textLayoutResult = remember (text, textStyle, constraints) {
        measurer.measure(
            text = text,
            style = textStyle,
            constraints = constraints
        )
    }
    val textBoundingSize = with(LocalDensity.current) {
        DpSize(
            width = textLayoutResult.size.width.toDp(),
            height = textLayoutResult.size.height.toDp()
        )
    }
    val boxModifier = when (textFieldStyle) {
        FieldStyle.Border -> Modifier
            .sizeIn(maxWidth = maxWidth, maxHeight = maxHeight)
            .border(
                width = 2.dp,
                color = containerColor,
                shape = RoundedCornerShape(roundedCorners)
            )
        FieldStyle.Default -> Modifier
            .sizeIn(maxWidth = maxWidth, maxHeight = maxHeight)
        FieldStyle.Filled -> Modifier
            .sizeIn(maxWidth = maxWidth, maxHeight = maxHeight)
            .background(
                color = containerColor,
                shape = RoundedCornerShape(roundedCorners)
            )
        FieldStyle.SoftFilled -> Modifier
            .sizeIn(maxWidth = maxWidth, maxHeight = maxHeight)
            .background(
                color = containerColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(roundedCorners)
            )
    }

    Box (
        modifier = boxModifier
            .padding(textPadding)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = textStyle,
            modifier = Modifier
                .size(textBoundingSize)
        )
    }
}

@Composable
fun OverlayTextField(
    modifier: Modifier = Modifier,
    text: String,
    onTextChange: (String) -> Unit,
    textFieldStyle: FieldStyle = FieldStyle.Filled,
    fontSize: TextUnit = 24.sp,
    fontFamily: FontFamily = NunitoFontFamily,
    textAlign: TextAlign = TextAlign.Center,
    textColor: Color = Color.White,
    containerColor: Color = Color.Blue,
    roundedCorners: Dp = 13.dp,
    maxWidth: Dp = 500.dp,
    maxHeight: Dp = 500.dp
) {
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val textPadding = 8.dp
    val maxWidth = maxWidth - (textPadding * 2)
    val maxHeight = maxHeight - (textPadding * 2)
    val placeholderText = "Type here"
    val textStyle = remember (text, fontFamily, textAlign, fontSize, textColor) {
        TextStyle(
            fontSize = fontSize,
            fontFamily = fontFamily,
            textAlign = textAlign,
            color = textColor
        )
    }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = text,
                selection = TextRange(text.length)
            )
        )
    }

    LaunchedEffect(text) {
        if (text != textFieldValue.text) {
            textFieldValue = TextFieldValue(
                text = text
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        delay(100)
        keyboardController?.show()
    }

    val measurer = rememberTextMeasurer()
    val constraints = calculateTextConstraints(maxWidth, maxHeight)
    val textLayoutResult = remember (text, textStyle, constraints) {
        if (text.isEmpty()) {
            measurer.measure(
                text = placeholderText,
                style = textStyle,
                constraints = constraints
            )
        } else measurer.measure(
            text = text,
            style = textStyle,
            constraints = constraints
        )
    }
    val textBoundingSize = with(density) {
        DpSize(
            width = textLayoutResult.size.width.toDp(),
            height = textLayoutResult.size.height.toDp()
        )
    }
    val boxModifier = when (textFieldStyle) {
        FieldStyle.Border -> Modifier
            .sizeIn(maxWidth = maxWidth, maxHeight = maxHeight)
            .border(
                width = 2.dp,
                color = containerColor,
                shape = RoundedCornerShape(roundedCorners)
            )
        FieldStyle.Default -> Modifier
            .sizeIn(maxWidth = maxWidth, maxHeight = maxHeight)
        FieldStyle.Filled -> Modifier
            .sizeIn(maxWidth = maxWidth, maxHeight = maxHeight)
            .background(
                color = containerColor,
                shape = RoundedCornerShape(roundedCorners)
            )
        FieldStyle.SoftFilled -> Modifier
            .sizeIn(maxWidth = maxWidth, maxHeight = maxHeight)
            .background(
                color = containerColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(roundedCorners)
            )
    }

    Box (
        modifier = boxModifier
            .padding(textPadding),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                onTextChange(it.text)
            },
            cursorBrush = SolidColor(Color.White),
            singleLine = false,
            textStyle = textStyle,
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = placeholderText,
                            style = textStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            },
            modifier = modifier
                .focusRequester(focusRequester)
                .size(textBoundingSize)
        )
    }
}

@Composable
private fun calculateTextConstraints(maxWidth: Dp, maxHeight: Dp): Constraints {
    val density = LocalDensity.current
    return with(density) {
        Constraints(
            maxWidth = maxWidth.roundToPx(),
            maxHeight = maxHeight.roundToPx()
        )
    }
}

data class OverlayTextConfig(
    val textStyle: TextStyle,
    val fieldStyle: FieldStyle,
    val containerColor: Color
)

sealed class FieldStyle {
    data object Default : FieldStyle()
    data object Filled : FieldStyle()
    data object SoftFilled: FieldStyle()
    data object Border: FieldStyle()
}