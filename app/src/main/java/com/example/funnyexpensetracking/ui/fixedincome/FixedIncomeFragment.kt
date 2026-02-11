package com.example.funnyexpensetracking.ui.fixedincome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.funnyexpensetracking.databinding.FragmentFixedIncomeBinding
import com.example.funnyexpensetracking.domain.model.FixedIncome
import com.example.funnyexpensetracking.ui.transaction.AddFixedIncomeBottomSheet
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DecimalFormat

/**
 * 固定收支管理Fragment
 *
 * 功能说明：
 * - 条目新增后不可编辑
 * - 点击条目显示详情
 * - 长按条目显示操作菜单（停用/删除）
 */
@AndroidEntryPoint
class FixedIncomeFragment : Fragment() {

    private var _binding: FragmentFixedIncomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FixedIncomeViewModel by viewModels()
    private lateinit var adapter: FixedIncomeAdapter

    private val perMinuteFormat = DecimalFormat("0.0000")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFixedIncomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupTabLayout()
        setupClickListeners()
        observeState()
        observeEvents()
    }

    private fun setupRecyclerView() {
        adapter = FixedIncomeAdapter(
            onItemClick = { fixedIncome ->
                // 点击显示详情
                showDetailDialog(fixedIncome)
            },
            onItemLongClick = { fixedIncome ->
                // 长按显示操作菜单
                showOptionsDialog(fixedIncome)
            }
        )

        binding.rvFixedIncomes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FixedIncomeFragment.adapter
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val filterType = when (tab?.position) {
                    0 -> FixedIncomeFilterType.ALL
                    1 -> FixedIncomeFilterType.INCOME
                    2 -> FixedIncomeFilterType.EXPENSE
                    else -> FixedIncomeFilterType.ALL
                }
                viewModel.setFilterType(filterType)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
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
                    // 更新统计卡片
                    updateSummaryCard(state)

                    // 更新列表
                    adapter.submitList(state.filteredFixedIncomes)

                    // 显示/隐藏空状态
                    if (state.filteredFixedIncomes.isEmpty()) {
                        binding.rvFixedIncomes.visibility = View.GONE
                        binding.emptyView.visibility = View.VISIBLE
                    } else {
                        binding.rvFixedIncomes.visibility = View.VISIBLE
                        binding.emptyView.visibility = View.GONE
                    }

                    // 显示添加弹窗
                    if (state.showAddDialog && addFixedIncomeDialog?.isShowing != true) {
                        showAddFixedIncomeDialog()
                        viewModel.hideAddDialog()
                    }
                }
            }
        }
    }

    private fun updateSummaryCard(state: FixedIncomeUiState) {
        // 每分钟总变动
        val netPerMinute = state.netPerMinute
        binding.tvPerMinuteTotal.text = if (netPerMinute >= 0) {
            "+¥${perMinuteFormat.format(netPerMinute)}"
        } else {
            "-¥${perMinuteFormat.format(-netPerMinute)}"
        }
        binding.tvPerMinuteTotal.setTextColor(
            requireContext().getColor(
                if (netPerMinute >= 0) android.R.color.holo_green_dark
                else android.R.color.holo_red_dark
            )
        )

        // 每分钟收入
        binding.tvIncomePerMinute.text = "+¥${perMinuteFormat.format(state.incomePerMinute)}"

        // 每分钟支出
        binding.tvExpensePerMinute.text = "-¥${perMinuteFormat.format(state.expensePerMinute)}"
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collectLatest { event ->
                    when (event) {
                        is FixedIncomeUiEvent.ShowMessage -> {
                            Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private var addFixedIncomeDialog: BottomSheetDialog? = null

    private fun showAddFixedIncomeDialog() {
        if (addFixedIncomeDialog?.isShowing == true) return

        val dialog = AddFixedIncomeBottomSheet(
            context = requireContext(),
            onSave = { name, amount, type, frequency, startDate, endDate ->
                viewModel.addFixedIncome(name, amount, type, frequency, startDate, endDate)
            },
            onDismiss = {
                addFixedIncomeDialog = null
            }
        )

        addFixedIncomeDialog = dialog
        dialog.show()
    }

    /**
     * 显示详情对话框
     */
    private fun showDetailDialog(fixedIncome: FixedIncome) {
        val typeText = if (fixedIncome.type == com.example.funnyexpensetracking.domain.model.FixedIncomeType.INCOME) "固定收入" else "固定支出"
        val frequencyText = when (fixedIncome.frequency) {
            com.example.funnyexpensetracking.domain.model.FixedIncomeFrequency.DAILY -> "每日"
            com.example.funnyexpensetracking.domain.model.FixedIncomeFrequency.WEEKLY -> "每周"
            com.example.funnyexpensetracking.domain.model.FixedIncomeFrequency.MONTHLY -> "每月"
            com.example.funnyexpensetracking.domain.model.FixedIncomeFrequency.YEARLY -> "每年"
        }
        val statusText = if (fixedIncome.isActive) "生效中" else "已停用"

        val message = buildString {
            appendLine("类型: $typeText")
            appendLine("频率: $frequencyText")
            appendLine("周期金额: ¥${String.format("%.2f", fixedIncome.amount)}")
            appendLine("每分钟: ¥${perMinuteFormat.format(fixedIncome.getAmountPerMinute())}")
            appendLine("累计时间: ${fixedIncome.getFormattedAccumulatedTime()}")
            appendLine("累计金额: ¥${String.format("%.2f", fixedIncome.accumulatedAmount)}")
            appendLine("状态: $statusText")
        }

        AlertDialog.Builder(requireContext())
            .setTitle(fixedIncome.name)
            .setMessage(message)
            .setPositiveButton("确定", null)
            .setNegativeButton("操作") { _, _ ->
                showOptionsDialog(fixedIncome)
            }
            .show()
    }

    /**
     * 显示操作选项对话框（只有停用和删除）
     */
    private fun showOptionsDialog(fixedIncome: FixedIncome) {
        val options = if (fixedIncome.isActive) {
            arrayOf("停用", "删除")
        } else {
            arrayOf("删除")  // 已停用的条目只能删除
        }

        AlertDialog.Builder(requireContext())
            .setTitle(fixedIncome.name)
            .setItems(options) { _, which ->
                if (fixedIncome.isActive) {
                    when (which) {
                        0 -> showDeactivateConfirmDialog(fixedIncome)
                        1 -> showDeleteConfirmDialog(fixedIncome)
                    }
                } else {
                    // 已停用，只有删除选项
                    showDeleteConfirmDialog(fixedIncome)
                }
            }
            .show()
    }

    /**
     * 显示停用确认对话框
     */
    private fun showDeactivateConfirmDialog(fixedIncome: FixedIncome) {
        AlertDialog.Builder(requireContext())
            .setTitle("停用确认")
            .setMessage("停用后将不再累计「${fixedIncome.name}」的金额，但历史累计数据会保留。\n\n确定要停用吗？")
            .setPositiveButton("停用") { _, _ ->
                viewModel.deactivateFixedIncome(fixedIncome)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(fixedIncome: FixedIncome) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除确认")
            .setMessage("删除后所有数据将无法恢复。\n\n确定要删除「${fixedIncome.name}」吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteFixedIncome(fixedIncome)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        addFixedIncomeDialog?.dismiss()
        _binding = null
    }
}

