package com.example.funnyexpensetracking.ui.usercenter

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.funnyexpensetracking.R
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 用户中心Fragment
 */
@AndroidEntryPoint
class UserCenterFragment : Fragment() {

    private val viewModel: UserCenterViewModel by viewModels()

    // Views
    private lateinit var cardExportData: MaterialCardView
    private lateinit var cardImportData: MaterialCardView
    private lateinit var cardClearData: MaterialCardView
    private lateinit var progressBar: ProgressBar

    // 文件选择器 - 导出
    private val exportFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { writeExportDataToFile(it) }
    }

    // 文件选择器 - 导入
    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { readImportDataFromFile(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_center, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupClickListeners()
        observeState()
    }

    private fun initViews(view: View) {
        cardExportData = view.findViewById(R.id.cardExportData)
        cardImportData = view.findViewById(R.id.cardImportData)
        cardClearData = view.findViewById(R.id.cardClearData)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        cardExportData.setOnClickListener {
            onExportDataClick()
        }

        cardImportData.setOnClickListener {
            onImportDataClick()
        }

        cardClearData.setOnClickListener {
            onClearDataClick()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is DataManagementState.Idle -> {
                            progressBar.visibility = View.GONE
                        }
                        is DataManagementState.Loading -> {
                            progressBar.visibility = View.VISIBLE
                        }
                        is DataManagementState.ExportSuccess -> {
                            progressBar.visibility = View.GONE
                            // 数据已准备好，现在打开文件选择器
                            openExportFilePicker()
                        }
                        is DataManagementState.ImportSuccess -> {
                            progressBar.visibility = View.GONE
                            showImportResultDialog(state.result)
                            viewModel.handleEvent(DataManagementEvent.ResetState)
                        }
                        is DataManagementState.ClearSuccess -> {
                            progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), "所有数据已清除", Toast.LENGTH_SHORT).show()
                            viewModel.handleEvent(DataManagementEvent.ResetState)
                        }
                        is DataManagementState.Error -> {
                            progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            viewModel.handleEvent(DataManagementEvent.ResetState)
                        }
                    }
                }
            }
        }
    }

    private fun onExportDataClick() {
        AlertDialog.Builder(requireContext())
            .setTitle("导出数据")
            .setMessage("将导出所有数据到一个JSON文件，您可以使用该文件在其他设备上恢复数据。")
            .setPositiveButton("导出") { _, _ ->
                viewModel.handleEvent(DataManagementEvent.ExportData)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun onImportDataClick() {
        AlertDialog.Builder(requireContext())
            .setTitle("导入数据")
            .setMessage("从备份文件导入数据。导入时只增加新数据，相同项目会进行合并：\n\n" +
                    "• 账户：相同名称的账户余额会叠加\n" +
                    "• 投资：相同项目的数量和金额会叠加\n" +
                    "• 股票：相同股票的持仓会合并\n" +
                    "• 交易记录：全部新增，不会去重")
            .setPositiveButton("选择文件") { _, _ ->
                importFileLauncher.launch("application/json")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun onClearDataClick() {
        AlertDialog.Builder(requireContext())
            .setTitle("清除所有数据")
            .setMessage("⚠️ 警告：此操作将删除所有本地数据，包括：\n\n" +
                    "• 所有交易记录\n" +
                    "• 所有账户信息\n" +
                    "• 所有固定收支\n" +
                    "• 所有投资记录\n" +
                    "• 所有股票持仓\n\n" +
                    "此操作不可恢复！建议先导出备份。")
            .setPositiveButton("清除") { _, _ ->
                // 二次确认
                AlertDialog.Builder(requireContext())
                    .setTitle("最终确认")
                    .setMessage("确定要删除所有数据吗？此操作无法撤销！")
                    .setPositiveButton("确定删除") { _, _ ->
                        viewModel.handleEvent(DataManagementEvent.ClearData)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun openExportFilePicker() {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "FET_backup_${dateFormat.format(Date())}.json"
        exportFileLauncher.launch(fileName)
    }

    private fun writeExportDataToFile(uri: Uri) {
        val exportData = viewModel.exportData.value
        if (exportData == null) {
            Toast.makeText(requireContext(), "没有可导出的数据", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(exportData.toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(requireContext(), "数据导出成功", Toast.LENGTH_SHORT).show()
            viewModel.clearExportData()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            viewModel.handleEvent(DataManagementEvent.ResetState)
        }
    }

    private fun readImportDataFromFile(uri: Uri) {
        try {
            val jsonContent = requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }

            if (jsonContent.isNullOrBlank()) {
                Toast.makeText(requireContext(), "文件为空或无法读取", Toast.LENGTH_SHORT).show()
                return
            }

            viewModel.handleEvent(DataManagementEvent.ImportData(jsonContent))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "读取文件失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showImportResultDialog(result: com.example.funnyexpensetracking.domain.repository.ImportResult) {
        val message = StringBuilder()
        message.appendLine("导入完成！")
        message.appendLine()
        message.appendLine("新增数据：")
        if (result.transactionsAdded > 0) message.appendLine("  • 交易记录: ${result.transactionsAdded} 条")
        if (result.accountsAdded > 0) message.appendLine("  • 账户: ${result.accountsAdded} 个")
        if (result.fixedIncomesAdded > 0) message.appendLine("  • 固定收支: ${result.fixedIncomesAdded} 项")
        if (result.investmentsAdded > 0) message.appendLine("  • 投资: ${result.investmentsAdded} 项")
        if (result.stockHoldingsAdded > 0) message.appendLine("  • 股票持仓: ${result.stockHoldingsAdded} 项")
        if (result.assetSnapshotsAdded > 0) message.appendLine("  • 资产快照: ${result.assetSnapshotsAdded} 条")

        if (result.getTotalMerged() > 0) {
            message.appendLine()
            message.appendLine("合并数据：")
            if (result.accountsMerged > 0) message.appendLine("  • 账户: ${result.accountsMerged} 个")
            if (result.fixedIncomesMerged > 0) message.appendLine("  • 固定收支: ${result.fixedIncomesMerged} 项")
            if (result.investmentsMerged > 0) message.appendLine("  • 投资: ${result.investmentsMerged} 项")
            if (result.stockHoldingsMerged > 0) message.appendLine("  • 股票持仓: ${result.stockHoldingsMerged} 项")
        }

        if (result.hasErrors()) {
            message.appendLine()
            message.appendLine("警告：")
            result.errors.take(3).forEach { error ->
                message.appendLine("  • $error")
            }
            if (result.errors.size > 3) {
                message.appendLine("  • ...还有 ${result.errors.size - 3} 条警告")
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("导入结果")
            .setMessage(message.toString())
            .setPositiveButton("确定", null)
            .show()
    }
}

