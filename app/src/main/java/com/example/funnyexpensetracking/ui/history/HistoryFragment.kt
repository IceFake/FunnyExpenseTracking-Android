package com.example.funnyexpensetracking.ui.history

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.funnyexpensetracking.R   
import com.example.funnyexpensetracking.domain.model.Transaction
import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.ui.transaction.DailyTransactionAdapter
import com.example.funnyexpensetracking.util.CurrencyUtil
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

/**
 * 历史账单Fragment - 显示按月/日期分组的账单明细
 */
@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var transactionAdapter: DailyTransactionAdapter

    // Views
    private lateinit var btnPrevMonth: MaterialButton
    private lateinit var btnNextMonth: MaterialButton
    private lateinit var btnSelectDate: MaterialButton
    private lateinit var tvMonthIncome: TextView
    private lateinit var tvMonthExpense: TextView
    private lateinit var tvMonthBalance: TextView
    private lateinit var rvTransactions: RecyclerView
    private lateinit var emptyView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        observeState()
        observeEvents()
    }

    private fun initViews(view: View) {
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth)
        btnNextMonth = view.findViewById(R.id.btnNextMonth)
        btnSelectDate = view.findViewById(R.id.btnSelectDate)
        tvMonthIncome = view.findViewById(R.id.tvMonthIncome)
        tvMonthExpense = view.findViewById(R.id.tvMonthExpense)
        tvMonthBalance = view.findViewById(R.id.tvMonthBalance)
        rvTransactions = view.findViewById(R.id.rvTransactions)
        emptyView = view.findViewById(R.id.emptyView)
    }

    private fun setupRecyclerView() {
        transactionAdapter = DailyTransactionAdapter(
            onTransactionClick = { transaction ->
                showTransactionDetailDialog(transaction)
            },
            onTransactionLongClick = { transaction ->
                showDeleteConfirmDialog(transaction)
            }
        )

        rvTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
    }

    private fun setupClickListeners() {
        btnPrevMonth.setOnClickListener {
            viewModel.selectPreviousMonth()
        }

        btnNextMonth.setOnClickListener {
            viewModel.selectNextMonth()
        }

        btnSelectDate.setOnClickListener {
            showMonthPickerDialog()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    // 更新月份显示
                    btnSelectDate.text = viewModel.getDisplayMonth()

                    // 更新月度统计
                    tvMonthIncome.text = "+${CurrencyUtil.formatCurrency(state.monthIncome)}"
                    tvMonthExpense.text = "-${CurrencyUtil.formatCurrency(state.monthExpense)}"

                    // 结余颜色根据正负变化
                    val balanceText = if (state.monthBalance >= 0) {
                        "+${CurrencyUtil.formatCurrency(state.monthBalance)}"
                    } else {
                        "-${CurrencyUtil.formatCurrency(-state.monthBalance)}"
                    }
                    tvMonthBalance.text = balanceText
                    tvMonthBalance.setTextColor(
                        if (state.monthBalance >= 0)
                            requireContext().getColor(android.R.color.holo_green_dark)
                        else
                            requireContext().getColor(android.R.color.holo_red_dark)
                    )

                    // 更新列表
                    transactionAdapter.submitList(state.dailyTransactions)

                    // 显示/隐藏空状态
                    if (state.dailyTransactions.isEmpty() && state.loadingState != LoadingState.LOADING) {
                        rvTransactions.visibility = View.GONE
                        emptyView.visibility = View.VISIBLE
                    } else {
                        rvTransactions.visibility = View.VISIBLE
                        emptyView.visibility = View.GONE
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
                        is HistoryUiEvent.ShowMessage -> {
                            Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                        }
                        is HistoryUiEvent.NavigateToDetail -> {
                            // 导航到详情页
                        }
                        is HistoryUiEvent.TransactionDeleted -> {
                            Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    /**
     * 显示月份选择器
     */
    private fun showMonthPickerDialog() {
        val state = viewModel.uiState.value
        val currentYear = state.selectedYear
        val currentMonth = state.selectedMonth

        // 使用DatePickerDialog，只选择年月
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, currentYear)
        calendar.set(Calendar.MONTH, currentMonth - 1)

        DatePickerDialog(
            requireContext(),
            { _, year, month, _ ->
                viewModel.selectMonth(year, month + 1)
            },
            currentYear,
            currentMonth - 1,
            1
        ).apply {
            // 隐藏日期选择
            datePicker.findViewById<View>(
                resources.getIdentifier("day", "id", "android")
            )?.visibility = View.GONE
        }.show()
    }

    /**
     * 显示交易详情对话框
     */
    private fun showTransactionDetailDialog(transaction: Transaction) {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
        val message = """
            |分类：${transaction.category}
            |金额：${CurrencyUtil.formatCurrency(transaction.amount)}
            |账户：${transaction.accountName}
            |日期：${dateFormat.format(Date(transaction.date))}
            |备注：${transaction.note.ifEmpty { "无" }}
        """.trimMargin()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("账单详情")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(transaction: Transaction) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除账单")
            .setMessage("确定要删除「${transaction.category}」这条账单记录吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteTransaction(transaction.id)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}

