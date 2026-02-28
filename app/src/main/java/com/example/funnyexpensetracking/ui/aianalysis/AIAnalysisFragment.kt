package com.example.funnyexpensetracking.ui.aianalysis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.funnyexpensetracking.R
import com.example.funnyexpensetracking.databinding.FragmentAiAnalysisBinding
import com.example.funnyexpensetracking.domain.model.HabitInsight
import com.example.funnyexpensetracking.domain.model.Suggestion
import com.example.funnyexpensetracking.ui.common.LoadingState
import com.example.funnyexpensetracking.util.DateTimeUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * AI分析Fragment
 * 自动调用API分析用户消费数据并显示结果
 */
@AndroidEntryPoint
class AIAnalysisFragment : Fragment() {

    private var _binding: FragmentAiAnalysisBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AIAnalysisViewModel by viewModels()

    private var apiKeyDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupClickListeners()
        observeState()
        // 不再自动调用AI接口，由ViewModel加载缓存结果展示
    }

    private fun setupViews() {
        // 初始界面状态由 observeState 中的 uiState 控制
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnRetry.setOnClickListener {
            startAnalysis()
        }

        binding.btnRefresh.setOnClickListener {
            startAnalysis()
        }

        binding.btnApiKeySetting.setOnClickListener {
            viewModel.openApiKeyDialog()
        }

        binding.btnFinancialQuery.setOnClickListener {
            navigateToFinancialQuery()
        }
    }

    private fun navigateToFinancialQuery() {
        val fragment = com.example.funnyexpensetracking.ui.financialquery.FinancialQueryFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack("ai_analysis")
            .commit()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest { state ->
                        updateUi(state)
                    }
                }

                launch {
                    viewModel.uiEvent.collectLatest { event ->
                        when (event) {
                            is AIAnalysisUiEvent.ShowMessage -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                            AIAnalysisUiEvent.AnalysisCompleted -> {
                                // 分析完成，可以在这里添加额外的处理
                            }
                            AIAnalysisUiEvent.ShowApiKeyDialog -> {
                                showApiKeyDialog()
                            }
                        }
                    }
                }

                launch {
                    viewModel.uiState.collectLatest { state ->
                        if (state.showApiKeyDialog && apiKeyDialog?.isShowing != true) {
                            showApiKeyDialog()
                        }
                    }
                }
            }
        }
    }

    private fun updateUi(state: AIAnalysisUiState) {
        when (state.loadingState) {
            LoadingState.LOADING -> {
                showLoading()
            }
            LoadingState.SUCCESS -> {
                if (state.analysisResult != null) {
                    showContent(state)
                } else {
                    showError("分析结果为空")
                }
            }
            LoadingState.ERROR -> {
                showError(state.errorMessage ?: "分析失败")
            }
            else -> {
                // IDLE状态，提示用户点击刷新按钮开始分析
                showEmpty()
            }
        }
    }

    private fun startAnalysis() {
        viewModel.requestAnalysis()
    }

    private fun showLoading() {
        binding.loadingLayout.isVisible = true
        binding.errorLayout.isVisible = false
        binding.contentLayout.isVisible = false
        binding.btnRefresh.isVisible = false
    }

    private fun showEmpty() {
        binding.loadingLayout.isVisible = false
        binding.errorLayout.isVisible = true
        binding.contentLayout.isVisible = false
        binding.btnRefresh.isVisible = true

        binding.tvError.text = "暂无分析结果，点击刷新按钮开始AI分析"
    }

    private fun showError(errorMessage: String) {
        binding.loadingLayout.isVisible = false
        binding.errorLayout.isVisible = true
        binding.contentLayout.isVisible = false
        binding.btnRefresh.isVisible = true

        binding.tvError.text = errorMessage
    }

    private fun showContent(state: AIAnalysisUiState) {
        binding.loadingLayout.isVisible = false
        binding.errorLayout.isVisible = false
        binding.contentLayout.isVisible = true
        binding.btnRefresh.isVisible = true

        val result = state.analysisResult ?: return

        // 显示分析摘要
        binding.tvAnalysisSummary.text = result.summary

        // 显示习惯洞察
        renderHabitInsights(result.spendingHabits)

        // 显示AI建议
        renderSuggestions(result.suggestions)

        // 显示未来预测
        renderPredictions(result.predictions)

        // 显示生成时间
        val formattedTime = DateTimeUtil.formatDateTime(result.generatedAt)
        binding.tvGeneratedAt.text = "分析时间: $formattedTime"
    }

    private fun renderHabitInsights(habits: List<HabitInsight>) {
        binding.layoutHabitInsights.removeAllViews()

        if (habits.isEmpty()) {
            val emptyView = TextView(requireContext()).apply {
                text = "暂无习惯洞察数据"
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                textSize = 14f
            }
            binding.layoutHabitInsights.addView(emptyView)
            return
        }

        habits.forEachIndexed { index, habit ->
            val habitView = createHabitInsightView(habit, index)
            binding.layoutHabitInsights.addView(habitView)
        }
    }

    private fun createHabitInsightView(habit: HabitInsight, index: Int): View {
        val habitView = TextView(requireContext()).apply {
            val trendEmoji = when (habit.trend) {
                com.example.funnyexpensetracking.domain.model.HabitTrend.INCREASING -> "📈"
                com.example.funnyexpensetracking.domain.model.HabitTrend.DECREASING -> "📉"
                com.example.funnyexpensetracking.domain.model.HabitTrend.STABLE -> "📊"
            }
            val habitText = "${index + 1}. ${habit.category}\n   $trendEmoji ${habit.insight}"
            text = habitText
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            textSize = 14f
            setLineSpacing(4f, 1f)
            setPadding(0, if (index == 0) 0 else 12, 0, 12)
        }
        return habitView
    }

    private fun renderSuggestions(suggestions: List<Suggestion>) {
        binding.layoutSuggestions.removeAllViews()

        if (suggestions.isEmpty()) {
            val emptyView = TextView(requireContext()).apply {
                text = "暂无优化建议"
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                textSize = 14f
            }
            binding.layoutSuggestions.addView(emptyView)
            return
        }

        suggestions.forEachIndexed { index, suggestion ->
            val suggestionView = createSuggestionView(suggestion, index)
            binding.layoutSuggestions.addView(suggestionView)
        }
    }

    private fun createSuggestionView(suggestion: Suggestion, index: Int): View {
        val suggestionView = TextView(requireContext()).apply {
            val priorityEmoji = when (suggestion.priority) {
                com.example.funnyexpensetracking.domain.model.SuggestionPriority.HIGH -> "🔴"
                com.example.funnyexpensetracking.domain.model.SuggestionPriority.MEDIUM -> "🟡"
                com.example.funnyexpensetracking.domain.model.SuggestionPriority.LOW -> "🟢"
            }
            val suggestionText = "${priorityEmoji} ${suggestion.title}\n   ${suggestion.description}"
            text = suggestionText
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            textSize = 14f
            setLineSpacing(4f, 1f)
            setPadding(0, if (index == 0) 0 else 12, 0, 12)
        }
        return suggestionView
    }

    private fun renderPredictions(predictions: com.example.funnyexpensetracking.domain.model.Prediction?) {
        if (predictions == null) {
            binding.cardPrediction.isVisible = false
            return
        }

        binding.cardPrediction.isVisible = true
        binding.layoutPredictions.removeAllViews()

        val predictionItems = listOf(
            "下月预测支出: ${String.format("%.2f", predictions.nextMonthExpense)} 元",
            "下月预测收入: ${String.format("%.2f", predictions.nextMonthIncome)} 元",
            "储蓄潜力: ${String.format("%.2f", predictions.savingsPotential)} 元"
        )

        predictionItems.forEachIndexed { index, text ->
            val predictionView = TextView(requireContext()).apply {
                this.text = "• $text"
                setTextColor(requireContext().getColor(android.R.color.black))
                textSize = 14f
                setPadding(0, if (index == 0) 0 else 8, 0, 8)
            }
            binding.layoutPredictions.addView(predictionView)
        }
    }

    /**
     * 显示API Key设置弹窗
     */
    private fun showApiKeyDialog() {
        // 避免重复弹出
        if (apiKeyDialog?.isShowing == true) return

        val currentKey = viewModel.uiState.value.currentApiKey

        val editText = EditText(requireContext()).apply {
            hint = "请输入DeepSeek API Key"
            setText(currentKey)
            isSingleLine = true
            setPadding(60, 40, 60, 40)
            // 将光标移到末尾
            setSelection(text.length)
        }

        apiKeyDialog = AlertDialog.Builder(requireContext())
            .setTitle("⚙️ API Key 设置")
            .setMessage("请输入您的 DeepSeek API Key\n\n可从 platform.deepseek.com 获取")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val inputKey = editText.text.toString().trim()
                if (inputKey.isNotBlank()) {
                    viewModel.saveApiKey(inputKey)
                } else {
                    Toast.makeText(requireContext(), "API Key不能为空", Toast.LENGTH_SHORT).show()
                }
                viewModel.dismissApiKeyDialog()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                viewModel.dismissApiKeyDialog()
            }
            .setOnDismissListener {
                viewModel.dismissApiKeyDialog()
            }
            .create()

        apiKeyDialog?.show()
    }

    override fun onDestroyView() {
        apiKeyDialog?.dismiss()
        apiKeyDialog = null
        super.onDestroyView()
        _binding = null
    }
}