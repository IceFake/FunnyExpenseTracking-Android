package com.example.funnyexpensetracking.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funnyexpensetracking.data.local.UserPreferencesManager

/**
 * Compose主题颜色定义
 */
@Immutable
data class ComposeColors(
    // Chart Colors
    val expenseChartColors: List<Color> = emptyList(),
    val incomeChartColors: List<Color> = emptyList(),
    
    // Semantic Colors
    val incomeGreen: Color = Color.Unspecified,
    val expenseRed: Color = Color.Unspecified,
    val investmentBlue: Color = Color.Unspecified,
    val savingsTeal: Color = Color.Unspecified,
    
    // Status Colors
    val success: Color = Color.Unspecified,
    val warning: Color = Color.Unspecified,
    val errorRed: Color = Color.Unspecified,
    val info: Color = Color.Unspecified
)

/**
 * 本地颜色提供者
 */
val LocalComposeColors = staticCompositionLocalOf { ComposeColors() }

/**
 * 扩展函数获取自定义颜色
 */
val MaterialTheme.composeColors: ComposeColors
    @Composable
    @ReadOnlyComposable
    get() = LocalComposeColors.current

/**
 * 创建浅色主题颜色方案 — 暖色调
 */
private val LightColorPalette = lightColorScheme(
    primary = Color(0xFFFF7043),             // 暖橙
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFCCBC),    // 浅珊瑚
    onPrimaryContainer = Color(0xFFBF360C),
    secondary = Color(0xFFFFB74D),           // 琥珀
    onSecondary = Color(0xFF3E2723),
    secondaryContainer = Color(0xFFFFECB3),  // 浅琥珀
    onSecondaryContainer = Color(0xFF5D4037),
    tertiary = Color(0xFFFF8A80),            // 浅红
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFCDD2),   // 浅粉
    onTertiaryContainer = Color(0xFFB71C1C),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFFF8E1),          // 暖米白
    onBackground = Color(0xFF3E2723),
    surface = Color(0xFFFFF8E1),             // 暖米白
    onSurface = Color(0xFF3E2723),
    surfaceVariant = Color(0xFFFFECB3),      // 浅琥珀
    onSurfaceVariant = Color(0xFF5D4037),
    outline = Color(0xFF8D6E63),             // 暖棕
    outlineVariant = Color(0xFFD7CCC8)       // 浅棕
)

/**
 * 创建深色主题颜色方案 — 暖色调
 */
private val DarkColorPalette = darkColorScheme(
    primary = Color(0xFFFFAB91),             // 浅橙
    onPrimary = Color(0xFF3E2723),
    primaryContainer = Color(0xFFE64A19),    // 深橙红
    onPrimaryContainer = Color(0xFFFFCCBC),
    secondary = Color(0xFFFFE082),           // 浅琥珀
    onSecondary = Color(0xFF3E2723),
    secondaryContainer = Color(0xFFFF8F00),  // 琥珀
    onSecondaryContainer = Color(0xFFFFECB3),
    tertiary = Color(0xFFEF9A9A),            // 浅红
    onTertiary = Color(0xFFB71C1C),
    tertiaryContainer = Color(0xFFC62828),   // 深红
    onTertiaryContainer = Color(0xFFFFCDD2),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF3E2723),          // 暖深棕
    onBackground = Color(0xFFEFEBE9),
    surface = Color(0xFF3E2723),             // 暖深棕
    onSurface = Color(0xFFEFEBE9),
    surfaceVariant = Color(0xFF5D4037),      // 中棕
    onSurfaceVariant = Color(0xFFD7CCC8),
    outline = Color(0xFFA1887F),             // 浅棕
    outlineVariant = Color(0xFF4E342E)       // 深棕
)

/**
 * 浅色主题自定义颜色 — 暖色系
 */
