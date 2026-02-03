package com.example.funnyexpensetracking.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.funnyexpensetracking.R
import com.example.funnyexpensetracking.domain.model.Transaction
import com.example.funnyexpensetracking.ui.history.TransactionAdapter
import com.example.funnyexpensetracking.util.CurrencyUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期明细BottomSheet - 显示某一天的账单明细
 */
@AndroidEntryPoint
class DayDetailBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: DayDetailViewModel by viewModels()
    private lateinit var transactionAdapter: TransactionAdapter

    // Views
    private lateinit var tvDateTitle: TextView
    private lateinit var btnClose: ImageButton
    private lateinit var tvDayIncome: TextView
    private lateinit var tvDayExpense: TextView
    private lateinit var tvDayBalance: TextView
    private lateinit var rvTransactions: RecyclerView
    private lateinit var emptyView: View

    companion object {
        private const val ARG_YEAR = "arg_year"
        private const val ARG_MONTH = "arg_month"
        private const val ARG_DAY = "arg_day"

        fun newInstance(year: Int, month: Int, day: Int): DayDetailBottomSheet {
            return DayDetailBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_YEAR, year)
                    putInt(ARG_MONTH, month)
                    putInt(ARG_DAY, day)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_day_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        observeState()

        // 加载指定日期的数据
        arguments?.let {
            val year = it.getInt(ARG_YEAR)
            val month = it.getInt(ARG_MONTH)
            val day = it.getInt(ARG_DAY)
            viewModel.loadDate(year, month, day)
        }
    }

    private fun initViews(view: View) {
        tvDateTitle = view.findViewById(R.id.tvDateTitle)
        btnClose = view.findViewById(R.id.btnClose)
        tvDayIncome = view.findViewById(R.id.tvDayIncome)
        tvDayExpense = view.findViewById(R.id.tvDayExpense)
        tvDayBalance = view.findViewById(R.id.tvDayBalance)
        rvTransactions = view.findViewById(R.id.rvTransactions)
        emptyView = view.findViewById(R.id.emptyView)
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
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
        btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    // 更新日期标题
                    tvDateTitle.text = formatDate(state.year, state.month, state.day)

                    // 更新统计
                    tvDayIncome.text = "+${CurrencyUtil.formatCurrency(state.dayIncome)}"
                    tvDayExpense.text = "-${CurrencyUtil.formatCurrency(state.dayExpense)}"

                    // 结余颜色
                    val balanceText = if (state.dayBalance >= 0) {
                        "+${CurrencyUtil.formatCurrency(state.dayBalance)}"
                    } else {
                        CurrencyUtil.formatCurrency(state.dayBalance)
                    }
                    tvDayBalance.text = balanceText
                    tvDayBalance.setTextColor(
                        if (state.dayBalance >= 0)
                            requireContext().getColor(android.R.color.holo_green_dark)
                        else
                            requireContext().getColor(android.R.color.holo_red_dark)
                    )

                    // 更新列表
                    transactionAdapter.submitList(state.transactions)

                    // 显示/隐藏空状态
                    if (state.transactions.isEmpty()) {
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

    private fun formatDate(year: Int, month: Int, day: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day)
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINA)
        return dateFormat.format(calendar.time)
    }

    private fun showTransactionDetailDialog(transaction: Transaction) {
        val message = buildString {
            appendLine("分类：${transaction.category}")
            appendLine("金额：${CurrencyUtil.formatCurrency(transaction.amount)}")
            appendLine("账户：${transaction.accountName}")
            if (transaction.note.isNotEmpty()) {
                appendLine("备注：${transaction.note}")
            }
            appendLine("时间：${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(transaction.createdAt))}")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("交易详情")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showDeleteConfirmDialog(transaction: Transaction) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除交易")
            .setMessage("确定要删除这条交易记录吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteTransaction(transaction.id)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
