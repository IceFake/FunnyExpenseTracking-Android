package com.example.funnyexpensetracking.ui.transaction

import android.app.DatePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.example.funnyexpensetracking.R
import com.example.funnyexpensetracking.domain.model.Account
import com.example.funnyexpensetracking.domain.model.Transaction
import com.example.funnyexpensetracking.domain.model.TransactionType
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.*

/**
 * 添加/编辑交易记录的底部弹窗
 */
class AddTransactionBottomSheet(
    context: Context,
    private val accounts: List<Account>,
    private val editingTransaction: Transaction?,
    private val onSave: (amount: Double, type: TransactionType, category: String, accountId: Long, note: String, date: Long) -> Unit,
    private val onDismiss: () -> Unit,
    private val onAddAccount: () -> Unit
) : BottomSheetDialog(context) {

    private val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)

    private var selectedType: TransactionType = TransactionType.EXPENSE
    private var selectedCategory: String = ""
    private var selectedAccountId: Long = 0
    private var selectedDate: Long = System.currentTimeMillis()

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_transaction, null)
        setContentView(view)

        setupViews(view)

        // 如果是编辑模式，填充数据
        editingTransaction?.let { fillEditingData(view, it) }

        setOnDismissListener { onDismiss() }
    }

    private fun setupViews(view: View) {
        val toggleType = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleType)
        val btnExpense = view.findViewById<MaterialButton>(R.id.btnExpense)
        val btnIncome = view.findViewById<MaterialButton>(R.id.btnIncome)
        val tilAmount = view.findViewById<TextInputLayout>(R.id.tilAmount)
        val etAmount = view.findViewById<TextInputEditText>(R.id.etAmount)
        val chipGroupCategory = view.findViewById<ChipGroup>(R.id.chipGroupCategory)
        val chipGroupAccount = view.findViewById<ChipGroup>(R.id.chipGroupAccount)
        val btnAddAccount = view.findViewById<MaterialButton>(R.id.btnAddAccount)
        val etDate = view.findViewById<TextInputEditText>(R.id.etDate)
        val etNote = view.findViewById<TextInputEditText>(R.id.etNote)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)

        // 设置默认日期
        etDate.setText(dateFormat.format(Date(selectedDate)))

        // 类型切换
        toggleType.check(R.id.btnExpense)
        toggleType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedType = if (checkedId == R.id.btnIncome) TransactionType.INCOME else TransactionType.EXPENSE
                updateCategoryChips(chipGroupCategory)
            }
        }

        // 初始化分类
        updateCategoryChips(chipGroupCategory)

        // 初始化账户
        updateAccountChips(chipGroupAccount)

        // 添加账户按钮
        btnAddAccount.setOnClickListener {
            onAddAccount()
        }

        // 日期选择
        etDate.setOnClickListener {
            showDatePicker(etDate)
        }

        // 保存按钮
        btnSave.setOnClickListener {
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

            if (selectedCategory.isEmpty()) {
                android.widget.Toast.makeText(context, "请选择分类", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedAccountId == 0L) {
                android.widget.Toast.makeText(context, "请选择账户", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val note = etNote.text?.toString() ?: ""

            onSave(amount, selectedType, selectedCategory, selectedAccountId, note, selectedDate)
            dismiss()
        }
    }

    private fun updateCategoryChips(chipGroup: ChipGroup) {
        chipGroup.removeAllViews()

        val categories = if (selectedType == TransactionType.INCOME) {
            INCOME_CATEGORIES
        } else {
            EXPENSE_CATEGORIES
        }

        categories.forEach { category ->
            val chip = Chip(context).apply {
                text = "${getCategoryIcon(category)} $category"
                isCheckable = true
                isCheckedIconVisible = false
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedCategory = category
                    }
                }
            }
            chipGroup.addView(chip)

            // 如果是编辑模式且分类匹配，选中
            if (category == selectedCategory) {
                chip.isChecked = true
            }
        }
    }

    private fun updateAccountChips(chipGroup: ChipGroup) {
        chipGroup.removeAllViews()

        if (accounts.isEmpty()) {
            return
        }

        accounts.forEach { account ->
            val chip = Chip(context).apply {
                text = "${account.name}"
                isCheckable = true
                isCheckedIconVisible = false
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedAccountId = account.id
                    }
                }
            }
            chipGroup.addView(chip)

            // 默认选中第一个或默认账户
            if (account.isDefault || (selectedAccountId == 0L && accounts.indexOf(account) == 0)) {
                chip.isChecked = true
                selectedAccountId = account.id
            }

            // 如果是编辑模式且账户匹配，选中
            if (account.id == selectedAccountId) {
                chip.isChecked = true
            }
        }
    }

    private fun showDatePicker(etDate: TextInputEditText) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDate

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = calendar.timeInMillis
                etDate.setText(dateFormat.format(Date(selectedDate)))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun fillEditingData(view: View, transaction: Transaction) {
        val toggleType = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleType)
        val etAmount = view.findViewById<TextInputEditText>(R.id.etAmount)
        val etDate = view.findViewById<TextInputEditText>(R.id.etDate)
        val etNote = view.findViewById<TextInputEditText>(R.id.etNote)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)

        selectedType = transaction.type
        selectedCategory = transaction.category
        selectedAccountId = transaction.accountId
        selectedDate = transaction.date

        toggleType.check(if (transaction.type == TransactionType.INCOME) R.id.btnIncome else R.id.btnExpense)
        etAmount.setText(transaction.amount.toString())
        etDate.setText(dateFormat.format(Date(transaction.date)))
        etNote.setText(transaction.note)
        btnSave.text = "更新"
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

