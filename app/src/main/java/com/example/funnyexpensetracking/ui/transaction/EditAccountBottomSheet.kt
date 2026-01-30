package com.example.funnyexpensetracking.ui.transaction

import android.content.Context
import android.view.LayoutInflater
import com.example.funnyexpensetracking.R
import com.example.funnyexpensetracking.domain.model.Account
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.TextView
import java.text.DecimalFormat

/**
 * 编辑账户的底部弹窗
 * 支持修改账户名称和直接设置账户余额
 */
class EditAccountBottomSheet(
    context: Context,
    private val account: Account,
    private val onSave: (accountId: Long, name: String, balance: Double) -> Unit,
    private val onDelete: ((accountId: Long) -> Unit)? = null,
    private val onDismiss: () -> Unit
) : BottomSheetDialog(context) {

    private val currencyFormat = DecimalFormat("#,##0.00")

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_account, null)
        setContentView(view)

        setupViews(view)

        setOnDismissListener { onDismiss() }
    }

    private fun setupViews(view: android.view.View) {
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tilAccountName = view.findViewById<TextInputLayout>(R.id.tilAccountName)
        val etAccountName = view.findViewById<TextInputEditText>(R.id.etAccountName)
        val tilBalance = view.findViewById<TextInputLayout>(R.id.tilBalance)
        val etBalance = view.findViewById<TextInputEditText>(R.id.etBalance)
        val tvCurrentBalance = view.findViewById<TextView>(R.id.tvCurrentBalance)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnConfirm = view.findViewById<MaterialButton>(R.id.btnConfirm)

        // 设置标题
        tvTitle.text = "编辑账户"

        // 填充当前数据
        etAccountName.setText(account.name)
        etBalance.setText(account.balance.toString())
        tvCurrentBalance.text = "当前余额: ¥${currencyFormat.format(account.balance)}"

        // 取消按钮
        btnCancel.setOnClickListener {
            dismiss()
        }

        // 保存按钮
        btnConfirm.setOnClickListener {
            // 验证名称
            val name = etAccountName.text?.toString()?.trim()
            if (name.isNullOrEmpty()) {
                tilAccountName.error = "请输入账户名称"
                return@setOnClickListener
            }
            tilAccountName.error = null

            // 获取余额
            val balanceText = etBalance.text?.toString()
            val balance = balanceText?.toDoubleOrNull() ?: 0.0

            onSave(account.id, name, balance)
            dismiss()
        }
    }
}

