package com.example.funnyexpensetracking.ui.transaction

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.funnyexpensetracking.R
import com.example.funnyexpensetracking.databinding.FragmentTransactionBinding
import com.example.funnyexpensetracking.domain.model.Transaction
import com.example.funnyexpensetracking.util.CurrencyUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

/**
 * 记账Fragment
 */
@AndroidEntryPoint
class TransactionFragment : Fragment() {

    private var _binding: FragmentTransactionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by viewModels()
    private lateinit var transactionAdapter: DailyTransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeState()
        observeEvents()
    }

    private fun setupRecyclerView() {
        transactionAdapter = DailyTransactionAdapter(
            onTransactionClick = { transaction ->
                viewModel.editTransaction(transaction)
            },
            onTransactionLongClick = { transaction ->
                showDeleteConfirmDialog(transaction)
            }
        )

        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAdd.setOnClickListener {
            viewModel.showAddDialog()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    // 更新余额
                    binding.tvTotalBalance.text = CurrencyUtil.formatCurrency(state.totalBalance)
                    binding.tvTodayIncome.text = "+${CurrencyUtil.formatCurrency(state.todayIncome)}"
                    binding.tvTodayExpense.text = "-${CurrencyUtil.formatCurrency(state.todayExpense)}"

                    // 更新列表
                    transactionAdapter.submitList(state.dailyTransactions)

                    // 显示/隐藏空状态
                    if (state.dailyTransactions.isEmpty()) {
                        binding.rvTransactions.visibility = View.GONE
                        binding.emptyView.visibility = View.VISIBLE
                    } else {
                        binding.rvTransactions.visibility = View.VISIBLE
                        binding.emptyView.visibility = View.GONE
                    }

                    // 显示添加对话框
                    if (state.showAddDialog) {
                        showAddTransactionDialog(state.editingTransaction)
                    }

                    // 显示添加账户对话框
                    if (state.showAddAccountDialog) {
                        showAddAccountDialog()
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collectLatest { event ->
                    when (event) {
                        is TransactionUiEvent.ShowMessage -> {
                            Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                        }
                        is TransactionUiEvent.TransactionAdded,
                        is TransactionUiEvent.TransactionUpdated,
                        is TransactionUiEvent.TransactionDeleted -> {
                            // 数据会自动更新
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private var addTransactionDialog: BottomSheetDialog? = null

    private fun showAddTransactionDialog(editingTransaction: Transaction?) {
        if (addTransactionDialog?.isShowing == true) return

        val dialog = AddTransactionBottomSheet(
            context = requireContext(),
            accounts = viewModel.uiState.value.accounts,
            editingTransaction = editingTransaction,
            onSave = { amount, type, category, accountId, note, date ->
                if (editingTransaction != null) {
                    viewModel.updateTransaction(
                        id = editingTransaction.id,
                        amount = amount,
                        type = type,
                        category = category,
                        accountId = accountId,
                        note = note,
                        date = date
                    )
                } else {
                    viewModel.addTransaction(
                        amount = amount,
                        type = type,
                        category = category,
                        accountId = accountId,
                        note = note,
                        date = date
                    )
                }
            },
            onDismiss = {
                viewModel.hideAddDialog()
                addTransactionDialog = null
            },
            onAddAccount = {
                viewModel.showAddAccountDialog()
            }
        )

        addTransactionDialog = dialog
        dialog.show()
    }

    private var addAccountDialog: BottomSheetDialog? = null

    private fun showAddAccountDialog() {
        if (addAccountDialog?.isShowing == true) return

        val dialog = AddAccountDialog(
            context = requireContext(),
            onConfirm = { name, balance ->
                viewModel.addAccount(name, balance)
            },
            onDismiss = {
                viewModel.hideAddAccountDialog()
                addAccountDialog = null
            }
        )

        addAccountDialog = dialog
        dialog.show()
    }

    private fun showDeleteConfirmDialog(transaction: Transaction) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("删除确认")
            .setMessage("确定要删除这条记录吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteTransaction(transaction)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        addTransactionDialog?.dismiss()
        addAccountDialog?.dismiss()
        _binding = null
    }
}

