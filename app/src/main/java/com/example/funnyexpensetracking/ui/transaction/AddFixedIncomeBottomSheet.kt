package com.example.funnyexpensetracking.ui.transaction

import android.app.DatePickerDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.example.funnyexpensetracking.R
import com.example.funnyexpensetracking.domain.model.FixedIncome
import com.example.funnyexpensetracking.domain.model.FixedIncomeFrequency
import com.example.funnyexpensetracking.domain.model.FixedIncomeType
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * 添加/编辑固定收支的底部弹窗
 * 固定收支以月/周/日/年为单位，系统会计算每分钟的平均值来实时更新资产
 */
class AddFixedIncomeBottomSheet(
    context: Context,
    private val editingFixedIncome: FixedIncome? = null,
    private val onSave: (name: String, amount: Double, type: FixedIncomeType, frequency: FixedIncomeFrequency, startDate: Long) -> Unit,
    private val onDismiss: () -> Unit
) : BottomSheetDialog(context) {

    private val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)
    private val decimalFormat = DecimalFormat("0.0000")

    private var selectedType: FixedIncomeType = FixedIncomeType.EXPENSE
    private var selectedFrequency: FixedIncomeFrequency = FixedIncomeFrequency.MONTHLY
    private var selectedStartDate: Long = System.currentTimeMillis()

    private lateinit var tvPerMinute: TextView
    private lateinit var etAmount: TextInputEditText

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_fixed_income, null)
        setContentView(view)

        setupViews(view)

        // 如果是编辑模式，填充数据
        editingFixedIncome?.let { fillEditingData(view, it) }

        setOnDismissListener { onDismiss() }
    }

    private fun setupViews(view: View) {
        val toggleType = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleType)
        val tilName = view.findViewById<TextInputLayout>(R.id.tilName)
        val etName = view.findViewById<TextInputEditText>(R.id.etName)
        val tilAmount = view.findViewById<TextInputLayout>(R.id.tilAmount)
        etAmount = view.findViewById(R.id.etAmount)
        val chipGroupFrequency = view.findViewById<ChipGroup>(R.id.chipGroupFrequency)
        tvPerMinute = view.findViewById(R.id.tvPerMinute)
        val etStartDate = view.findViewById<TextInputEditText>(R.id.etStartDate)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)

        // 设置默认开始日期
        etStartDate.setText(dateFormat.format(Date(selectedStartDate)))

        // 类型切换
        toggleType.check(R.id.btnExpense)
        toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedType = if (checkedId == R.id.btnIncome) FixedIncomeType.INCOME else FixedIncomeType.EXPENSE
                updatePerMinuteDisplay()
            }
        }

        // 金额变化监听
        etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updatePerMinuteDisplay()
            }
        })

        // 频率选择
        chipGroupFrequency.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                selectedFrequency = when (checkedIds[0]) {
                    R.id.chipDaily -> FixedIncomeFrequency.DAILY
                    R.id.chipWeekly -> FixedIncomeFrequency.WEEKLY
                    R.id.chipMonthly -> FixedIncomeFrequency.MONTHLY
                    R.id.chipYearly -> FixedIncomeFrequency.YEARLY
                    else -> FixedIncomeFrequency.MONTHLY
                }
                updatePerMinuteDisplay()
            }
        }

        // 开始日期选择
        etStartDate.setOnClickListener {
            showDatePicker(etStartDate)
        }

        // 保存按钮
        btnSave.setOnClickListener {
            // 验证名称
            val name = etName.text?.toString()?.trim()
            if (name.isNullOrEmpty()) {
                tilName.error = "请输入名称"
                return@setOnClickListener
            }
            tilName.error = null

            // 验证金额
            val amountText = etAmount.text?.toString()
            if (amountText.isNullOrEmpty()) {
                tilAmount.error = "请输入金额"
                return@setOnClickListener
            }
            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                tilAmount.error = "请输入有效金额"
                return@setOnClickListener
            }
            tilAmount.error = null

            onSave(name, amount, selectedType, selectedFrequency, selectedStartDate)
            dismiss()
        }

        // 初始化每分钟显示
        updatePerMinuteDisplay()
    }

    private fun updatePerMinuteDisplay() {
        val amountText = etAmount.text?.toString()
        val amount = amountText?.toDoubleOrNull() ?: 0.0

        val perMinute = amount * selectedFrequency.toMinuteMultiplier()
        val formattedAmount = decimalFormat.format(perMinute)

        if (selectedType == FixedIncomeType.INCOME) {
            tvPerMinute.text = "+¥$formattedAmount"
            tvPerMinute.setTextColor(context.getColor(android.R.color.holo_green_dark))
        } else {
            tvPerMinute.text = "-¥$formattedAmount"
            tvPerMinute.setTextColor(context.getColor(android.R.color.holo_red_dark))
        }
    }

    private fun showDatePicker(etDate: TextInputEditText) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedStartDate

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedStartDate = calendar.timeInMillis
                etDate.setText(dateFormat.format(Date(selectedStartDate)))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun fillEditingData(view: View, fixedIncome: FixedIncome) {
        val toggleType = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleType)
        val etName = view.findViewById<TextInputEditText>(R.id.etName)
        val etAmount = view.findViewById<TextInputEditText>(R.id.etAmount)
        val chipGroupFrequency = view.findViewById<ChipGroup>(R.id.chipGroupFrequency)
        val etStartDate = view.findViewById<TextInputEditText>(R.id.etStartDate)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)

        selectedType = fixedIncome.type
        selectedFrequency = fixedIncome.frequency
        selectedStartDate = fixedIncome.startDate

        // 设置类型
        toggleType.check(if (fixedIncome.type == FixedIncomeType.INCOME) R.id.btnIncome else R.id.btnExpense)

        // 设置名称和金额
        etName.setText(fixedIncome.name)
        etAmount.setText(fixedIncome.amount.toString())

        // 设置频率
        val frequencyChipId = when (fixedIncome.frequency) {
            FixedIncomeFrequency.DAILY -> R.id.chipDaily
            FixedIncomeFrequency.WEEKLY -> R.id.chipWeekly
            FixedIncomeFrequency.MONTHLY -> R.id.chipMonthly
            FixedIncomeFrequency.YEARLY -> R.id.chipYearly
        }
        chipGroupFrequency.check(frequencyChipId)

        // 设置开始日期
        etStartDate.setText(dateFormat.format(Date(fixedIncome.startDate)))

        // 更改按钮文字
        btnSave.text = "更新"

        // 更新每分钟显示
        updatePerMinuteDisplay()
    }

    private fun FixedIncomeFrequency.toMinuteMultiplier(): Double {
        return when (this) {
            FixedIncomeFrequency.DAILY -> 1.0 / (24 * 60)
            FixedIncomeFrequency.WEEKLY -> 1.0 / (7 * 24 * 60)
            FixedIncomeFrequency.MONTHLY -> 1.0 / (30 * 24 * 60)
            FixedIncomeFrequency.YEARLY -> 1.0 / (365 * 24 * 60)
        }
    }
}

