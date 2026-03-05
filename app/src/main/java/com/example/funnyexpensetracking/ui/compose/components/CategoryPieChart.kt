package com.example.funnyexpensetracking.ui.compose.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funnyexpensetracking.domain.model.CategoryStat
import com.example.funnyexpensetracking.util.CurrencyUtil
import kotlin.math.cos
import kotlin.math.sin

/**
 * 分类饼图组件
 * 
 * 使用Canvas绘制自定义饼图，支持动画和交互
 */
@Composable
fun CategoryPieChart(
    categoryStats: List<CategoryStat>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
    animationDuration: Int = 1000,
    showLabels: Boolean = true,
    onSliceClicked: ((CategoryStat) -> Unit)? = null
) {
    // 如果没有数据，显示空状态
    if (categoryStats.isEmpty()) {
        EmptyPieChart(modifier = modifier)
        return
    }
    
    // 计算总金额
    val totalAmount = categoryStats.sumOf { it.amount }
    
    // 动画进度（0到1）
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = animationDuration),
        label = "pieChartAnimation"
    )
    
    // 文本测量器
    val textMeasurer = rememberTextMeasurer()
    
    // 交互状态
    var hoveredSliceIndex by remember { mutableStateOf<Int?>(null) }
    
    // Hoist theme colors outside Canvas DrawScope
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val backgroundColor = MaterialTheme.colorScheme.background
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size.minDimension
            val center = Offset(canvasSize / 2, canvasSize / 2)
            val radius = canvasSize / 2 * 0.8f // 80%的可用空间
            val strokeWidthPx = strokeWidth.toPx()
            
            // 计算每个扇形的角度
            var startAngle = -90f // 从顶部开始
            
            categoryStats.forEachIndexed { index, stat ->
                // 计算扇形角度（基于百分比）
                val sweepAngle = (stat.percentage.toFloat() / 100f * 360f) * animationProgress

                // 选择颜色
                val color = colors.getOrElse(index) { primaryColor }

                // 绘制扇形
                drawPieSlice(
                    center = center,
                    radius = radius,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    color = color,
                    strokeWidth = strokeWidthPx,
                    isHovered = hoveredSliceIndex == index,
                    borderColor = surfaceColor
                )
                
                // 绘制标签（如果启用）
                if (showLabels && sweepAngle > 10f) { // 只给足够大的扇形添加标签
                    drawSliceLabel(
                        center = center,
                        radius = radius,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        label = "${stat.category}\n${String.format("%.1f", stat.percentage)}%",
                        color = onSurfaceColor,
                        textMeasurer = textMeasurer
                    )
                }
                
                // 更新起始角度
                startAngle += sweepAngle
            }
            
            // 绘制中心圆（饼图中间的空心）
            drawCircle(
                color = backgroundColor,
                radius = radius * 0.4f,
                center = center,
                style = Fill
            )
            
            // 绘制中心文本（总金额）
            drawCenterText(
                center = center,
                totalAmount = totalAmount,
                textMeasurer = textMeasurer,
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariantColor = onSurfaceVariantColor
            )
        }
        
        // 交互层（检测点击）
        if (onSliceClicked != null) {
            // TODO: 添加点击检测
        }
    }
}

/**
 * 绘制饼图扇形
 */
private fun DrawScope.drawPieSlice(
    center: Offset,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    color: Color,
    strokeWidth: Float,
    isHovered: Boolean,
    borderColor: Color
) {
    // 计算扇形的外半径（悬停时稍微放大）
    val sliceRadius = if (isHovered) radius * 1.05f else radius
    
    // 绘制扇形填充
    drawArc(
        color = color,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = true,
        topLeft = Offset(center.x - sliceRadius, center.y - sliceRadius),
        size = Size(sliceRadius * 2, sliceRadius * 2),
        style = Fill
    )
    
    // 绘制扇形边框
    drawArc(
        color = borderColor,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = true,
        topLeft = Offset(center.x - sliceRadius, center.y - sliceRadius),
        size = Size(sliceRadius * 2, sliceRadius * 2),
        style = Stroke(width = strokeWidth)
    )
}

