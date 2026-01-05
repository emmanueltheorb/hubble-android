package com.orb.hubble.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.orb.hubble.R

val RobotoFontFamily = FontFamily(
    Font(R.font.font_roboto_bold, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(R.font.font_roboto_black, weight = FontWeight.Black, style = FontStyle.Normal),
    Font(R.font.font_roboto_regular, weight = FontWeight.Normal, style = FontStyle.Normal)
)

val NunitoFontFamily = FontFamily(
    Font(R.font.nunito_extra_light, weight = FontWeight.ExtraLight, style = FontStyle.Normal),
    Font(R.font.nunito_light, weight = FontWeight.Light, style = FontStyle.Normal),
    Font(R.font.nunito_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(R.font.nunito_medium, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(R.font.nunito_semi_bold, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(R.font.nunito_bold, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(R.font.nunito_extra_bold, weight = FontWeight.ExtraBold, style = FontStyle.Normal),
    Font(R.font.nunito_black, weight = FontWeight.Black, style = FontStyle.Normal)
)

// Set of Material typography styles to start with
val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = RobotoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 24.sp
    ),
    bodySmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)