package com.example.funnyexpensetracking.ui.compose.utils

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/**
 * 动态颜色提取器
 * 
 * 从壁纸提取主要颜色，用于动态主题配色
 */
object DynamicColorExtractor {

    /**
     * 从壁纸提取主要颜色
     * 
     * @param wallpaperManager WallpaperManager实例
     * @return 提取的颜色列表（主要颜色、次要颜色、第三颜色），如果失败返回空列表
     */
    @RequiresApi(Build.VERSION_CODES.O_MR1)
    suspend fun extractColorsFromWallpaper(wallpaperManager: WallpaperManager): List<Color> {
        return withContext(Dispatchers.IO) {
            try {
                // 使用Android O MR1+的WallpaperColors API
                val wallpaperColors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                
                val colors = mutableListOf<Color>()
                
                // 提取主要颜色
                wallpaperColors?.primaryColor?.toArgb()?.let { colors.add(Color(it)) }
                
                // 提取次要颜色
                wallpaperColors?.secondaryColor?.toArgb()?.let { colors.add(Color(it)) }
                
                // 提取第三颜色
                wallpaperColors?.tertiaryColor?.toArgb()?.let { colors.add(Color(it)) }
                
                // 如果API提取的颜色不足，使用备选算法
                if (colors.size < 3) {
                    val fallbackColors = extractColorsFromWallpaperFallback(wallpaperManager)
                    colors.addAll(fallbackColors.take(3 - colors.size))
                }
                
                colors.take(3) // 最多返回3个颜色
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    /**
     * 备选颜色提取算法（兼容旧版本或API失败时）
     *
     * 注意：WallpaperManager.getDrawable() 在 Android 13 (API 33) 及以上版本需要
     * READ_WALLPAPER_INTERNAL 权限（系统权限，不可申请）或 MANAGE_EXTERNAL_STORAGE。
     * 由于这些权限对普通应用不可用，此方法不再调用 getDrawable()，
     * 而是返回空列表，让调用方使用默认主题色。
     */
    private fun extractColorsFromWallpaperFallback(@Suppress("UNUSED_PARAMETER") wallpaperManager: WallpaperManager): List<Color> {
        // WallpaperManager.getDrawable() 在高版本 Android 上需要系统级权限，
        // 无法安全调用，直接返回空列表使用默认配色
        return emptyList()
    }
    
    /**
     * 从Bitmap提取主要颜色（简化版K-Means算法）
     * 
     * @param bitmap 源图像
     * @param colorCount 要提取的颜色数量
     * @return 提取的主要颜色列表
     */
    private fun extractDominantColors(bitmap: Bitmap, colorCount: Int): List<Color> {
        // 缩小图像以加速处理
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false)
        
        // 收集所有像素
        val pixels = IntArray(scaledBitmap.width * scaledBitmap.height)
        scaledBitmap.getPixels(pixels, 0, scaledBitmap.width, 0, 0, scaledBitmap.width, scaledBitmap.height)
        
        // 转换为Lab颜色空间（更好的颜色距离计算）
        val labPixels = pixels.map { pixel ->
            val r = android.graphics.Color.red(pixel) / 255.0
            val g = android.graphics.Color.green(pixel) / 255.0
            val b = android.graphics.Color.blue(pixel) / 255.0
            val lab = DoubleArray(3)
            ColorUtils.RGBToLAB((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt(), lab)
            lab
        }
        
        // 简单版颜色聚类（取平均值）
        val step = labPixels.size / colorCount
        val dominantColors = mutableListOf<Color>()
        
        for (i in 0 until colorCount) {
            val start = i * step
            val end = if (i == colorCount - 1) labPixels.size else (i + 1) * step
            val slice = labPixels.slice(start until end)
            
            if (slice.isNotEmpty()) {
                // 计算平均Lab值
                val avgL = slice.map { it[0] }.average()
                val avgA = slice.map { it[1] }.average()
                val avgB = slice.map { it[2] }.average()
                
                // 转换回RGB
                val colorInt = ColorUtils.LABToColor(avgL, avgA, avgB)

                // 创建Color对象
                dominantColors.add(Color(colorInt))
            }
        }
        
        // 按亮度排序（最亮的在前）
        return dominantColors.sortedByDescending { color ->
            ColorUtils.calculateLuminance(color.toArgb())
        }
    }
    
    /**
     * 计算颜色的感知亮度
     */
    private fun calculatePerceivedBrightness(color: Color): Double {
        val rgb = color.toArgb()
        val r = android.graphics.Color.red(rgb) / 255.0
        val g = android.graphics.Color.green(rgb) / 255.0
        val b = android.graphics.Color.blue(rgb) / 255.0
        
        // 使用ITU-R BT.709亮度公式
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
    
    /**
     * 调整颜色以确保可读性（根据WCAG对比度标准）
     * 
     * @param color 要调整的颜色
     * @param backgroundColor 背景颜色
     * @param minContrastRatio 最小对比度比率（默认4.5:1，WCAG AA标准）
     * @return 调整后的颜色
     */
    fun adjustColorForReadability(
        color: Color,
        backgroundColor: Color,
        minContrastRatio: Double = 4.5
    ): Color {
        var currentColor = color
        var attempts = 0
        val maxAttempts = 20
        
        while (attempts < maxAttempts) {
            val contrast = calculateContrastRatio(currentColor, backgroundColor)
            if (contrast >= minContrastRatio) {
                return currentColor
            }
            
            // 调整颜色（增加与背景色的差异）
            currentColor = adjustColorTowardsContrast(currentColor, backgroundColor)
            attempts++
        }
        
        // 如果无法达到要求的对比度，返回最大差异的颜色
        return if (calculatePerceivedBrightness(backgroundColor) > 0.5) {
            Color.Black // 浅色背景使用黑色
        } else {
            Color.White // 深色背景使用白色
        }
    }
    
    /**
     * 计算两个颜色之间的对比度比率
     */
    private fun calculateContrastRatio(color1: Color, color2: Color): Double {
        val luminance1 = calculateRelativeLuminance(color1)
        val luminance2 = calculateRelativeLuminance(color2)
        
        val lighter = maxOf(luminance1, luminance2)
        val darker = minOf(luminance1, luminance2)
        
        return (lighter + 0.05) / (darker + 0.05)
    }
    
    /**
     * 计算相对亮度（WCAG标准）
     */
    private fun calculateRelativeLuminance(color: Color): Double {
        val rgb = color.toArgb()
        val r = android.graphics.Color.red(rgb) / 255.0
        val g = android.graphics.Color.green(rgb) / 255.0
        val b = android.graphics.Color.blue(rgb) / 255.0
        
        // 应用伽马校正
        val rLinear = if (r <= 0.03928) r / 12.92 else ((r + 0.055) / 1.055).pow(2.4)
        val gLinear = if (g <= 0.03928) g / 12.92 else ((g + 0.055) / 1.055).pow(2.4)
        val bLinear = if (b <= 0.03928) b / 12.92 else ((b + 0.055) / 1.055).pow(2.4)
        
        return 0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear
    }
    
    /**
     * 调整颜色以提高对比度
     */
    private fun adjustColorTowardsContrast(color: Color, backgroundColor: Color): Color {
        val brightnessColor = calculatePerceivedBrightness(color)
        val brightnessBackground = calculatePerceivedBrightness(backgroundColor)
        
        return if (brightnessColor > brightnessBackground) {
            // 当前颜色比背景亮，使其更亮
            lightenColor(color, 0.1f)
        } else {
            // 当前颜色比背景暗，使其更暗
            darkenColor(color, 0.1f)
        }
    }
    
    /**
     * 变亮颜色
     */
    private fun lightenColor(color: Color, factor: Float): Color {
        val rgb = color.toArgb()
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            android.graphics.Color.red(rgb),
            android.graphics.Color.green(rgb),
            android.graphics.Color.blue(rgb),
            hsv
        )
        
        hsv[2] = (hsv[2] * (1 + factor)).coerceAtMost(1f) // 增加亮度
        
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
    
    /**
     * 变暗颜色
     */
    private fun darkenColor(color: Color, factor: Float): Color {
        val rgb = color.toArgb()
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            android.graphics.Color.red(rgb),
            android.graphics.Color.green(rgb),
            android.graphics.Color.blue(rgb),
            hsv
        )
        
        hsv[2] = (hsv[2] * (1 - factor)).coerceAtLeast(0f) // 降低亮度
        
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
    
    /**
     * 扩展函数：Double的幂运算
     */
    private fun Double.pow(exponent: Double): Double {
        return Math.pow(this, exponent)
    }
}