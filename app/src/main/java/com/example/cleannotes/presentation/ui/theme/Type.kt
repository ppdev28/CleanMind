package com.example.cleannotes.presentation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.cleannotes.R

val Inter = FontFamily(
    Font(R.font.inter_regular), Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold), Font(R.font.inter_bold, FontWeight.Bold)
)
// Kept as aliases for existing integrations; all app text now uses the Figma family.
val SfPro = Inter
val LexendDeca = Inter
val Typography = Typography(
    headlineLarge = TextStyle(lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None), fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None), fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 35.sp),
    headlineSmall = TextStyle(lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None), fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None), fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None), fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None), fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None), fontFamily = Inter, fontSize = 18.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None), fontFamily = Inter, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None), fontFamily = Inter, fontSize = 12.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None), fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None), fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None), fontFamily = Inter, fontSize = 11.sp, lineHeight = 20.sp)
)
