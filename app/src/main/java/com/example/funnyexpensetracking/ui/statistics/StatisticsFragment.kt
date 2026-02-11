package com.example.funnyexpensetracking.ui.statistics

import android.graphics.Color
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
import com.example.funnyexpensetracking.databinding.FragmentStatisticsBinding
import com.example.funnyexpensetracking.domain.model.CategoryStat
import com.example.funnyexpensetracking.domain.model.TransactionType
import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.util.CurrencyUtil
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 统计图表Fragment
 */
@AndroidEntryPoint
class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels()

    private lateinit var expenseCategoryAdapter: CategoryStatAdapter
    private lateinit var incomeCategoryAdapter: CategoryStatAdapter

    // 支出分类颜色
    private val expenseColors = listOf(
        Color.parseColor("#FF5722"), // 深橙
        Color.parseColor("#E91E63"), // 粉红
        Color.parseColor("#9C27B0"), // 紫色
        Color.parseColor("#673AB7"), // 深紫
        Color.parseColor("#3F51B5"), // 靛蓝
        Color.parseColor("#2196F3"), // 蓝色
        Color.parseColor("#03A9F4"), // 浅蓝
        Color.parseColor("#00BCD4"), // 青色
        Color.parseColor("#009688"), // 蓝绿
        Color.parseColor("#795548"), // 棕色
    )

    // 收入分类颜色
    private val incomeColors = listOf(
        Color.parseColor("#4CAF50"), // 绿色
        Color.parseColor("#8BC34A"), // 浅绿
        Color.parseColor("#CDDC39"), // 酸橙
        Color.parseColor("#FFEB3B"), // 黄色
        Color.parseColor("#FFC107"), // 琥珀
        Color.parseColor("#FF9800"), // 橙色
        Color.parseColor("#00E676"), // 亮绿
        Color.parseColor("#69F0AE"), // 薄荷绿
        Color.parseColor("#00BFA5"), // 青绿
        Color.parseColor("#1DE9B6"), // 蓝绿
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        observeState()
    }

    private fun setupViews() {
        // 设置支出分类 RecyclerView
        expenseCategoryAdapter = CategoryStatAdapter()
        binding.rvExpenseCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = expenseCategoryAdapter
        }

        // 设置收入分类 RecyclerView
        incomeCategoryAdapter = CategoryStatAdapter()
        binding.rvIncomeCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = incomeCategoryAdapter
        }

        // 设置环状图
        setupPieChart(binding.pieChartExpense)
        setupPieChart(binding.pieChartIncome)

        // 设置Tab切换
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.loadCurrentMonthStatistics()
                    1 -> viewModel.loadYearlyStatistics(viewModel.uiState.value.selectedYear)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupPieChart(pieChart: PieChart) {
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)

            dragDecelerationFrictionCoef = 0.95f

            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f

            setDrawCenterText(true)
            centerText = ""

            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true

            legend.apply {
                isEnabled = false
            }

            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(10f)
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnPrev.setOnClickListener {
            val state = viewModel.uiState.value
            if (state.isMonthlyView) {
                viewModel.previousMonth()
            } else {
                viewModel.selectYear(state.selectedYear - 1)
            }
        }

        binding.btnNext.setOnClickListener {
            val state = viewModel.uiState.value
            if (state.isMonthlyView) {
                viewModel.nextMonth()
            } else {
                viewModel.selectYear(state.selectedYear + 1)
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest { state ->
                        // 更新日期显示
                        if (state.isMonthlyView) {
                            binding.btnSelectDate.text = "${state.selectedYear}年${String.format("%02d", state.selectedMonth)}月"
                        } else {
                            binding.btnSelectDate.text = "${state.selectedYear}年"
                        }

                        // 更新统计数据
                        state.currentStatistics?.let { stats ->
                            binding.tvTotalIncome.text = "+${CurrencyUtil.formatCurrency(stats.totalIncome)}"
                            binding.tvTotalExpense.text = "-${CurrencyUtil.formatCurrency(stats.totalExpense)}"
                            val balance = stats.totalIncome - stats.totalExpense
                            binding.tvBalance.text = CurrencyUtil.formatCurrency(balance)
                            if (balance >= 0) {
                                binding.tvBalance.setTextColor(requireContext().getColor(android.R.color.holo_green_light))
                            } else {
                                binding.tvBalance.setTextColor(requireContext().getColor(android.R.color.holo_red_light))
                            }
                        }

                        // 更新分类统计图表
                        updateCategoryCharts(state.categoryStats)

                        // 更新加载状态
                        when (state.loadingState) {
                            LoadingState.LOADING -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.cardExpenseChart.visibility = View.GONE
                                binding.cardIncomeChart.visibility = View.GONE
                                binding.emptyView.visibility = View.GONE
                            }
                            LoadingState.SUCCESS -> {
                                binding.progressBar.visibility = View.GONE
                                if (state.categoryStats.isEmpty()) {
                                    binding.cardExpenseChart.visibility = View.GONE
                                    binding.cardIncomeChart.visibility = View.GONE
                                    binding.emptyView.visibility = View.VISIBLE
                                } else {
                                    binding.cardExpenseChart.visibility = View.VISIBLE
                                    binding.cardIncomeChart.visibility = View.VISIBLE
                                    binding.emptyView.visibility = View.GONE
                                }
                            }
                            LoadingState.ERROR -> {
                                binding.progressBar.visibility = View.GONE
                                binding.emptyView.visibility = View.VISIBLE
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    viewModel.uiEvent.collectLatest { event ->
                        when (event) {
                            is StatisticsUiEvent.ShowMessage -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                            is StatisticsUiEvent.OpenChart -> {
                                // 可以在这里处理打开图表的逻辑
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateCategoryCharts(categoryStats: List<CategoryStat>) {
        // 分离收入和支出分类
        val expenseStats = categoryStats.filter { it.type == TransactionType.EXPENSE }
        val incomeStats = categoryStats.filter { it.type == TransactionType.INCOME }

        // 更新支出图表
        updatePieChart(
            binding.pieChartExpense,
            expenseStats,
            expenseColors,
            "支出"
        )

        // 更新支出分类列表
        val expenseItems = expenseStats.mapIndexed { index, stat ->
            CategoryStatItem(stat, expenseColors[index % expenseColors.size])
        }
        expenseCategoryAdapter.submitList(expenseItems)

        // 更新收入图表
        updatePieChart(
            binding.pieChartIncome,
            incomeStats,
            incomeColors,
            "收入"
        )

        // 更新收入分类列表
        val incomeItems = incomeStats.mapIndexed { index, stat ->
            CategoryStatItem(stat, incomeColors[index % incomeColors.size])
        }
        incomeCategoryAdapter.submitList(incomeItems)

        // 根据数据显示/隐藏卡片
        binding.cardExpenseChart.visibility = if (expenseStats.isNotEmpty()) View.VISIBLE else View.GONE
        binding.cardIncomeChart.visibility = if (incomeStats.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun updatePieChart(
        pieChart: PieChart,
        categoryStats: List<CategoryStat>,
        colors: List<Int>,
        centerText: String
    ) {
        if (categoryStats.isEmpty()) {
            pieChart.clear()
            pieChart.centerText = "无数据"
            pieChart.invalidate()
            return
        }

        val entries = categoryStats.map { stat ->
            PieEntry(stat.percentage.toFloat(), stat.category)
        }

        val dataSet = PieDataSet(entries, "").apply {
            setDrawIcons(false)
            sliceSpace = 2f
            selectionShift = 5f
            setColors(colors.take(categoryStats.size))
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(pieChart))
            setValueTextSize(11f)
            setValueTextColor(Color.WHITE)
        }

        pieChart.data = data

        // 计算总金额
        val total = categoryStats.sumOf { it.amount }
        pieChart.centerText = "$centerText\n${CurrencyUtil.formatCurrency(total)}"

        pieChart.highlightValues(null)
        pieChart.animateY(1000, Easing.EaseInOutQuad)
        pieChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
