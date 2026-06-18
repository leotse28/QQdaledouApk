package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DaledouAccount
import com.example.network.DaledouRunner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DaledouViewModel(application: Application) : AndroidViewModel(application) {

    private val accountDao = AppDatabase.getDatabase(application).accountDao()

    val accountsState: StateFlow<List<DaledouAccount>> = accountDao.getAllAccountsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _isAutomationRunning = MutableStateFlow(false)
    val isAutomationRunning: StateFlow<Boolean> = _isAutomationRunning.asStateFlow()

    private val _selectedTasks = MutableStateFlow<Set<String>>(
        setOf("每日签到", "大笨钟", "幸运金蛋", "帮派巡礼", "邪神秘宝", "帮派宝库")
    )
    val selectedTasks: StateFlow<Set<String>> = _selectedTasks.asStateFlow()

    // Custom Command states
    private val _customCommandPath = MutableStateFlow("cmd=dailyAct")
    val customCommandPath: StateFlow<String> = _customCommandPath.asStateFlow()

    private val _customQueryResult = MutableStateFlow("")
    val customQueryResult: StateFlow<String> = _customQueryResult.asStateFlow()

    private val _isCustomRunning = MutableStateFlow(false)
    val isCustomRunning: StateFlow<Boolean> = _isCustomRunning.asStateFlow()

    fun updateCustomCommandPath(path: String) {
        _customCommandPath.value = path
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    private fun addLog(message: String) {
        val currentList = _logs.value.toMutableList()
        currentList.add(message)
        _logs.value = currentList
    }

    fun toggleTask(task: String) {
        val currentSet = _selectedTasks.value.toMutableSet()
        if (currentSet.contains(task)) {
            currentSet.remove(task)
        } else {
            currentSet.add(task)
        }
        _selectedTasks.value = currentSet
    }

    fun addAccount(qq: String, description: String, cookies: String) {
        viewModelScope.launch {
            val account = DaledouAccount(
                qq = qq.trim(),
                description = description.trim().ifEmpty { "QQ Account" },
                cookieString = cookies.trim()
            )
            accountDao.insertAccount(account)
        }
    }

    fun updateAccount(account: DaledouAccount) {
        viewModelScope.launch {
            accountDao.updateAccount(account)
        }
    }

    fun deleteAccount(account: DaledouAccount) {
        viewModelScope.launch {
            accountDao.deleteAccount(account)
        }
    }

    fun runAutomation(account: DaledouAccount) {
        if (_isAutomationRunning.value) return
        _isAutomationRunning.value = true
        
        viewModelScope.launch {
            try {
                DaledouRunner.runAutomation(
                    account = account,
                    tasksToRun = _selectedTasks.value.toList(),
                    onLog = { logMessage ->
                        addLog(logMessage)
                    }
                )
            } finally {
                _isAutomationRunning.value = false
            }
        }
    }

    fun runCustomCommand(account: DaledouAccount) {
        if (_isCustomRunning.value) return
        val path = _customCommandPath.value.trim()
        if (path.isEmpty()) return

        _isCustomRunning.value = true
        _customQueryResult.value = "Sending request to game server..."

        viewModelScope.launch {
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            addLog("[$timestamp] [Custom Command] Sending: $path")
            
            val rawResult = DaledouRunner.executeSingleCommand(account, path)
            _customQueryResult.value = rawResult
            _isCustomRunning.value = false
            
            addLog("[$timestamp] [Custom Command] Response completed.")
        }
    }
}
