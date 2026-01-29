package com.example.funnyexpensetracking.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期时间工具类
 */
object DateTimeUtil {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    /**
     * 获取今天的开始时间戳（00:00:00）
     */
    fun getTodayStartTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * 获取今天的结束时间戳（23:59:59）
     */
    fun getTodayEndTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /**
     * 获取本月第一天的开始时间戳
     */
    fun getMonthStartTimestamp(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * 获取本月最后一天的结束时间戳
     */
    fun getMonthEndTimestamp(year: Int, month: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /**
     * 获取本年第一天的开始时间戳
     */
    fun getYearStartTimestamp(year: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, 0, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * 获取本年最后一天的结束时间戳
     */
    fun getYearEndTimestamp(year: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, 11, 31, 23, 59, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /**
     * 格式化日期
     */
    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    /**
     * 格式化日期时间
     */
    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }

    /**
     * 格式化月份
     */
    fun formatMonth(timestamp: Long): String {
        return monthFormat.format(Date(timestamp))
    }

    /**
     * 获取当前年份
     */
    fun getCurrentYear(): Int {
        return Calendar.getInstance().get(Calendar.YEAR)
    }

    /**
     * 获取当前月份（1-12）
     */
    fun getCurrentMonth(): Int {
        return Calendar.getInstance().get(Calendar.MONTH) + 1
    }

    /**
     * 计算两个时间戳之间的分钟数
     */
    fun getMinutesBetween(startTime: Long, endTime: Long): Long {
        return (endTime - startTime) / (1000 * 60)
    }
}

