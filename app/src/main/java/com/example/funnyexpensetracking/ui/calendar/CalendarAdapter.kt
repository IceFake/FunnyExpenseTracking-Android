package com.example.funnyexpensetracking.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.funnyexpensetracking.R
import com.example.funnyexpensetracking.util.CurrencyUtil
import java.util.*

/**
 * 日历适配器
 */
class CalendarAdapter(
    private val onDateClick: (Int, Int, Int) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private var year: Int = 0
    private var month: Int = 0
    private var calendarDays: List<CalendarDay> = emptyList()
    private var dailyBalanceMap: Map<Long, Double> = emptyMap()

    /**
     * 日历日期数据类
     */
    data class CalendarDay(
        val year: Int,
        val month: Int,
        val day: Int,
        val isCurrentMonth: Boolean,
        val timestamp: Long
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        holder.bind(calendarDays[position])
    }

    override fun getItemCount(): Int = calendarDays.size

    /**
     * 设置日历数据
     */
    fun setCalendarData(year: Int, month: Int, dailyBalanceMap: Map<Long, Double>) {
        // 确保年月有效
        if (year <= 0 || month <= 0 || month > 12) {
            return
        }
        this.year = year
        this.month = month
        this.dailyBalanceMap = dailyBalanceMap
        this.calendarDays = generateCalendarDays(year, month)
        notifyDataSetChanged()
    }

    /**
     * 生成日历日期列表（包括上月末尾和下月开头的日期）
     */
    private fun generateCalendarDays(year: Int, month: Int): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()
        val calendar = Calendar.getInstance()

        // 设置为当月1号
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // 获取本月1号是星期几（1=周日，2=周一，...，7=周六）
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // 添加上月的日期（填充到周一开始）
        val daysToAdd = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
        calendar.add(Calendar.DAY_OF_MONTH, -daysToAdd)

        // 生成42天（6周）
        for (i in 0 until 42) {
            val dayYear = calendar.get(Calendar.YEAR)
            val dayMonth = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val timestamp = calendar.timeInMillis

            days.add(
                CalendarDay(
                    year = dayYear,
                    month = dayMonth,
                    day = day,
                    isCurrentMonth = dayMonth == month,
                    timestamp = timestamp
                )
            )

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return days
    }

    inner class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        private val tvBalance: TextView = itemView.findViewById(R.id.tvBalance)

        fun bind(calendarDay: CalendarDay) {
            // 显示日期
            tvDay.text = calendarDay.day.toString()

            // 设置当月/非当月的样式
            if (calendarDay.isCurrentMonth) {
                tvDay.alpha = 1.0f
                tvBalance.alpha = 1.0f
            } else {
                tvDay.alpha = 0.3f
                tvBalance.alpha = 0.3f
            }

            // 获取当日收支差额
            val balance = dailyBalanceMap[calendarDay.timestamp] ?: 0.0

            // 显示收支差额
            if (balance != 0.0) {
                val balanceText = if (balance > 0) {
                    "+${CurrencyUtil.formatCurrency(balance)}"
                } else {
                    CurrencyUtil.formatCurrency(balance)
                }
                tvBalance.text = balanceText
                tvBalance.visibility = View.VISIBLE

                // 设置颜色
                tvBalance.setTextColor(
                    if (balance > 0)
                        0xFF4CAF50.toInt() // 绿色表示盈余
                    else
                        0xFFFF5722.toInt() // 红色表示亏损
                )
            } else {
                // 无收支数据时显示空文本，保持布局高度一致
                tvBalance.text = ""
                tvBalance.visibility = View.VISIBLE
            }

            // 点击事件
            itemView.setOnClickListener {
                if (calendarDay.isCurrentMonth) {
                    onDateClick(calendarDay.year, calendarDay.month, calendarDay.day)
                }
            }

            // 高亮今天
            val today = Calendar.getInstance()
            val isToday = calendarDay.year == today.get(Calendar.YEAR) &&
                    calendarDay.month == today.get(Calendar.MONTH) + 1 &&
                    calendarDay.day == today.get(Calendar.DAY_OF_MONTH)

            if (isToday && calendarDay.isCurrentMonth) {
                itemView.setBackgroundResource(R.drawable.bg_calendar_today)
            } else {
                itemView.setBackgroundResource(R.drawable.bg_calendar_day)
            }
        }
    }
}

