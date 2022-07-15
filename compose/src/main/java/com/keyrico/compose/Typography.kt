package com.keyrico.compose

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val fonts = FontFamily(
    Font(R.font.rubik_regular),
    Font(R.font.rubik_light, weight = FontWeight.Light),
)

internal val typography = Typography(
    bodySmall = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Light,
        fontSize = 14.sp
    )
)
