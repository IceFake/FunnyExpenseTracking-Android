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
import com.example.funnyexpensetracking.MainActivity
import com.example.funnyexpensetracking.R
import com.example.funnyexpensetracking.databinding.FragmentStatisticsBinding
import com.example.funnyexpensetracking.domain.model.CategoryStat
import com.example.funnyexpensetracking.domain.model.DailyTrend
import com.example.funnyexpensetracking.domain.model.TransactionType
import com.example.funnyexpensetracking.ui.aianalysis.AIAnalysisFragment
import com.example.funnyexpensetracking.ui.financialquery.FinancialQueryFragment
import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.util.CurrencyUtil
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
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

    private fun setupLineChart(lineChart: LineChart) {
        lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)

            axisRight.isEnabled = false
            axisLeft.apply {
                textColor = Color.parseColor("#999999")
                gridColor = Color.parseColor("#EEEEEE")
                enableGridDashedLine(10f, 10f, 0f)
            }

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.parseColor("#999999")
                setDrawGridLines(false)
                granularity = 1f
            }

            legend.apply {
                textColor = Color.parseColor("#666666")
                textSize = 12f
                form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
            }
        }
    }

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

    override fun onResume() {
        super.onResume()
        setBottomNavigationVisible(false)
    }

    override fun onPause() {
        setBottomNavigationVisible(true)
        super.onPause()
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

        // 设置折线图
        setupLineChart(binding.lineChartTrend)

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

        binding.btnAiAnalysis.setOnClickListener {
            navigateToAiAnalysis()
        }

        binding.btnFinancialQuery.setOnClickListener {
            navigateToFinancialQuery()
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

    private fun navigateToAiAnalysis() {
        val aiAnalysisFragment = AIAnalysisFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, aiAnalysisFragment)
            .addToBackStack("statistics")
            .commit()
    }

    private fun navigateToFinancialQuery() {
        val financialQueryFragment = FinancialQueryFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, financialQueryFragment)
            .addToBackStack("statistics")
            .commit()
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

                            // 环比计算显示
                            if (state.isMonthlyView) {
                                val curExpense = stats.totalExpense
                                val prevExpense = state.previousTotalExpense
                                if (prevExpense != null && prevExpense > 0) {
                                    val percent = ((curExpense - prevExpense) / prevExpense) * 100
                                    val sign = if (percent > 0) "+" else ""
                                    binding.tvMoMExpense.text = "支出环比: $sign${String.format("%.1f", percent)}%"
                                } else {
                                    binding.tvMoMExpense.text = "支出环比: --"
                                }

                                val curIncome = stats.totalIncome
                                val prevIncome = state.previousTotalIncome
                                if (prevIncome != null && prevIncome > 0) {
                                    val percent = ((curIncome - prevIncome) / prevIncome) * 100
                                    val sign = if (percent > 0) "+" else ""
                                    binding.tvMoMIncome.text = "收入环比: $sign${String.format("%.1f", percent)}%"
                                } else {
                                    binding.tvMoMIncome.text = "收入环比: --"
                                }
                            } else {
                                binding.tvMoMExpense.text = ""
                                binding.tvMoMIncome.text = ""
                            }
                        }

                        // 更新折线图
                        if (state.currentStatistics?.dailyTrends.isNullOrEmpty()) {
                            binding.cardTrendChart.visibility = View.GONE
                        } else {
                            binding.cardTrendChart.visibility = View.VISIBLE
                            updateLineChart(binding.lineChartTrend, state.currentStatistics!!.dailyTrends)
                        }

                        // 更新分类统计图表
                        updateCategoryCharts(state.categoryStats)

                        // 更新加载状态
                        when (state.loadingState) {
                            LoadingState.LOADING -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.cardTrendChart.visibility = View.GONE
                                binding.cardExpenseChart.visibility = View.GONE
                                binding.cardIncomeChart.visibility = View.GONE
                                binding.emptyView.visibility = View.GONE
                            }
                            LoadingState.SUCCESS -> {
                                binding.progressBar.visibility = View.GONE
                                if (state.categoryStats.isEmpty() && state.currentStatistics?.dailyTrends.isNullOrEmpty()) {
                                    binding.cardTrendChart.visibility = View.GONE
                                    binding.cardExpenseChart.visibility = View.GONE
                                    binding.cardIncomeChart.visibility = View.GONE
                                    binding.emptyView.visibility = View.VISIBLE
                                } else {
                                    if (!state.currentStatistics?.dailyTrends.isNullOrEmpty()) {
                                        binding.cardTrendChart.visibility = View.VISIBLE
                                    }
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

    private fun updateLineChart(lineChart: LineChart, dailyTrends: List<DailyTrend>) {
        if (dailyTrends.isEmpty()) {
            lineChart.clear()
            return
        }

        val expenseEntries = ArrayList<Entry>()
        val incomeEntries = ArrayList<Entry>()

        dailyTrends.forEach {
            expenseEntries.add(Entry(it.day.toFloat(), it.expense.toFloat()))
            incomeEntries.add(Entry(it.day.toFloat(), it.income.toFloat()))
        }

        val expenseDataSet = LineDataSet(expenseEntries, "支出").apply {
            color = Color.parseColor("#FF5722")
            setCircleColor(Color.parseColor("#FF5722"))
            lineWidth = 2f
            circleRadius = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        }

        val incomeDataSet = LineDataSet(incomeEntries, "收入").apply {
            color = Color.parseColor("#4CAF50")
            setCircleColor(Color.parseColor("#4CAF50"))
            lineWidth = 2f
            circleRadius = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        }

        val lineData = LineData(expenseDataSet, incomeDataSet)
        lineChart.data = lineData
        lineChart.animateX(1000, Easing.EaseInOutQuad)
        lineChart.invalidate()
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
        setBottomNavigationVisible(true)
        super.onDestroyView()
        _binding = null
    }

    private fun setBottomNavigationVisible(visible: Boolean) {
        (activity as? MainActivity)?.setBottomNavigationVisible(visible)
    }
}
