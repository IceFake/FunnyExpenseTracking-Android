package com.example.funnyexpensetracking.ui.compose.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.funnyexpensetracking.ui.statistics.StatisticsViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * 统计页面Compose版本Fragment
 * 
 * 使用Jetpack Compose重构的统计页面，保持与现有ViewModel的兼容性
 * 
 * 这是一个渐进式迁移的示例，展示了如何：
 * 1. 在现有Fragment架构中使用Compose
 * 2. 重用现有的ViewModel和业务逻辑
 * 3. 实现现代化的Material Design 3 UI
 */
@AndroidEntryPoint
class StatisticsComposeFragment : Fragment() {

    private val viewModel: StatisticsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // 避免在Fragment/Activity重建时重新组合
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            
            setContent {
                StatisticsScreen(
                    onBackClick = { navigateBack() },
                    onAiAnalysisClick = { navigateToAiAnalysis() },
                    onFinancialQueryClick = { navigateToFinancialQuery() },
                    viewModel = viewModel
                )
            }
        }
    }
    
    /**
     * 返回上一页
     */
    private fun navigateBack() {
        parentFragmentManager.popBackStack()
    }
    
    /**
     * 跳转到AI分析页面
     */
    private fun navigateToAiAnalysis() {
        // 保持与现有导航逻辑的一致性
        // 这里可以调用现有的导航方法
        val aiAnalysisFragment = Class.forName(
            "com.example.funnyexpensetracking.ui.aianalysis.AIAnalysisFragment"
        ).newInstance() as Fragment
        
        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, aiAnalysisFragment)
            .addToBackStack("statistics")
            .commit()
    }
    
    /**
     * 跳转到财务问答页面
     */
    private fun navigateToFinancialQuery() {
        // 保持与现有导航逻辑的一致性
        val financialQueryFragment = Class.forName(
            "com.example.funnyexpensetracking.ui.financialquery.FinancialQueryFragment"
        ).newInstance() as Fragment
        
        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, financialQueryFragment)
            .addToBackStack("statistics")
            .commit()
    }
    
    companion object {
        /**
         * 创建新的StatisticsComposeFragment实例
         */
        fun newInstance(): StatisticsComposeFragment {
            return StatisticsComposeFragment()
        }
    }
}