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
                showOptionsDialog(fixedIncome)
            },
            onItemLongClick = { fixedIncome ->
                showDeleteConfirmDialog(fixedIncome)
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

                    // 显示添加弹窗（只在状态变为true时显示一次）
                    if (state.showAddDialog && addFixedIncomeDialog?.isShowing != true) {
                        showAddFixedIncomeDialog(state.editingFixedIncome)
                        // 立即重置状态，防止重复显示
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

    private fun showAddFixedIncomeDialog(editingFixedIncome: FixedIncome?) {
        if (addFixedIncomeDialog?.isShowing == true) return

        val dialog = AddFixedIncomeBottomSheet(
            context = requireContext(),
            editingFixedIncome = editingFixedIncome,
            onSave = { name, amount, type, frequency, startDate, accumulatedAmount ->
                if (editingFixedIncome != null) {
                    viewModel.updateFixedIncome(
                        id = editingFixedIncome.id,
                        name = name,
                        amount = amount,
                        type = type,
                        frequency = frequency,
                        startDate = startDate,
                        accumulatedAmount = accumulatedAmount
                    )
                } else {
                    viewModel.addFixedIncome(name, amount, type, frequency, startDate, accumulatedAmount)
                }
            },
            onDismiss = {
                // 状态已在显示弹窗时重置，这里只需清理引用
                addFixedIncomeDialog = null
            }
        )

        addFixedIncomeDialog = dialog
        dialog.show()
    }

    private fun showOptionsDialog(fixedIncome: FixedIncome) {
        val options = arrayOf(
            "编辑",
            if (fixedIncome.isActive) "停用" else "启用",
            "删除"
        )

        AlertDialog.Builder(requireContext())
            .setTitle(fixedIncome.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.editFixedIncome(fixedIncome)
                    1 -> viewModel.toggleFixedIncomeStatus(fixedIncome)
                    2 -> showDeleteConfirmDialog(fixedIncome)
                }
            }
            .show()
    }

    private fun showDeleteConfirmDialog(fixedIncome: FixedIncome) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除确认")
            .setMessage("确定要删除「${fixedIncome.name}」吗？")
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