/**
 * 绘制扇形标签
 */
private fun DrawScope.drawSliceLabel(
    center: Offset,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    label: String,
    color: Color,
    textMeasurer: TextMeasurer
) {
    // 计算标签位置（扇形中间，距离中心70%半径处）
    val labelRadius = radius * 0.7f
    val midAngle = startAngle + sweepAngle / 2
    val midAngleRad = Math.toRadians(midAngle.toDouble())
    
    val labelX = center.x + (labelRadius * cos(midAngleRad).toFloat())
    val labelY = center.y + (labelRadius * sin(midAngleRad).toFloat())
    
    // 测量文本
    val textStyle = TextStyle(
        color = color,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )
    
    val textLayoutResult = textMeasurer.measure(
        text = label,
        style = textStyle
    )
    
    // 绘制文本
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            labelX - textLayoutResult.size.width / 2,
            labelY - textLayoutResult.size.height / 2
        )
    )
}

/**
 * 绘制中心文本
 */
private fun DrawScope.drawCenterText(
    center: Offset,
    totalAmount: Double,
    textMeasurer: TextMeasurer,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color
) {
    // 格式化金额
    val formattedAmount = CurrencyUtil.formatCurrency(totalAmount)
    
    // 主要文本样式
    val amountTextStyle = TextStyle(
        color = onSurfaceColor,
        fontSize = 14.sp,
        lineHeight = 16.sp
    )
    
    val amountLayout = textMeasurer.measure(
        text = formattedAmount,
        style = amountTextStyle
    )
    
    // 绘制总金额
    drawText(
        textLayoutResult = amountLayout,
        topLeft = Offset(
            center.x - amountLayout.size.width / 2,
            center.y - amountLayout.size.height / 2 - 8f
        )
    )
    
    // 副标题文本
    val subtitleTextStyle = TextStyle(
        color = onSurfaceVariantColor,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )
    
    val subtitleLayout = textMeasurer.measure(
        text = "总计",
        style = subtitleTextStyle
    )
    
    // 绘制副标题
    drawText(
        textLayoutResult = subtitleLayout,
        topLeft = Offset(
            center.x - subtitleLayout.size.width / 2,
            center.y + amountLayout.size.height / 2
        )
    )
}

/**
 * 空饼图状态
 */
@Composable
private fun EmptyPieChart(modifier: Modifier = Modifier) {
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size.minDimension
            val center = Offset(canvasSize / 2, canvasSize / 2)
            val radius = canvasSize / 2 * 0.8f
            
            // 绘制灰色圆环
            drawCircle(
                color = surfaceVariantColor.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            
            // 绘制中心文本
            val textStyle = TextStyle(
                color = onSurfaceVariantColor,
                fontSize = 12.sp
            )
            
            val textLayout = textMeasurer.measure(
                text = "无数据",
                style = textStyle
            )
            
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(
                    center.x - textLayout.size.width / 2,
                    center.y - textLayout.size.height / 2
                )
            )
        }
    }
}

/**
 * 简单的饼图预览组件（用于小空间）
 */
@Composable
fun SimplePieChart(
    categoryStats: List<CategoryStat>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    // 如果没有数据，显示空状态
    if (categoryStats.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "无数据",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val canvasSize = size.minDimension
        val center = Offset(canvasSize / 2, canvasSize / 2)
        val radius = canvasSize / 2
        
        var startAngle = -90f
        
        categoryStats.forEachIndexed { index, stat ->
            val sweepAngle = stat.percentage.toFloat() / 100f * 360f
            val color = colors.getOrElse(index) { primaryColor }

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Fill
            )
            
            startAngle += sweepAngle
        }
    }
}