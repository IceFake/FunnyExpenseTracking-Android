package com.example.funnyexpensetracking.ui.fixedincome

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.funnyexpensetracking.R
import com.example.funnyexpensetracking.domain.model.FixedIncome
import com.example.funnyexpensetracking.domain.model.FixedIncomeFrequency
import com.example.funnyexpensetracking.domain.model.FixedIncomeType
import java.text.DecimalFormat

/**
 * 固定收支列表适配器
 */
class FixedIncomeAdapter(
    private val onItemClick: (FixedIncome) -> Unit,
    private val onItemLongClick: (FixedIncome) -> Unit
) : ListAdapter<FixedIncome, FixedIncomeAdapter.ViewHolder>(DiffCallback()) {

    private val currencyFormat = DecimalFormat("#,##0.00")
    private val perMinuteFormat = DecimalFormat("0.0000")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fixed_income, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIcon: TextView = itemView.findViewById(R.id.tvIcon)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvFrequency: TextView = itemView.findViewById(R.id.tvFrequency)
        private val tvPerMinute: TextView = itemView.findViewById(R.id.tvPerMinute)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvAccumulatedAmount: TextView = itemView.findViewById(R.id.tvAccumulatedAmount)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        fun bind(item: FixedIncome) {
            // 设置图标
            tvIcon.text = if (item.type == FixedIncomeType.INCOME) "💰" else "💸"

            // 设置名称
            tvName.text = item.name

            // 设置频率
            tvFrequency.text = when (item.frequency) {
                FixedIncomeFrequency.DAILY -> "每日"
                FixedIncomeFrequency.WEEKLY -> "每周"
                FixedIncomeFrequency.MONTHLY -> "每月"
                FixedIncomeFrequency.YEARLY -> "每年"
            }

            // 设置每分钟金额
            val perMinute = item.getAmountPerMinute()
            tvPerMinute.text = "≈ ¥${perMinuteFormat.format(perMinute)}/分钟"

            // 设置金额
            val formattedAmount = currencyFormat.format(item.amount)
            if (item.type == FixedIncomeType.INCOME) {
                tvAmount.text = "+¥$formattedAmount"
                tvAmount.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
            } else {
                tvAmount.text = "-¥$formattedAmount"
                tvAmount.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
            }

            // 设置累计信息（时间 + 金额）
            if (item.accumulatedMinutes > 0 || item.accumulatedAmount > 0) {
                tvAccumulatedAmount.visibility = View.VISIBLE
                val formattedAccumulated = currencyFormat.format(item.accumulatedAmount)
                val timeText = item.getFormattedAccumulatedTime()
                tvAccumulatedAmount.text = "累计: ¥$formattedAccumulated ($timeText)"
            } else {
                tvAccumulatedAmount.visibility = View.GONE
            }

            // 设置状态（根据当前时间判断是否处于生效期）
            val currentTime = System.currentTimeMillis()
            val isCurrentlyEffective = item.isEffectiveAt(currentTime)

            when {
                !item.isActive -> {
                    tvStatus.text = "已停用"
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
                    // 设置整体半透明效果
                    itemView.alpha = 0.6f
                }
                currentTime < item.startDate -> {
                    tvStatus.text = "未开始"
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                    itemView.alpha = 1.0f
                }
                item.endDate != null && currentTime > item.endDate -> {
                    tvStatus.text = "已结束"
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
                    itemView.alpha = 0.6f
                }
                else -> {
                    tvStatus.text = "生效中"
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                    itemView.alpha = 1.0f
                }
            }

            // 点击事件
            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FixedIncome>() {
        override fun areItemsTheSame(oldItem: FixedIncome, newItem: FixedIncome): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FixedIncome, newItem: FixedIncome): Boolean {
            return oldItem == newItem
        }
    }
}

