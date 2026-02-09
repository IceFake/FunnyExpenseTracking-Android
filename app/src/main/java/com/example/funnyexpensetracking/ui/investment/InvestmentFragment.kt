package com.example.funnyexpensetracking.ui.investment

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
import com.example.funnyexpensetracking.databinding.FragmentInvestmentBinding
import com.example.funnyexpensetracking.domain.model.Investment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DecimalFormat

/**
 * 理财/投资管理Fragment
 */
@AndroidEntryPoint
class InvestmentFragment : Fragment() {

    private var _binding: FragmentInvestmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InvestmentViewModel by viewModels()
    private lateinit var adapter: InvestmentAdapter

    private val currencyFormat = DecimalFormat("#,##0.00")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInvestmentBinding.inflate(inflater, container, false)
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
        adapter = InvestmentAdapter(
            onItemClick = { investment ->
                showOptionsDialog(investment)
            },
            onItemLongClick = { investment ->
                showDeleteConfirmDialog(investment)
            }
        )

        binding.rvInvestments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@InvestmentFragment.adapter
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val filterType = when (tab?.position) {
                    0 -> InvestmentFilterType.ALL
                    1 -> InvestmentFilterType.STOCK
                    2 -> InvestmentFilterType.OTHER
                    else -> InvestmentFilterType.ALL
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
                    adapter.submitList(state.filteredInvestments)

                    // 显示/隐藏空状态
                    if (state.filteredInvestments.isEmpty()) {
                        binding.rvInvestments.visibility = View.GONE
                        binding.emptyView.visibility = View.VISIBLE
                    } else {
                        binding.rvInvestments.visibility = View.VISIBLE
                        binding.emptyView.visibility = View.GONE
                    }

                    // 显示添加弹窗（只在状态变为true时显示一次）
                    if (state.showAddDialog && !isDialogShowing) {
                        showAddInvestmentDialog(state.editingInvestment)
                        // 立即重置状态，防止重复显示
                        viewModel.hideAddDialog()
                    }
                }
            }
        }
    }

    private fun updateSummaryCard(state: InvestmentUiState) {
        // 总价值
        binding.tvTotalValue.text = "¥${currencyFormat.format(state.totalCurrentValue)}"

        // 总投入
        binding.tvTotalInvestment.text = "投入: ¥${currencyFormat.format(state.totalInvestment)}"

        // 总盈亏
        val profitLoss = state.totalProfitLoss
        if (profitLoss >= 0) {
            binding.tvTotalProfitLoss.text = "+¥${currencyFormat.format(profitLoss)}"
            binding.tvTotalProfitLoss.setTextColor(
                requireContext().getColor(android.R.color.holo_green_dark)
            )
        } else {
            binding.tvTotalProfitLoss.text = "-¥${currencyFormat.format(-profitLoss)}"
            binding.tvTotalProfitLoss.setTextColor(
                requireContext().getColor(android.R.color.holo_red_dark)
            )
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collectLatest { event ->
                    when (event) {
                        is InvestmentUiEvent.ShowMessage -> {
                            Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private var addInvestmentDialog: BottomSheetDialog? = null
    private var isDialogShowing = false

    private fun showAddInvestmentDialog(editingInvestment: Investment?) {
        if (isDialogShowing || addInvestmentDialog?.isShowing == true) return
        isDialogShowing = true

        val dialog = AddInvestmentBottomSheet(
            context = requireContext(),
            editingInvestment = editingInvestment,
            onSave = { category, description, quantity, investment, currentValue ->
                if (editingInvestment != null) {
                    viewModel.updateInvestment(
                        id = editingInvestment.id,
                        category = category,
                        description = description,
                        quantity = quantity,
                        investment = investment,
                        currentValue = currentValue
                    )
                } else {
                    viewModel.addInvestment(category, description, quantity, investment, currentValue)
                }
            },
            onDismiss = {
                // 状态已在显示弹窗时重置，这里只需清理引用
                addInvestmentDialog = null
                isDialogShowing = false
            }
        )

        addInvestmentDialog = dialog
        dialog.show()
    }

    private fun showOptionsDialog(investment: Investment) {
        val options = arrayOf("编辑", "删除")

        AlertDialog.Builder(requireContext())
            .setTitle(investment.description)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.editInvestment(investment)
                    1 -> showDeleteConfirmDialog(investment)
                }
            }
            .show()
    }

    private fun showDeleteConfirmDialog(investment: Investment) {
        AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("确定要删除「${investment.description}」吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteInvestment(investment)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

