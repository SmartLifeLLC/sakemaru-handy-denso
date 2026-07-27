package biz.smt_life.android.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.sp

private val JapaneseLocaleList = LocaleList(Locale("ja-JP"))
private val JapaneseFontFamily = FontFamily.SansSerif

private fun TextStyle.withJapaneseGlyphs(): TextStyle = copy(
    fontFamily = fontFamily ?: JapaneseFontFamily,
    localeList = JapaneseLocaleList
)

private val BaseTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = JapaneseFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleMedium = TextStyle(
        fontFamily = JapaneseFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = JapaneseFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = JapaneseFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.125.sp
    ),
    labelLarge = TextStyle(
        fontFamily = JapaneseFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.05.sp
    )
)

val Typography = BaseTypography.copy(
    displayLarge = BaseTypography.displayLarge.withJapaneseGlyphs(),
    displayMedium = BaseTypography.displayMedium.withJapaneseGlyphs(),
    displaySmall = BaseTypography.displaySmall.withJapaneseGlyphs(),
    headlineLarge = BaseTypography.headlineLarge.withJapaneseGlyphs(),
    headlineMedium = BaseTypography.headlineMedium.withJapaneseGlyphs(),
    headlineSmall = BaseTypography.headlineSmall.withJapaneseGlyphs(),
    titleLarge = BaseTypography.titleLarge.withJapaneseGlyphs(),
    titleMedium = BaseTypography.titleMedium.withJapaneseGlyphs(),
    titleSmall = BaseTypography.titleSmall.withJapaneseGlyphs(),
    bodyLarge = BaseTypography.bodyLarge.withJapaneseGlyphs(),
    bodyMedium = BaseTypography.bodyMedium.withJapaneseGlyphs(),
    bodySmall = BaseTypography.bodySmall.withJapaneseGlyphs(),
    labelLarge = BaseTypography.labelLarge.withJapaneseGlyphs(),
    labelMedium = BaseTypography.labelMedium.withJapaneseGlyphs(),
    labelSmall = BaseTypography.labelSmall.withJapaneseGlyphs()
)
