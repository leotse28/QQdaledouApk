package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DaledouAccount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaledouApp(viewModel: DaledouViewModel) {
    val accounts by viewModel.accountsState.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val isAutomationRunning by viewModel.isAutomationRunning.collectAsStateWithLifecycle()
    val selectedTasks by viewModel.selectedTasks.collectAsStateWithLifecycle()
    val customCommandPath by viewModel.customCommandPath.collectAsStateWithLifecycle()
    val customQueryResult by viewModel.customQueryResult.collectAsStateWithLifecycle()
    val isCustomRunning by viewModel.isCustomRunning.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Accounts, 1: Tasks, 2: Command/Logs
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedAccountForAutomation by remember { mutableStateOf<DaledouAccount?>(null) }
    var accountToEdit by remember { mutableStateOf<DaledouAccount?>(null) }

    // Synchronize default selected account
    LaunchedEffect(accounts) {
        if (selectedAccountForAutomation == null && accounts.isNotEmpty()) {
            selectedAccountForAutomation = accounts.firstOrNull()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.SportsMartialArts,
                            contentDescription = "Martial Arts Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = "大乐斗助手",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Daledou Companion",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    if (activeTab == 2) {
                        IconButton(
                            onClick = { viewModel.clearLogs() },
                            modifier = Modifier.testTag("clear_logs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear logs"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 0) Icons.Filled.People else Icons.Outlined.People,
                            contentDescription = "Manage Accounts"
                        )
                    },
                    label = { Text("账号管理") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 1) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircle,
                            contentDescription = "Automation Hub"
                        )
                    },
                    label = { Text("挂机大厅") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (activeTab == 2) Icons.Filled.Terminal else Icons.Outlined.Terminal,
                            contentDescription = "Console Logs"
                        )
                    },
                    label = { Text("指令与日志") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            when (activeTab) {
                0 -> {
                    AccountManagementScreen(
                        accounts = accounts,
                        onAddClick = { showAddDialog = true },
                        onDeleteClick = { viewModel.deleteAccount(it) },
                        onEditClick = { accountToEdit = it },
                        modifier = Modifier
                    )
                }

                1 -> {
                    AutomationScreen(
                        accounts = accounts,
                        selectedAccount = selectedAccountForAutomation,
                        onAccountSelected = { selectedAccountForAutomation = it },
                        selectedTasks = selectedTasks,
                        onTaskToggle = { viewModel.toggleTask(it) },
                        isAutomationRunning = isAutomationRunning,
                        onStartAutomation = { account ->
                            viewModel.runAutomation(account)
                            activeTab = 2 // Auto route to terminal
                        },
                        modifier = Modifier
                    )
                }

                2 -> {
                    ConsoleLogAndCommandScreen(
                        accounts = accounts,
                        selectedAccount = selectedAccountForAutomation,
                        onAccountSelected = { selectedAccountForAutomation = it },
                        customCommandPath = customCommandPath,
                        onCommandChange = { viewModel.updateCustomCommandPath(it) },
                        customQueryResult = customQueryResult,
                        isCustomRunning = isCustomRunning,
                        onRunCommand = { account -> viewModel.runCustomCommand(account) },
                        logs = logs,
                        modifier = Modifier
                    )
                }
            }
        }
    }

    // Add Account Dialog
    if (showAddDialog) {
        AccountEditDialog(
            title = "新增账号 Setup Account",
            qqInitial = "",
            descInitial = "",
            cookiesInitial = "",
            onDismiss = { showAddDialog = false },
            onSave = { qq, desc, cookies ->
                viewModel.addAccount(qq, desc, cookies)
                showAddDialog = false
            }
        )
    }

    // Edit Account Dialog
    if (accountToEdit != null) {
        val account = accountToEdit!!
        AccountEditDialog(
            title = "修改账号 Edit Account",
            qqInitial = account.qq,
            descInitial = account.description,
            cookiesInitial = account.cookieString,
            onDismiss = { accountToEdit = null },
            onSave = { qq, desc, cookies ->
                viewModel.updateAccount(
                    account.copy(qq = qq, description = desc, cookieString = cookies)
                )
                accountToEdit = null
            }
        )
    }
}

@Composable
fun AccountManagementScreen(
    accounts: List<DaledouAccount>,
    onAddClick: () -> Unit,
    onDeleteClick: (DaledouAccount) -> Unit,
    onEditClick: (DaledouAccount) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "游戏账号库",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "共收录 ${accounts.size} 个乐斗账号",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("add_account_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
                Spacer(modifier = Modifier.width(4.dp))
                Text("配置账号", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.People,
                        contentDescription = "Empty Accounts",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无账号配置",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "点击上方“配置账号”输入QQ与Cookie即可开始挂机！",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(accounts) { account ->
                    AccountItemCard(
                        account = account,
                        onDelete = { onDeleteClick(account) },
                        onEdit = { onEditClick(account) }
                    )
                }
            }
        }
    }
}

@Composable
fun AccountItemCard(
    account: DaledouAccount,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("account_card_${account.qq}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "QQ Avatar Placeholder",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.qq,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = account.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AssistChip(
                    onClick = {},
                    label = { Text("Cookie已配置", fontSize = 11.sp, color = Color(0xFF2E7D32)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onEdit,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("edit_account_${account.qq}")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Account", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("修改配置")
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_account_${account.qq}")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Account", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除")
                }
            }
        }
    }
}

