package com.example.funnyexpensetracking.ui.usercenter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.funnyexpensetracking.R
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint

/**
 * 用户中心Fragment
 */
@AndroidEntryPoint
class UserCenterFragment : Fragment() {

    // Views
    private lateinit var cardImportExport: MaterialCardView

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
    }

    private fun initViews(view: View) {
        cardImportExport = view.findViewById(R.id.cardImportExport)
    }

    private fun setupClickListeners() {
        cardImportExport.setOnClickListener {
            onImportExportClick()
        }
    }

    private fun onImportExportClick() {
        // TODO: 实现导入/导出数据功能
        // 实现该选项功能，可以将所有存储在本地的数据导出为一个方便的文件，同时也能通过加载该文件新增条目到当前数据
        Toast.makeText(requireContext(), "导入/导出数据功能即将推出", Toast.LENGTH_SHORT).show()
    }
}

