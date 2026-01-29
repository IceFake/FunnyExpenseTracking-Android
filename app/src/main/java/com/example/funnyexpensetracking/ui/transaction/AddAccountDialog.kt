package com.example.funnyexpensetracking.ui.transaction

import android.content.Context
import android.view.LayoutInflater
import com.example.funnyexpensetracking.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * 添加账户对话框
 */
class AddAccountDialog(
    context: Context,
    private val onConfirm: (name: String, balance: Double) -> Unit,
    private val onDismiss: () -> Unit
) : BottomSheetDialog(context) {

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_account, null)
        setContentView(view)

        val tilAccountName = view.findViewById<TextInputLayout>(R.id.tilAccountName)
        val etAccountName = view.findViewById<TextInputEditText>(R.id.etAccountName)
        val etInitialBalance = view.findViewById<TextInputEditText>(R.id.etInitialBalance)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnConfirm = view.findViewById<MaterialButton>(R.id.btnConfirm)

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnConfirm.setOnClickListener {
            val name = etAccountName.text?.toString()?.trim()
            if (name.isNullOrEmpty()) {
                tilAccountName.error = "请输入账户名称"
                return@setOnClickListener
            }
            tilAccountName.error = null

            val balanceText = etInitialBalance.text?.toString()
            val balance = balanceText?.toDoubleOrNull() ?: 0.0

            onConfirm(name, balance)
            dismiss()
        }

        setOnDismissListener { onDismiss() }
    }
}

