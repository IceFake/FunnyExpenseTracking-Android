package com.example.funnyexpensetracking.ui.usercenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.funnyexpensetracking.domain.model.BackupData
import com.example.funnyexpensetracking.domain.repository.DataManagementRepository
import com.example.funnyexpensetracking.domain.usecase.RealtimeAssetCalculator
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 用户中心ViewModel
 * 处理数据导入/导出/清除功能
 */
@HiltViewModel
class UserCenterViewModel @Inject constructor(
    private val dataManagementRepository: DataManagementRepository,
    private val realtimeAssetCalculator: RealtimeAssetCalculator
) : ViewModel() {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .create()

    private val _state = MutableStateFlow<DataManagementState>(DataManagementState.Idle)
    val state: StateFlow<DataManagementState> = _state.asStateFlow()

    private val _exportData = MutableStateFlow<String?>(null)
    val exportData: StateFlow<String?> = _exportData.asStateFlow()

    /**
     * 处理事件
     */
    fun handleEvent(event: DataManagementEvent) {
        when (event) {
            is DataManagementEvent.ExportData -> exportData()
            is DataManagementEvent.ImportData -> importData(event.jsonContent)
            is DataManagementEvent.ClearData -> clearData()
            is DataManagementEvent.ResetState -> resetState()
        }
    }

    /**
     * 导出所有数据
     */
    private fun exportData() {
        viewModelScope.launch {
            _state.value = DataManagementState.Loading
            try {
                val backupData = dataManagementRepository.exportAllData()
                val jsonContent = gson.toJson(backupData)
                _exportData.value = jsonContent
                _state.value = DataManagementState.ExportSuccess("数据已准备导出")
            } catch (e: Exception) {
                _state.value = DataManagementState.Error("导出失败: ${e.message}")
            }
        }
    }

    /**
     * 导入数据
     */
    private fun importData(jsonContent: String) {
        viewModelScope.launch {
            _state.value = DataManagementState.Loading
            try {
                val backupData = gson.fromJson(jsonContent, BackupData::class.java)
                if (backupData == null) {
                    _state.value = DataManagementState.Error("导入失败: 无效的备份文件格式")
                    return@launch
                }

                val result = dataManagementRepository.importData(backupData)
                // 导入后重新计算总资产
                realtimeAssetCalculator.recalculateAsset()
                _state.value = DataManagementState.ImportSuccess(result)
            } catch (e: Exception) {
                _state.value = DataManagementState.Error("导入失败: ${e.message}")
            }
        }
    }

    /**
     * 清除所有数据
     */
    private fun clearData() {
        viewModelScope.launch {
            _state.value = DataManagementState.Loading
            try {
                dataManagementRepository.clearAllData()
                // 清除后重新计算总资产（此时应该为0）
                realtimeAssetCalculator.recalculateAsset()
                _state.value = DataManagementState.ClearSuccess
            } catch (e: Exception) {
                _state.value = DataManagementState.Error("清除失败: ${e.message}")
            }
        }
    }

    /**
     * 重置状态
     */
    private fun resetState() {
        _state.value = DataManagementState.Idle
        _exportData.value = null
    }

    /**
     * 清除导出数据缓存
     */
    fun clearExportData() {
        _exportData.value = null
    }
}

