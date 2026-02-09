package com.example.funnyexpensetracking.ui.usercenter

import com.example.funnyexpensetracking.domain.repository.ImportResult

/**
 * 用户中心数据管理状态
 */
sealed class DataManagementState {
    object Idle : DataManagementState()
    object Loading : DataManagementState()
    data class ExportSuccess(val filePath: String) : DataManagementState()
    data class ImportSuccess(val result: ImportResult) : DataManagementState()
    object ClearSuccess : DataManagementState()
    data class Error(val message: String) : DataManagementState()
}

/**
 * 用户中心事件
 */
sealed class DataManagementEvent {
    object ExportData : DataManagementEvent()
    data class ImportData(val jsonContent: String) : DataManagementEvent()
    object ClearData : DataManagementEvent()
    object ResetState : DataManagementEvent()
}

