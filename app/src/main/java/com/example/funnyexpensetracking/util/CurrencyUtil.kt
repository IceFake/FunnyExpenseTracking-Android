package com.example.funnyexpensetracking.util

import java.text.DecimalFormat

/**
 * 货币格式化工具类
 */
object CurrencyUtil {

    private val currencyFormat = DecimalFormat("¥#,##0.00")
    private val percentFormat = DecimalFormat("0.00%")

    /**
     * 格式化金额为货币字符串
     */
    fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount)
    }

    /**
     * 格式化金额（带正负号）
     */
    fun formatCurrencyWithSign(amount: Double): String {
        val prefix = if (amount >= 0) "+" else ""
        return prefix + currencyFormat.format(amount)
    }

    /**
     * 格式化百分比
     */
    fun formatPercent(value: Double): String {
        return percentFormat.format(value / 100)
    }

    /**
     * 格式化百分比（带正负号）
     */
    fun formatPercentWithSign(value: Double): String {
        val prefix = if (value >= 0) "+" else ""
        return prefix + percentFormat.format(value / 100)
    }

    /**
     * 简化大数字显示（如：1.2万、3.5亿）
     */
    fun formatLargeNumber(amount: Double): String {
        return when {
            amount >= 100000000 -> String.format("%.2f亿", amount / 100000000)
            amount >= 10000 -> String.format("%.2f万", amount / 10000)
            else -> String.format("%.2f", amount)
        }
    }
}

