package com.example.funnyexpensetracking.ui.calendar

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.funnyexpensetracking.R
import com.example.funnyexpensetracking.util.CurrencyUtil
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 日历Fragment - 显示月度日历视图
 */
@AndroidEntryPoint
class CalendarFragment : Fragment() {

    private val viewModel: CalendarViewModel by viewModels()
    private lateinit var calendarAdapter: CalendarAdapter

    // Views
    private lateinit var btnPrevMonth: MaterialButton
    private lateinit var btnNextMonth: MaterialButton
    private lateinit var btnSelectMonth: MaterialButton
    private lateinit var tvMonthIncome: TextView
    private lateinit var tvMonthExpense: TextView
    private lateinit var tvMonthBalance: TextView
    private lateinit var rvCalendar: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
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
        btnSelectMonth = view.findViewById(R.id.btnSelectMonth)
        tvMonthIncome = view.findViewById(R.id.tvMonthIncome)
        tvMonthExpense = view.findViewById(R.id.tvMonthExpense)
        tvMonthBalance = view.findViewById(R.id.tvMonthBalance)
        rvCalendar = view.findViewById(R.id.rvCalendar)
    }

    private fun setupRecyclerView() {
        calendarAdapter = CalendarAdapter { year, month, day ->
            // 点击日期，跳转到历史账单页面并显示该日的账单
            viewModel.onDateClick(year, month, day)
        }

        rvCalendar.apply {
            layoutManager = GridLayoutManager(requireContext(), 7) // 7列（一周7天）
            adapter = calendarAdapter
        }
    }

    private fun setupClickListeners() {
        btnPrevMonth.setOnClickListener {
            viewModel.selectPreviousMonth()
        }

        btnNextMonth.setOnClickListener {
            viewModel.selectNextMonth()
        }

        btnSelectMonth.setOnClickListener {
            // TODO: 可以添加月份选择器
            Toast.makeText(requireContext(), "月份选择功能", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    // 更新月份显示
                    btnSelectMonth.text = viewModel.getDisplayMonth()

                    // 更新月度统计
                    tvMonthIncome.text = "+${CurrencyUtil.formatCurrency(state.monthIncome)}"
                    tvMonthExpense.text = "-${CurrencyUtil.formatCurrency(state.monthExpense)}"

                    // 结余颜色根据正负变化
                    val balanceText = if (state.monthBalance >= 0) {
                        "+${CurrencyUtil.formatCurrency(state.monthBalance)}"
                    } else {
                        CurrencyUtil.formatCurrency(state.monthBalance)
                    }
                    tvMonthBalance.text = balanceText
                    tvMonthBalance.setTextColor(
                        if (state.monthBalance >= 0)
                            requireContext().getColor(android.R.color.holo_green_dark)
                        else
                            requireContext().getColor(android.R.color.holo_red_dark)
                    )

                    // 更新日历
                    calendarAdapter.setCalendarData(
                        state.selectedYear,
                        state.selectedMonth,
                        state.dailyBalanceMap
                    )
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collect { event ->
                    when (event) {
                        is CalendarUiEvent.ShowMessage -> {
                            Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                        }
                        is CalendarUiEvent.NavigateToDay -> {
                            // 提示用户点击的日期
                            val message = "${event.year}年${event.month}月${event.day}日"
                            Toast.makeText(requireContext(), "查看 $message 的账单", Toast.LENGTH_SHORT).show()
                            // 可以通过共享ViewModel或EventBus传递日期给HistoryFragment
                        }
                    }
                }
            }
        }
    }
}

