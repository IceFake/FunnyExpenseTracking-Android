package com.example.funnyexpensetracking.ui.transaction

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.funnyexpensetracking.R
import com.example.funnyexpensetracking.domain.model.DailyTransactions
import com.example.funnyexpensetracking.domain.model.Transaction
import com.example.funnyexpensetracking.domain.model.TransactionType
import com.example.funnyexpensetracking.util.CurrencyUtil

/**
 * 按日期分组的交易记录适配器
 */
class DailyTransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit,
    private val onTransactionLongClick: (Transaction) -> Unit
) : ListAdapter<DailyTransactions, DailyTransactionAdapter.DailyViewHolder>(DailyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_daily_transactions, parent, false)
        return DailyViewHolder(view)
    }

    override fun onBindViewHolder(holder: DailyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DailyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvDayOfWeek: TextView = itemView.findViewById(R.id.tvDayOfWeek)
        private val tvDayIncome: TextView = itemView.findViewById(R.id.tvDayIncome)
        private val tvDayExpense: TextView = itemView.findViewById(R.id.tvDayExpense)
        private val llTransactions: LinearLayout = itemView.findViewById(R.id.llTransactions)

        fun bind(dailyTransactions: DailyTransactions) {
            tvDate.text = dailyTransactions.dateString
            tvDayOfWeek.text = dailyTransactions.dayOfWeek

            tvDayIncome.text = "收入:${CurrencyUtil.formatCurrency(dailyTransactions.totalIncome)}"
            tvDayExpense.text = "支出:${CurrencyUtil.formatCurrency(dailyTransactions.totalExpense)}"

            // 隐藏收入/支出为0的显示
            tvDayIncome.visibility = if (dailyTransactions.totalIncome > 0) View.VISIBLE else View.GONE
            tvDayExpense.visibility = if (dailyTransactions.totalExpense > 0) View.VISIBLE else View.GONE

            // 清除旧的子视图
            llTransactions.removeAllViews()

            // 添加交易记录子视图
            dailyTransactions.transactions.forEach { transaction ->
                val transactionView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_transaction, llTransactions, false)
                bindTransactionView(transactionView, transaction)
                llTransactions.addView(transactionView)
            }
        }

        private fun bindTransactionView(view: View, transaction: Transaction) {
            val tvCategoryIcon: TextView = view.findViewById(R.id.tvCategoryIcon)
            val tvCategory: TextView = view.findViewById(R.id.tvCategory)
            val tvNote: TextView = view.findViewById(R.id.tvNote)
            val tvAccount: TextView = view.findViewById(R.id.tvAccount)
            val tvAmount: TextView = view.findViewById(R.id.tvAmount)

            // 设置分类图标
            tvCategoryIcon.text = getCategoryIcon(transaction.category)
            tvCategory.text = transaction.category

            // 备注
            if (transaction.note.isNotEmpty()) {
                tvNote.text = transaction.note
                tvNote.visibility = View.VISIBLE
            } else {
                tvNote.visibility = View.GONE
            }

            // 账户
            tvAccount.text = transaction.accountName

            // 金额
            val amountText = if (transaction.type == TransactionType.INCOME) {
                "+${CurrencyUtil.formatCurrency(transaction.amount)}"
            } else {
                "-${CurrencyUtil.formatCurrency(transaction.amount)}"
            }
            tvAmount.text = amountText
            tvAmount.setTextColor(
                if (transaction.type == TransactionType.INCOME)
                    0xFF4CAF50.toInt()
                else
                    0xFFFF5722.toInt()
            )

            // 点击事件
            view.setOnClickListener { onTransactionClick(transaction) }
            view.setOnLongClickListener {
                onTransactionLongClick(transaction)
                true
            }
        }

        private fun getCategoryIcon(category: String): String {
            return when (category) {
                "餐饮" -> "🍚"
                "交通" -> "🚗"
                "购物" -> "🛒"
                "娱乐" -> "🎮"
                "医疗" -> "💊"
                "教育" -> "📚"
                "居住" -> "🏠"
                "通讯" -> "📱"
                "服饰" -> "👔"
                "工资" -> "💰"
                "奖金" -> "🎁"
                "投资收益" -> "📈"
                "兼职" -> "💼"
                "红包" -> "🧧"
                "退款" -> "↩️"
                else -> "📝"
            }
        }
    }

    class DailyDiffCallback : DiffUtil.ItemCallback<DailyTransactions>() {
        override fun areItemsTheSame(oldItem: DailyTransactions, newItem: DailyTransactions): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: DailyTransactions, newItem: DailyTransactions): Boolean {
            return oldItem == newItem
        }
    }
}

