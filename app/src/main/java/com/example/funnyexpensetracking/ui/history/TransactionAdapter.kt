package com.example.funnyexpensetracking.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.funnyexpensetracking.R
import com.example.funnyexpensetracking.domain.model.Transaction
import com.example.funnyexpensetracking.domain.model.TransactionType
import com.example.funnyexpensetracking.util.CurrencyUtil

/**
 * 交易记录适配器
 */
class TransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit,
    private val onTransactionLongClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoryIcon: TextView = itemView.findViewById(R.id.tvCategoryIcon)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        private val tvAccount: TextView = itemView.findViewById(R.id.tvAccount)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)

        fun bind(transaction: Transaction) {
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
            itemView.setOnClickListener { onTransactionClick(transaction) }
            itemView.setOnLongClickListener {
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

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}

