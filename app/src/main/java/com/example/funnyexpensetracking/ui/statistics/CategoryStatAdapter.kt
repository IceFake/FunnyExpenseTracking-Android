package com.example.funnyexpensetracking.ui.statistics

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.funnyexpensetracking.databinding.ItemCategoryStatBinding
import com.example.funnyexpensetracking.domain.model.CategoryStat
import com.example.funnyexpensetracking.util.CurrencyUtil

/**
 * 分类统计适配器
 */
class CategoryStatAdapter : ListAdapter<CategoryStatItem, CategoryStatAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryStatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemCategoryStatBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CategoryStatItem) {
            binding.tvCategoryName.text = item.categoryStat.category
            binding.tvAmount.text = CurrencyUtil.formatCurrency(item.categoryStat.amount)
            binding.tvPercentage.text = String.format("%.1f%%", item.categoryStat.percentage)

            // 设置颜色指示器
            val drawable = binding.viewColorIndicator.background as? GradientDrawable
                ?: GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                }
            drawable.setColor(item.color)
            binding.viewColorIndicator.background = drawable
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CategoryStatItem>() {
        override fun areItemsTheSame(oldItem: CategoryStatItem, newItem: CategoryStatItem): Boolean {
            return oldItem.categoryStat.category == newItem.categoryStat.category
        }

        override fun areContentsTheSame(oldItem: CategoryStatItem, newItem: CategoryStatItem): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * 分类统计项（带颜色）
 */
data class CategoryStatItem(
    val categoryStat: CategoryStat,
    val color: Int
)

