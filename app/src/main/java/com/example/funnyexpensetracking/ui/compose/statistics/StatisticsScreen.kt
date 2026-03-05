package com.example.funnyexpensetracking.ui.compose.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.funnyexpensetracking.R
import com.example.funnyexpensetracking.domain.model.CategoryStat
import com.example.funnyexpensetracking.domain.model.TransactionType
import com.example.funnyexpensetracking.ui.compose.FunnyExpenseTheme
import com.example.funnyexpensetracking.ui.compose.composeColors
import com.example.funnyexpensetracking.ui.compose.components.CategoryPieChart
import com.example.funnyexpensetracking.ui.statistics.StatisticsViewModel
import com.example.funnyexpensetracking.util.CurrencyUtil

/**
 * 统计页面主屏幕（Compose版本）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBackClick: () -> Unit,
    onAiAnalysisClick: () -> Unit,
    onFinancialQueryClick: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    // 收集状态
    val uiState by viewModel.uiState.collectAsState()
    
    // 初始化加载
    LaunchedEffect(Unit) {
        viewModel.loadCurrentMonthStatistics()
    }
    
    FunnyExpenseTheme {
        Scaffold(
            topBar = {
                StatisticsTopAppBar(
                    onBackClick = onBackClick,
                    onAiAnalysisClick = onAiAnalysisClick,
                    onFinancialQueryClick = onFinancialQueryClick
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            StatisticsContent(
                uiState = uiState,
                paddingValues = paddingValues,
                onPreviousClick = {
                    if (uiState.isMonthlyView) {
                        viewModel.previousMonth()
                    } else {
                        viewModel.selectYear(uiState.selectedYear - 1)
                    }
                },
                onNextClick = {
                    if (uiState.isMonthlyView) {
                        viewModel.nextMonth()
                    } else {
                        viewModel.selectYear(uiState.selectedYear + 1)
                    }
                },
                onTabSelected = { tabIndex ->
                    when (tabIndex) {
                        0 -> viewModel.loadCurrentMonthStatistics()
                        1 -> viewModel.loadYearlyStatistics(uiState.selectedYear)
                    }
                }
            )
        }
    }
}

/**
 * 统计页面顶部应用栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatisticsTopAppBar(
    onBackClick: () -> Unit,
    onAiAnalysisClick: () -> Unit,
    onFinancialQueryClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.statistics_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back)
                )
            }
        },
        actions = {
            // AI分析按钮
            IconButton(onClick = onAiAnalysisClick) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = stringResource(R.string.ai_analysis)
                )
            }
            
            // 财务问答按钮
            IconButton(onClick = onFinancialQueryClick) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = stringResource(R.string.financial_query)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

/**
 * 统计页面主要内容
 */
