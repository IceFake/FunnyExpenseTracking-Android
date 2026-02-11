package com.example.funnyexpensetracking.ui.transaction

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.example.funnyexpensetracking.R
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
 * 添加固定收支的底部弹窗
 *
 * 必填项：名称、金额、类型、频率、开始日期
 * 选填项：结束日期
 *
 * 条目新增后不可编辑
 */
class AddFixedIncomeBottomSheet(
    context: Context,
    private val onSave: (name: String, amount: Double, type: FixedIncomeType, frequency: FixedIncomeFrequency, startDate: Long, endDate: Long?) -> Unit,
    private val onDismiss: () -> Unit
) : BottomSheetDialog(context) {

    private val dateTimeFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.CHINA)
    private val decimalFormat = DecimalFormat("0.0000")

    private var selectedType: FixedIncomeType = FixedIncomeType.EXPENSE
    private var selectedFrequency: FixedIncomeFrequency = FixedIncomeFrequency.MONTHLY
    private var selectedStartDate: Long = getCurrentMinuteTimestamp()
    private var selectedEndDate: Long? = null

    private lateinit var tvPerMinute: TextView
    private lateinit var etAmount: TextInputEditText

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_fixed_income, null)
        setContentView(view)

        setupViews(view)
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
        val tilEndDate = view.findViewById<TextInputLayout>(R.id.tilEndDate)
        val etEndDate = view.findViewById<TextInputEditText>(R.id.etEndDate)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)

        // 隐藏累计金额输入框（如果存在）
        view.findViewById<View>(R.id.tilAccumulatedAmount)?.visibility = View.GONE

        // 设置默认开始日期
        etStartDate.setText(dateTimeFormat.format(Date(selectedStartDate)))

        // 设置结束日期提示
        etEndDate?.hint = "可选，留空表示持续"

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

        // 开始日期选择（精确到分钟）
        etStartDate.setOnClickListener {
            showDateTimePicker(selectedStartDate) { timestamp ->
                selectedStartDate = timestamp
                etStartDate.setText(dateTimeFormat.format(Date(selectedStartDate)))
            }
        }

        // 结束日期选择（精确到分钟）
        etEndDate?.setOnClickListener {
            val initialTime = selectedEndDate ?: (selectedStartDate + 30L * 24 * 60 * 60 * 1000) // 默认开始日期后30天
            showDateTimePicker(initialTime) { timestamp ->
                selectedEndDate = timestamp
                etEndDate.setText(dateTimeFormat.format(Date(timestamp)))
            }
        }

        // 清除结束日期按钮（长按清除）
        etEndDate?.setOnLongClickListener {
            selectedEndDate = null
            etEndDate.setText("")
            true
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

            // 验证结束日期（如果设置了，必须晚于开始日期）
            if (selectedEndDate != null && selectedEndDate!! <= selectedStartDate) {
                tilEndDate?.error = "结束日期必须晚于开始日期"
                return@setOnClickListener
            }
            tilEndDate?.error = null

            onSave(name, amount, selectedType, selectedFrequency, selectedStartDate, selectedEndDate)
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

    /**
     * 显示日期时间选择器（精确到分钟）
     */
    private fun showDateTimePicker(initialTime: Long, onSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = initialTime

        // 先选日期
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)

                // 再选时间
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        onSelected(calendar.timeInMillis)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true // 24小时制
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * 获取当前分钟的时间戳（精确到分钟）
     */
    private fun getCurrentMinuteTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