private val LightComposeColors = ComposeColors(
    expenseChartColors = listOf(
        Color(0xFFFF7043), // 深橙
        Color(0xFFFF5722), // 橙红
        Color(0xFFE64A19), // 深橙红
        Color(0xFFFF8A65), // 浅橙
        Color(0xFFFFAB91), // 浅珊瑚
        Color(0xFFFFCCBC), // 浅橙
        Color(0xFFD7CCC8), // 浅棕
        Color(0xFFA1887F), // 棕色
        Color(0xFF8D6E63), // 中棕
        Color(0xFF795548)  // 棕色
    ),
    incomeChartColors = listOf(
        Color(0xFF66BB6A), // 绿色
        Color(0xFF81C784), // 浅绿
        Color(0xFFAED581), // 浅黄绿
        Color(0xFFFFEE58), // 黄色
        Color(0xFFFFCA28), // 琥珀
        Color(0xFFFFA726), // 橙色
        Color(0xFFFF7043), // 深橙
        Color(0xFFFF8A65), // 浅橙
        Color(0xFFFFB74D), // 琥珀
        Color(0xFFFFD54F)  // 金色
    ),
    incomeGreen = Color(0xFF66BB6A),
    expenseRed = Color(0xFFEF5350),
    investmentBlue = Color(0xFFFFA726),     // 改用琥珀色
    savingsTeal = Color(0xFF8D6E63),        // 改用暖棕色
    success = Color(0xFF66BB6A),
    warning = Color(0xFFFFA726),
    errorRed = Color(0xFFEF5350),
    info = Color(0xFFFFA726)                // 改用琥珀色
)

/**
 * 深色主题自定义颜色 — 暖色系
 */
private val DarkComposeColors = ComposeColors(
    expenseChartColors = listOf(
        Color(0xFFFFAB91), // 浅橙
        Color(0xFFFF8A65), // 橙
        Color(0xFFFF7043), // 深橙
        Color(0xFFFF5722), // 橙红
        Color(0xFFE64A19), // 深橙红
        Color(0xFFFFCCBC), // 浅珊瑚
        Color(0xFFD7CCC8), // 浅棕
        Color(0xFFA1887F), // 棕色
        Color(0xFF8D6E63), // 中棕
        Color(0xFF795548)  // 棕色
    ),
    incomeChartColors = listOf(
        Color(0xFFA5D6A7), // 浅绿
        Color(0xFFC8E6C9), // 浅浅绿
        Color(0xFFE6EE9C), // 浅酸橙
        Color(0xFFFFF59D), // 浅黄
        Color(0xFFFFE082), // 浅琥珀
        Color(0xFFFFCC80), // 浅橙
        Color(0xFFFFAB91), // 浅橙
        Color(0xFFFF8A65), // 橙
        Color(0xFFFFB74D), // 琥珀
        Color(0xFFFFD54F)  // 金色
    ),
    incomeGreen = Color(0xFFA5D6A7),
    expenseRed = Color(0xFFEF9A9A),
    investmentBlue = Color(0xFFFFB74D),     // 改用浅琥珀色
    savingsTeal = Color(0xFFA1887F),        // 改用暖棕色
    success = Color(0xFFA5D6A7),
    warning = Color(0xFFFFB74D),
    errorRed = Color(0xFFEF9A9A),
    info = Color(0xFFFFB74D)                // 改用浅琥珀色
)

/**
 * 自定义排版定义
 */
private val FunnyExpenseTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * 自定义形状定义
 */
private val FunnyExpenseShapes = androidx.compose.material3.Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)

/**
 * 记住当前主题模式（基于用户偏好）
 */
@Composable
private fun rememberThemeMode(): Boolean {
    val context = LocalContext.current
    val prefs = remember { UserPreferencesManager(context) }
    val isSystemInDarkTheme = isSystemInDarkTheme()
    return prefs.shouldUseDarkTheme(isSystemInDarkTheme)
}

/**
 * 趣味记账Compose主题
 * 
 * @param darkTheme 是否使用深色主题（如果为null，则使用用户偏好设置）
 * @param dynamicColor 是否使用动态配色（Android 12+）
 * @param content 子组件内容
 */
@Composable
fun FunnyExpenseTheme(
    darkTheme: Boolean? = null,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val useDarkTheme = darkTheme ?: rememberThemeMode()
    
    // 选择颜色方案
    val colorScheme = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColorPalette
        else -> LightColorPalette
    }
    
    // 选择自定义颜色
    val composeColors = if (useDarkTheme) DarkComposeColors else LightComposeColors
    
    CompositionLocalProvider(LocalComposeColors provides composeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FunnyExpenseTypography,
            shapes = FunnyExpenseShapes,
            content = content
        )
    }
}