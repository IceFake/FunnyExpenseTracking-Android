package com.example.funnyexpensetracking.ui.financialquery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.funnyexpensetracking.databinding.FragmentFinancialQueryBinding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 自然语言财务查询Fragment
 * 提供聊天式界面，支持用户用自然语言查询财务状况
 */
@AndroidEntryPoint
class FinancialQueryFragment : Fragment() {

    private var _binding: FragmentFinancialQueryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FinancialQueryViewModel by viewModels()

    private lateinit var chatAdapter: ChatMessageAdapter
    private var apiKeyDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFinancialQueryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        setupInputListeners()
        observeState()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatMessageAdapter()
        binding.rvChatMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        binding.btnApiKey.setOnClickListener {
            viewModel.openApiKeyDialog()
            showApiKeyDialog()
        }

        binding.btnClearChat.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("清空对话")
                .setMessage("确定要清空所有对话记录吗？")
                .setPositiveButton("确定") { _, _ ->
                    viewModel.clearChat()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun setupInputListeners() {
        binding.etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }

    private fun sendMessage() {
        val text = binding.etInput.text?.toString()?.trim()
        if (!text.isNullOrBlank()) {
            viewModel.sendQuery(text)
            binding.etInput.text?.clear()
        }
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
                            is FinancialQueryUiEvent.ShowMessage -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                            is FinancialQueryUiEvent.ShowApiKeyDialog -> {
                                showApiKeyDialog()
                            }
                            is FinancialQueryUiEvent.ScrollToBottom -> {
                                scrollToBottom()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateUi(state: FinancialQueryUiState) {
        // 更新消息列表
        chatAdapter.submitList(state.messages) {
            if (state.messages.isNotEmpty()) {
                scrollToBottom()
            }
        }

        // 更新加载指示器
        binding.loadingIndicator.isVisible = state.isLoading

        // 更新发送按钮状态
        binding.btnSend.isEnabled = !state.isLoading

        // 更新快捷问题芯片
        updateSuggestionChips(state.suggestedQuestions)
    }

    /**
     * 更新快捷问题芯片组
     */
    private var chipsInitialized = false
    private fun updateSuggestionChips(suggestions: List<String>) {
        if (chipsInitialized) return
        chipsInitialized = true

        binding.chipGroupSuggestions.removeAllViews()
        suggestions.forEach { question ->
            val chip = Chip(requireContext()).apply {
                text = question
                isClickable = true
                isCheckable = false
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#E3F2FD")
                )
                setTextColor(android.graphics.Color.parseColor("#1976D2"))
                textSize = 13f
                chipStrokeWidth = 0f
                setOnClickListener {
                    viewModel.sendQuery(question)
                }
            }
            binding.chipGroupSuggestions.addView(chip)
        }
    }

    private fun scrollToBottom() {
        binding.rvChatMessages.post {
            val itemCount = chatAdapter.itemCount
            if (itemCount > 0) {
                binding.rvChatMessages.smoothScrollToPosition(itemCount - 1)
            }
        }
    }

    /**
     * 显示API Key设置弹窗
     */
    private fun showApiKeyDialog() {
        if (apiKeyDialog?.isShowing == true) return

        val editText = EditText(requireContext()).apply {
            hint = "请输入DeepSeek API Key"
            setText(viewModel.uiState.value.currentApiKey)
            setPadding(48, 32, 48, 32)
            isSingleLine = true
        }

        apiKeyDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("🔑 API Key 设置")
            .setMessage("请输入您的DeepSeek API Key\n获取地址: platform.deepseek.com")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val apiKey = editText.text.toString().trim()
                if (apiKey.isNotBlank()) {
                    viewModel.saveApiKey(apiKey)
                } else {
                    Toast.makeText(requireContext(), "API Key不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消") { _, _ ->
                viewModel.dismissApiKeyDialog()
            }
            .setOnDismissListener {
                viewModel.dismissApiKeyDialog()
            }
            .create()

        apiKeyDialog?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        apiKeyDialog?.dismiss()
        apiKeyDialog = null
        _binding = null
    }
}



