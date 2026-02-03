package com.example.funnyexpensetracking.ui.investment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.funnyexpensetracking.R
import com.example.funnyexpensetracking.domain.model.Investment
import com.example.funnyexpensetracking.domain.model.InvestmentCategory
import java.text.DecimalFormat

/**
 * 投资列表适配器
 */
class InvestmentAdapter(
    private val onItemClick: (Investment) -> Unit,
    private val onItemLongClick: (Investment) -> Unit
) : ListAdapter<Investment, InvestmentAdapter.ViewHolder>(DiffCallback()) {

    private val currencyFormat = DecimalFormat("#,##0.00")
    private val quantityFormat = DecimalFormat("#,##0.##")
    private val percentFormat = DecimalFormat("+0.00%;-0.00%")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_investment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIcon: TextView = itemView.findViewById(R.id.tvIcon)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val tvInvestment: TextView = itemView.findViewById(R.id.tvInvestment)
        private val tvCurrentValue: TextView = itemView.findViewById(R.id.tvCurrentValue)
        private val tvProfitLoss: TextView = itemView.findViewById(R.id.tvProfitLoss)

        fun bind(item: Investment) {
            // 设置图标
            tvIcon.text = if (item.category == InvestmentCategory.STOCK) "📈" else "💼"

            // 设置描述
            tvDescription.text = item.description

            // 设置分类
            tvCategory.text = when (item.category) {
                InvestmentCategory.STOCK -> "股票"
                InvestmentCategory.OTHER -> "其他"
            }

            // 设置数量（仅股票显示）
            if (item.category == InvestmentCategory.STOCK) {
                tvQuantity.visibility = View.VISIBLE
                tvQuantity.text = "持有: ${quantityFormat.format(item.quantity)}股"
            } else {
                tvQuantity.visibility = View.GONE
            }

            // 设置投入金额
            tvInvestment.text = "投入: ¥${currencyFormat.format(item.investment)}"

            // 设置当前价值
            val currentValue = item.calcCurrentValue()
            tvCurrentValue.text = "当前: ¥${currencyFormat.format(currentValue)}"

            // 设置盈亏
            val profitLoss = item.getProfitLoss()
            val profitLossPercent = item.getProfitLossPercent()

            if (profitLoss >= 0) {
                tvProfitLoss.text = "+¥${currencyFormat.format(profitLoss)} (${String.format("+%.2f%%", profitLossPercent)})"
                tvProfitLoss.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
            } else {
                tvProfitLoss.text = "-¥${currencyFormat.format(-profitLoss)} (${String.format("%.2f%%", profitLossPercent)})"
                tvProfitLoss.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
            }

            // 点击事件
            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Investment>() {
        override fun areItemsTheSame(oldItem: Investment, newItem: Investment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Investment, newItem: Investment): Boolean {
            return oldItem == newItem
        }
    }
}