@Composable
private fun StatisticsContent(
    uiState: com.example.funnyexpensetracking.ui.statistics.StatisticsUiState,
    paddingValues: PaddingValues,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onTabSelected: (Int) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 加载状态处理
        when (uiState.loadingState) {
            com.example.funnyexpensetracking.ui.common.LoadingState.LOADING -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            
            com.example.funnyexpensetracking.ui.common.LoadingState.ERROR -> {
                item {
                    ErrorView(
                        message = uiState.errorMessage ?: "加载失败",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            com.example.funnyexpensetracking.ui.common.LoadingState.SUCCESS -> {
                if (uiState.categoryStats.isEmpty()) {
                    item {
                        EmptyView(modifier = Modifier.padding(16.dp))
                    }
                } else {
                    // 日期选择器
                    item {
                        DateSelector(
                            selectedYear = uiState.selectedYear,
                            selectedMonth = uiState.selectedMonth,
                            isMonthlyView = uiState.isMonthlyView,
                            onPreviousClick = onPreviousClick,
                            onNextClick = onNextClick,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    
                    // 标签页选择器
                    item {
                        StatisticsTabRow(
                            selectedTabIndex = selectedTabIndex,
                            onTabSelected = { index ->
                                selectedTabIndex = index
                                onTabSelected(index)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    
                    // 摘要卡片
                    item {
                        SummaryCard(
                            totalIncome = uiState.currentStatistics?.totalIncome ?: 0.0,
                            totalExpense = uiState.currentStatistics?.totalExpense ?: 0.0,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    
                    // 支出图表
                    val expenseStats = uiState.categoryStats.filter { it.type == TransactionType.EXPENSE }
                    if (expenseStats.isNotEmpty()) {
                        item {
                            ExpenseChartSection(
                                categoryStats = expenseStats,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    
                    // 收入图表
                    val incomeStats = uiState.categoryStats.filter { it.type == TransactionType.INCOME }
                    if (incomeStats.isNotEmpty()) {
                        item {
                            IncomeChartSection(
                                categoryStats = incomeStats,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
            
            else -> {}
        }
    }
}

/**
 * 日期选择器组件
 */
@Composable
private fun DateSelector(
    selectedYear: Int,
    selectedMonth: Int,
    isMonthlyView: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 上一个月/年份按钮
            IconButton(onClick = onPreviousClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Previous"
                )
            }
            
            // 日期显示
            Text(
                text = if (isMonthlyView) {
                    "${selectedYear}年${String.format("%02d", selectedMonth)}月"
                } else {
                    "${selectedYear}年"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 下一个月/年份按钮
            IconButton(onClick = onNextClick) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Next"
                )
            }
        }
    }
}

/**
 * 统计标签页行
 */
@Composable
private fun StatisticsTabRow(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("月度统计", "年度统计")
    
    Column(modifier = modifier) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                )
            }
        }
    }
}

/**
 * 摘要卡片
 */
@Composable
private fun SummaryCard(
    totalIncome: Double,
    totalExpense: Double,
    modifier: Modifier = Modifier
) {
    val balance = totalIncome - totalExpense
    val balanceColor = if (balance >= 0) {
        MaterialTheme.composeColors.incomeGreen
    } else {
        MaterialTheme.composeColors.expenseRed
    }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 收入行
            StatRow(
                label = "总收入",
                amount = totalIncome,
                color = MaterialTheme.composeColors.incomeGreen,
                isPositive = true
            )
            
            // 支出行
            StatRow(
                label = "总支出",
                amount = totalExpense,
                color = MaterialTheme.composeColors.expenseRed,
                isPositive = false
            )
            
            // 分隔线
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ) {}
            
            // 结余行
            StatRow(
                label = "结余",
                amount = balance,
                color = balanceColor,
                isPositive = balance >= 0,
                isBalance = true
            )
        }
    }
}

/**
 * 统计行组件
 */
@Composable
private fun StatRow(
    label: String,
    amount: Double,
    color: androidx.compose.ui.graphics.Color,
    isPositive: Boolean,
    isBalance: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBalance) {
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodyLarge
            },
            color = if (isBalance) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        
        Text(
            text = "${if (isPositive && amount > 0) "+" else ""}${CurrencyUtil.formatCurrency(amount)}",
            style = if (isBalance) {
                MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.titleMedium
            },
            color = if (isBalance) {
                color
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

/**
 * 支出图表部分
 */
@Composable
private fun ExpenseChartSection(
    categoryStats: List<CategoryStat>,
    modifier: Modifier = Modifier
) {
    ChartSection(
        title = "支出分类",
        categoryStats = categoryStats,
        chartColors = MaterialTheme.composeColors.expenseChartColors,
        modifier = modifier
    )
}

/**
 * 收入图表部分
 */
@Composable
private fun IncomeChartSection(
    categoryStats: List<CategoryStat>,
    modifier: Modifier = Modifier
) {
    ChartSection(
        title = "收入分类",
        categoryStats = categoryStats,
        chartColors = MaterialTheme.composeColors.incomeChartColors,
        modifier = modifier
    )
}

/**
 * 图表部分通用组件
 */
@Composable
private fun ChartSection(
    title: String,
    categoryStats: List<CategoryStat>,
    chartColors: List<androidx.compose.ui.graphics.Color>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 饼图和列表行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 饼图
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CategoryPieChart(
                        categoryStats = categoryStats,
                        colors = chartColors,
                        modifier = Modifier.size(180.dp)
                    )
                }
                
                // 分类列表
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryStats.forEachIndexed { index, stat ->
                        if (index < 5) { // 最多显示5个分类
                            CategoryListItem(
                                categoryStat = stat,
                                color = chartColors.getOrElse(index) { MaterialTheme.colorScheme.primary },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 分类列表项
 */
@Composable
private fun CategoryListItem(
    categoryStat: CategoryStat,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 颜色指示器
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        
        // 分类名称
        Text(
            text = categoryStat.category,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        
        // 金额和百分比
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = CurrencyUtil.formatCurrency(categoryStat.amount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = String.format("%.1f%%", categoryStat.percentage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 错误视图
 */
@Composable
private fun ErrorView(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "加载失败",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            
            Button(
                onClick = { /* 重试逻辑 */ },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "重试")
            }
        }
    }
}

/**
 * 空数据视图
 */
@Composable
private fun EmptyView(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "暂无数据",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "当前时间段内没有收支记录，\n开始记账后即可查看统计",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}