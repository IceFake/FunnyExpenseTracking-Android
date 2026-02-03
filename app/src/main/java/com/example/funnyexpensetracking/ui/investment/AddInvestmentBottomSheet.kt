package com.example.funnyexpensetracking.ui.investment

import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import com.example.funnyexpensetracking.databinding.DialogAddInvestmentBinding
import com.example.funnyexpensetracking.domain.model.Investment
import com.example.funnyexpensetracking.domain.model.InvestmentCategory
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * 添加/编辑投资条目的底部弹窗
 */
class AddInvestmentBottomSheet(
    context: Context,
    private val editingInvestment: Investment? = null,
    private val onSave: (category: InvestmentCategory, description: String, quantity: Double, investment: Double, currentValue: Double?) -> Unit,
    private val onDismiss: () -> Unit
) : BottomSheetDialog(context) {

    private val binding: DialogAddInvestmentBinding

    init {
        binding = DialogAddInvestmentBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        setupViews()
        setupListeners()

        setOnDismissListener {
            onDismiss()
        }
    }

    private fun setupViews() {
        // 设置分类下拉选择
        val categories = arrayOf("股票", "其他")
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(adapter)

        // 默认选择"其他"
        binding.spinnerCategory.setText(categories[1], false)
        updateUIForCategory(InvestmentCategory.OTHER)

        // 如果是编辑模式，填充数据
        editingInvestment?.let { investment ->
            binding.tvTitle.text = "编辑投资"

            val categoryIndex = when (investment.category) {
                InvestmentCategory.STOCK -> 0
                InvestmentCategory.OTHER -> 1
            }
            binding.spinnerCategory.setText(categories[categoryIndex], false)
            updateUIForCategory(investment.category)

            // 编辑模式下禁止修改分类
            binding.spinnerCategory.isEnabled = false
            binding.tilCategory.isEnabled = false

            binding.etDescription.setText(investment.description)
            binding.etQuantity.setText(investment.quantity.toString())
            binding.etInvestment.setText(investment.investment.toString())

            // 股票的当前价值不可编辑，显示提示
            if (investment.category == InvestmentCategory.STOCK) {
                binding.tilDescription.hint = "股票代码"
                binding.etDescription.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            } else {
                // 其他类型可以编辑当前价值
                binding.etCurrentValue.setText(investment.calcCurrentValue().toString())
            }
        }
    }

    private fun setupListeners() {
        // 分类选择监听
        binding.spinnerCategory.setOnItemClickListener { _, _, position, _ ->
            val category = when (position) {
                0 -> InvestmentCategory.STOCK
                else -> InvestmentCategory.OTHER
            }
            updateUIForCategory(category)
        }

        // 保存按钮
        binding.btnSave.setOnClickListener {
            saveInvestment()
        }

        // 取消按钮
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun updateUIForCategory(category: InvestmentCategory) {
        when (category) {
            InvestmentCategory.STOCK -> {
                binding.tilDescription.hint = "股票代码"
                binding.etDescription.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                binding.tilQuantity.visibility = View.VISIBLE
                binding.tilCurrentValue.visibility = View.GONE
            }
            InvestmentCategory.OTHER -> {
                binding.tilDescription.hint = "描述"
                binding.etDescription.inputType = InputType.TYPE_CLASS_TEXT
                binding.tilQuantity.visibility = View.GONE
                binding.etQuantity.setText("1")
                binding.tilCurrentValue.visibility = View.VISIBLE
            }
        }
    }

    private fun getSelectedCategory(): InvestmentCategory {
        return when (binding.spinnerCategory.text.toString()) {
            "股票" -> InvestmentCategory.STOCK
            else -> InvestmentCategory.OTHER
        }
    }

    private fun saveInvestment() {
        val category = getSelectedCategory()
        val description = binding.etDescription.text.toString().trim()
        val quantityStr = binding.etQuantity.text.toString().trim()
        val investmentStr = binding.etInvestment.text.toString().trim()
        val currentValueStr = binding.etCurrentValue.text.toString().trim()

        // 验证输入
        if (description.isEmpty()) {
            binding.tilDescription.error = if (category == InvestmentCategory.STOCK) "请输入股票代码" else "请输入描述"
            return
        }
        binding.tilDescription.error = null

        if (category == InvestmentCategory.STOCK && quantityStr.isEmpty()) {
            binding.tilQuantity.error = "请输入数量"
            return
        }
        binding.tilQuantity.error = null

        if (investmentStr.isEmpty()) {
            binding.tilInvestment.error = "请输入投入金额"
            return
        }
        binding.tilInvestment.error = null

        val quantity = if (category == InvestmentCategory.STOCK) {
            quantityStr.toDoubleOrNull() ?: 0.0
        } else {
            1.0
        }

        val investment = investmentStr.toDoubleOrNull()
        if (investment == null || investment <= 0) {
            binding.tilInvestment.error = "请输入有效金额"
            return
        }

        // 当前价值（其他类型可选）
        val currentValue: Double? = if (category == InvestmentCategory.OTHER && currentValueStr.isNotEmpty()) {
            currentValueStr.toDoubleOrNull()
        } else {
            null
        }

        onSave(category, description.uppercase(), quantity, investment, currentValue)
        dismiss()
    }
}