@Composable
fun AutomationScreen(
    accounts: List<DaledouAccount>,
    selectedAccount: DaledouAccount?,
    onAccountSelected: (DaledouAccount) -> Unit,
    selectedTasks: Set<String>,
    onTaskToggle: (String) -> Unit,
    isAutomationRunning: Boolean,
    onStartAutomation: (DaledouAccount) -> Unit,
    modifier: Modifier = Modifier
) {
    val tasksList = listOf("每日签到", "大笨钟", "幸运金蛋", "帮派巡礼", "邪神秘宝", "帮派宝库")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "自动化挂机大厅",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "勾选所要挂机运行的任务，一键在后台多层交互执行。",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "请先配置账号，再启动挂机大厅！",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "选择执行账号 Select Account",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    var expanded by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedAccount?.let { "${it.qq} (${it.description})" } ?: "未选择账号",
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            }
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text("${account.qq} (${account.description})") },
                                    onClick = {
                                        onAccountSelected(account)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "选择挂机活动 Select Tasks",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Task list checkboxes
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                LazyColumn(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(tasksList) { task ->
                        val isChecked = selectedTasks.contains(task)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onTaskToggle(task) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { onTaskToggle(task) },
                                modifier = Modifier.testTag("checkbox_$task")
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isChecked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = when (task) {
                                        "每日签到" -> "自动完成游戏首页签到与日常礼包领取"
                                        "大笨钟" -> "中午/晚上领取大笨钟整点乐斗次数与奖励"
                                        "幸运金蛋" -> "免费探测金蛋位置并完成砸开"
                                        "帮派巡礼" -> "自动领取深渊狂潮中的帮派福利赠礼"
                                        "邪神秘宝" -> "每日免费抽取高级和极品邪神秘宝"
                                        "帮派宝库" -> "自动解析可用礼包索引完成一键商会福利领取"
                                        else -> ""
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { selectedAccount?.let { onStartAutomation(it) } },
                enabled = selectedAccount != null && selectedTasks.isNotEmpty() && !isAutomationRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("run_automation_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isAutomationRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("整备武艺中...", fontSize = 16.sp)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play Icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("开始自动挂机 (一键乐斗)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ConsoleLogAndCommandScreen(
    accounts: List<DaledouAccount>,
    selectedAccount: DaledouAccount?,
    onAccountSelected: (DaledouAccount) -> Unit,
    customCommandPath: String,
    onCommandChange: (String) -> Unit,
    customQueryResult: String,
    isCustomRunning: Boolean,
    onRunCommand: (DaledouAccount) -> Unit,
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (accounts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无账号可用，指令发送器与控制台关闭。")
            }
            return
        }

        // Custom Command sender Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "手动执行单条游戏指令 Custom Command",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customCommandPath,
                        onValueChange = onCommandChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_command_input"),
                        label = { Text("URL参数 (如 cmd=dailyAct)", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { selectedAccount?.let { onRunCommand(it) } },
                        enabled = selectedAccount != null && customCommandPath.isNotEmpty() && !isCustomRunning,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("send_custom_command_button")
                    ) {
                        if (isCustomRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("发送")
                        }
                    }
                }

                if (customQueryResult.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "指令回复 Response:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 100.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            item {
                                Text(
                                    text = customQueryResult,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logging header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "实时挂机控制台 stdout log",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            FilledTonalIconChip(
                label = "LIVE TERMINAL",
                color = Color(0xFFA1887F)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Log screen Styled like retro green/amber Terminal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFF333333), shape = RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "控制台暂无输出。\n请点击 ➔“挂机大厅”➔“开始自动挂机”来进行操作测试！",
                        color = Color(0xFF888888),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                val listState = rememberLazyListState()

                // Auto Scroll to Bottom when logs change
                LaunchedEffect(logs.size) {
                    if (logs.isNotEmpty()) {
                        listState.animateScrollToItem(logs.size - 1)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        val color = when {
                            log.contains("✖") || log.contains("Error") -> Color(0xFFE57373) // soft red
                            log.contains("✔") || log.contains("成功") -> Color(0xFF81C784) // soft green
                            log.contains("➔") || log.contains("正在") -> Color(0xFF64B5F6) // soft blue
                            else -> Color(0xFFFFB74D) // soft amber default
                        }
                        Text(
                            text = log,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = color,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilledTonalIconChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun AccountEditDialog(
    title: String,
    qqInitial: String,
    descInitial: String,
    cookiesInitial: String,
    onDismiss: () -> Unit,
    onSave: (qq: String, desc: String, cookies: String) -> Unit
) {
    var qq by remember { mutableStateOf(qqInitial) }
    var desc by remember { mutableStateOf(descInitial) }
    var cookies by remember { mutableStateOf(cookiesInitial) }

    var qqError by remember { mutableStateOf(false) }
    var cookiesError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = qq,
                    onValueChange = {
                        qq = it
                        qqError = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_qq_input"),
                    label = { Text("QQ端账号 (纯数字)") },
                    isError = qqError,
                    singleLine = true
                )
                if (qqError) {
                    Text("QQ账号不能为空且限纯数字", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_desc_input"),
                    label = { Text("备注标签 (如: 我的大号)") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = cookies,
                    onValueChange = {
                        cookies = it
                        cookiesError = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("dialog_cookies_input"),
                    label = { Text("游戏Cookie String") },
                    placeholder = { Text("含 uin=o0xxxxx; skey=@xxxx ; RK=... 等在内的完整手机大乐斗网站 cookies...") },
                    isError = cookiesError
                )
                if (cookiesError) {
                    Text("请提供有效的腾讯大乐斗授权 Cookies", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("取消")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            var hasError = false
                            if (qq.trim().isEmpty() || !qq.all { it.isDigit() }) {
                                qqError = true
                                hasError = true
                            }
                            if (cookies.trim().isEmpty()) {
                                cookiesError = true
                                hasError = true
                            }
                            if (!hasError) {
                                onSave(qq, desc, cookies)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dialog_save_button")
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}
