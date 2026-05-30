package s4y.yopt.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import yopt.composeapp.generated.resources.Res
import yopt.composeapp.generated.resources.*

import s4y.yopt.domain.services.AppPreferencesService
import s4y.yopt.usecases.ExportImportUseCase
import s4y.yopt.usecases.ManageAuthUseCase
import s4y.yopt.usecases.ManageChatsUseCase
import s4y.yopt.usecases.ManageModelSelectionUseCase
import s4y.yopt.usecases.ManageModelsUseCase
import s4y.yopt.usecases.RefreshModelsUseCase
import s4y.yopt.usecases.ManageResponseDisplayUseCase
import s4y.yopt.usecases.ManageGlobalInstructionsUseCase
import s4y.yopt.usecases.ManageLastChatIdUseCase
import s4y.yopt.usecases.ManageLastPromptUseCase
import s4y.yopt.usecases.ManageSplitFractionUseCase
import s4y.yopt.usecases.ManageProvidersUseCase
import s4y.yopt.usecases.SendPromptUseCase
import s4y.yopt.usecases.currentTimeMillis

private const val RESPONSE_AUTO_EXPAND_WORD_LIMIT = 50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    sendUseCase: SendPromptUseCase,
    modelsUseCase: ManageModelsUseCase,
    modelSelectionUseCase: ManageModelSelectionUseCase,
    chatsUseCase: ManageChatsUseCase,
    exportUseCase: ExportImportUseCase,
    manageAuthUseCase: ManageAuthUseCase,
    refreshModelsUseCase: RefreshModelsUseCase,
    manageProvidersUseCase: ManageProvidersUseCase,
    responseDisplayUseCase: ManageResponseDisplayUseCase,
    lastChatIdUseCase: ManageLastChatIdUseCase,
    lastPromptUseCase: ManageLastPromptUseCase,
    splitFractionUseCase: ManageSplitFractionUseCase,
    globalInstructionsUseCase: ManageGlobalInstructionsUseCase
) {
    val scope = rememberCoroutineScope()
    val persistedFraction by splitFractionUseCase.observe().collectAsState(AppPreferencesService.DEFAULT_SPLIT_FRACTION)
    var splitFraction by remember(persistedFraction) { mutableStateOf(persistedFraction) }
    var columnHeightPx by remember { mutableStateOf(1f) }
    val lastChatId by lastChatIdUseCase.observe().collectAsState(null)
    val lastPrompt by lastPromptUseCase.observe().collectAsState("")
    val globalInstructions by globalInstructionsUseCase.observe().collectAsState("")
    val allChats by chatsUseCase.observeAll().collectAsState(emptyList())
    var currentChatId by remember { mutableStateOf<String?>(null) }
    var chatName by remember { mutableStateOf("") }
    var prompt by remember(lastPrompt) { mutableStateOf(lastPrompt) }
    var loading by remember { mutableStateOf(false) }
    var sendJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showChatSettings by remember { mutableStateOf(false) }
    var chatSearchQuery by remember { mutableStateOf("") }
    var chatDropdownExpanded by remember { mutableStateOf(false) }
    val selectedModel by modelSelectionUseCase.observe().collectAsState(null)
    val models by modelsUseCase.observeEnabledModels().collectAsState(emptyList())
    val providers by manageProvidersUseCase.observeProviders().collectAsState(emptyList())
    val defaultShowMarkdown by responseDisplayUseCase.observeDefaultShowMarkdown().collectAsState(false)

    val currentChat = allChats.find { it.id == currentChatId }

    // Init: restore last chat or pick first, handle deletion of current chat
    LaunchedEffect(allChats, lastChatId) {
        if (currentChatId == null && allChats.isNotEmpty()) {
            val restored = allChats.find { it.id == lastChatId }
            currentChatId = restored?.id ?: allChats.first().id
            chatName = restored?.title ?: allChats.first().title
        } else if (currentChatId != null && allChats.none { it.id == currentChatId }) {
            currentChatId = allChats.firstOrNull()?.id
            allChats.firstOrNull()?.let { chatName = it.title }
        }
    }

    // Sync chatName and persist last used chat when switching
    LaunchedEffect(currentChatId) {
        currentChat?.let { chatName = it.title }
        currentChatId?.let { lastChatIdUseCase.set(it) }
    }

    // Auto-select first model if none selected or saved model no longer exists
    LaunchedEffect(models, selectedModel) {
        if ((selectedModel == null || models.none { it.id == selectedModel }) && models.isNotEmpty()) {
            modelSelectionUseCase.set(models.first().id)
        }
    }

    val filteredChats = allChats
        .filter {
            chatSearchQuery.isBlank() ||
                    it.title.contains(chatSearchQuery, ignoreCase = true) ||
                    it.labels.any { label -> label.contains(chatSearchQuery, ignoreCase = true) }
        }
        .sortedByDescending { c ->
            c.history.lastOrNull()?.timestamp ?: c.id.removePrefix("chat_").toLongOrNull() ?: 0L
        }

    if (showSettings) {
        SettingsScreen(
            manageModelsUseCase = modelsUseCase,
            manageChatsUseCase = chatsUseCase,
            exportImportUseCase = exportUseCase,
            manageAuthUseCase = manageAuthUseCase,
            refreshModelsUseCase = refreshModelsUseCase,
            manageProvidersUseCase = manageProvidersUseCase,
            globalInstructions = globalInstructions,
            onSetGlobalInstructions = { scope.launch { globalInstructionsUseCase.set(it) } },
            onDismiss = { showSettings = false }
        )
    } else {

        if (showChatSettings && currentChat != null) {
            var instr by remember(currentChat) { mutableStateOf(currentChat.instructions) }
            var editingLabels by remember(currentChat) { mutableStateOf(currentChat.labels) }
            var showAddTagDialog by remember { mutableStateOf(false) }
            var newTagText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showChatSettings = false },
                title = { Text(stringResource(Res.string.chat_settings)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = instr,
                            onValueChange = { instr = it },
                            label = { Text(stringResource(Res.string.instructions)) },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            maxLines = 5
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (tag in editingLabels) {
                                OutlinedButton(
                                    onClick = { editingLabels = editingLabels - tag },
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(tag, style = MaterialTheme.typography.labelSmall)
                                    Spacer(Modifier.width(2.dp))
                                    Text("×", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            OutlinedButton(
                                onClick = { showAddTagDialog = true },
                                modifier = Modifier.height(28.dp)
                            ) { Text(stringResource(Res.string.add_tag), style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        scope.launch {
                            chatsUseCase.update(
                                currentChat.copy(
                                    instructions = instr,
                                    labels = editingLabels
                                )
                            )
                        }
                        showChatSettings = false
                    }) { Text(stringResource(Res.string.save)) }
                },
                dismissButton = {
                    TextButton(onClick = { showChatSettings = false }) { Text(stringResource(Res.string.cancel)) }
                }
            )
            if (showAddTagDialog) {
                AlertDialog(
                    onDismissRequest = { showAddTagDialog = false; newTagText = "" },
                    title = { Text(stringResource(Res.string.add_tag_title)) },
                    text = {
                        OutlinedTextField(
                            value = newTagText,
                            onValueChange = { newTagText = it },
                            label = { Text(stringResource(Res.string.tag)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            val trimmed = newTagText.trim()
                            if (trimmed.isNotEmpty() && trimmed !in editingLabels) {
                                editingLabels = editingLabels + trimmed
                            }
                            newTagText = ""
                            showAddTagDialog = false
                        }) { Text(stringResource(Res.string.add)) }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showAddTagDialog = false; newTagText = ""
                        }) { Text(stringResource(Res.string.cancel)) }
                    }
                )
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(12.dp)
                .onGloballyPositioned { columnHeightPx = it.size.height.toFloat() }
        ) {
            Column(
                Modifier
                    .weight(splitFraction)
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
            ) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val headerWidth = maxWidth
                    val headerNarrow = headerWidth < 630.dp
                    if (headerNarrow) {
                        // Narrow: search + buttons on line 1, chat name on line 2
                        Column {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = chatSearchQuery,
                                        onValueChange = {
                                            chatSearchQuery = it
                                            chatDropdownExpanded = true
                                        },
                                        singleLine = true,
                                        placeholder = { Text(stringResource(Res.string.search)) },
                                        trailingIcon = {
                                            TooltipBox(
                                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                                tooltip = { PlainTooltip { Text(stringResource(Res.string.chat_list_tooltip)) } },
                                                state = rememberTooltipState()
                                            ) {
                                                TextButton(onClick = {
                                                    chatDropdownExpanded = !chatDropdownExpanded
                                                }) {
                                                    Icon(
                                                        AppIcons.ChatListToggle,
                                                        contentDescription = stringResource(Res.string.chat_list_tooltip),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    )
                                    DropdownMenu(
                                        expanded = chatDropdownExpanded,
                                        onDismissRequest = { chatDropdownExpanded = false }
                                    ) {
                                        filteredChats.forEach { chat ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        chat.title,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                onClick = {
                                                    currentChatId = chat.id
                                                    chatSearchQuery = ""
                                                    chatDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                        tooltip = { PlainTooltip { Text(stringResource(Res.string.new_chat_tooltip)) } },
                                        state = rememberTooltipState()
                                    ) {
                                        val newChatTitle = stringResource(Res.string.new_chat)
                                        TextButton(onClick = {
                                            scope.launch {
                                                val c = chatsUseCase.create(newChatTitle)
                                                currentChatId = c.id
                                            }
                                        }) {
                                            Icon(
                                                AppIcons.NewChat,
                                                contentDescription = stringResource(Res.string.new_chat_tooltip),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    if (allChats.size > 1) {
                                        TooltipBox(
                                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                            tooltip = { PlainTooltip { Text(stringResource(Res.string.delete_chat_tooltip)) } },
                                            state = rememberTooltipState()
                                        ) {
                                            TextButton(onClick = {
                                                currentChat?.let { chat ->
                                                    val remaining = allChats.filter { it.id != chat.id }
                                                    currentChatId = remaining.firstOrNull()?.id
                                                    scope.launch {
                                                        chatsUseCase.delete(chat.id)
                                                    }
                                                }
                                            }) {
                                                Icon(
                                                    AppIcons.DeleteChat,
                                                    contentDescription = stringResource(Res.string.delete_chat_tooltip),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                        tooltip = { PlainTooltip { Text(stringResource(Res.string.chat_settings_tooltip)) } },
                                        state = rememberTooltipState()
                                    ) {
                                        TextButton(onClick = { showChatSettings = true }) {
                                            Icon(
                                                AppIcons.ChatInstructions,
                                                contentDescription = stringResource(Res.string.chat_settings_tooltip),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                        tooltip = { PlainTooltip { Text(stringResource(Res.string.settings_tooltip)) } },
                                        state = rememberTooltipState()
                                    ) {
                                        TextButton(onClick = { showSettings = true }) {
                                            Icon(
                                                AppIcons.Settings,
                                                contentDescription = stringResource(Res.string.settings_tooltip),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = chatName,
                                onValueChange = { newTitle ->
                                    chatName = newTitle
                                    currentChat?.let { chat ->
                                        if (newTitle.isNotBlank()) {
                                            scope.launch { chatsUseCase.update(chat.copy(title = newTitle)) }
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(stringResource(Res.string.chat_name)) }
                            )
                        }
                    } else {
                        // Wide: search, chat name filling middle, buttons on right
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.width(headerWidth * 0.3f)) {
                                OutlinedTextField(
                                    value = chatSearchQuery,
                                    onValueChange = {
                                        chatSearchQuery = it
                                        chatDropdownExpanded = true
                                    },
                                    singleLine = true,
                                    placeholder = { Text(stringResource(Res.string.search)) },
                                    trailingIcon = {
                                        TooltipBox(
                                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                            tooltip = { PlainTooltip { Text(stringResource(Res.string.chat_list_tooltip)) } },
                                            state = rememberTooltipState()
                                        ) {
                                            TextButton(onClick = {
                                                chatDropdownExpanded = !chatDropdownExpanded
                                            }) {
                                                Icon(
                                                    AppIcons.ChatListToggle,
                                                    contentDescription = stringResource(Res.string.chat_list_tooltip),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                )
                                DropdownMenu(
                                    expanded = chatDropdownExpanded,
                                    onDismissRequest = { chatDropdownExpanded = false }
                                ) {
                                    filteredChats.forEach { chat ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    chat.title,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            onClick = {
                                                currentChatId = chat.id
                                                chatSearchQuery = ""
                                                chatDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = chatName,
                                onValueChange = { newTitle ->
                                    chatName = newTitle
                                    currentChat?.let { chat ->
                                        if (newTitle.isNotBlank()) {
                                            scope.launch { chatsUseCase.update(chat.copy(title = newTitle)) }
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                placeholder = { Text(stringResource(Res.string.chat_name)) }
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                    tooltip = { PlainTooltip { Text(stringResource(Res.string.new_chat_tooltip)) } },
                                    state = rememberTooltipState()
                                ) {
                                    val newChatTitle = stringResource(Res.string.new_chat)
                                    TextButton(onClick = {
                                        scope.launch {
                                            val c = chatsUseCase.create(newChatTitle)
                                            currentChatId = c.id
                                        }
                                    }) {
                                        Icon(
                                            AppIcons.NewChat,
                                            contentDescription = stringResource(Res.string.new_chat_tooltip),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                if (allChats.size > 1) {
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                        tooltip = { PlainTooltip { Text(stringResource(Res.string.delete_chat_tooltip)) } },
                                        state = rememberTooltipState()
                                    ) {
                                        TextButton(onClick = {
                                            currentChat?.let { chat ->
                                                val remaining = allChats.filter { it.id != chat.id }
                                                currentChatId = remaining.firstOrNull()?.id
                                                scope.launch { chatsUseCase.delete(chat.id) }
                                            }
                                        }) {
                                            Icon(
                                                AppIcons.DeleteChat,
                                                contentDescription = stringResource(Res.string.delete_chat_tooltip),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                    tooltip = { PlainTooltip { Text(stringResource(Res.string.chat_settings_tooltip)) } },
                                    state = rememberTooltipState()
                                ) {
                                    TextButton(onClick = { showChatSettings = true }) {
                                        Icon(
                                            AppIcons.ChatInstructions,
                                            contentDescription = stringResource(Res.string.chat_settings_tooltip),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                    tooltip = { PlainTooltip { Text(stringResource(Res.string.settings_tooltip)) } },
                                    state = rememberTooltipState()
                                ) {
                                    TextButton(onClick = {
                                        showSettings = true
                                    }) {
                                        Icon(
                                            AppIcons.Settings,
                                            contentDescription = stringResource(Res.string.settings_tooltip),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    placeholder = { Text(stringResource(Res.string.enter_prompt)) },
                    maxLines = Int.MAX_VALUE
                )

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    var modelExpanded by remember { mutableStateOf(false) }
                    val sel = models.find { it.id == selectedModel }
                    val providerName = providers.find { it.id == sel?.providerId }?.name
                    val selectedLabel = if (sel != null && providerName != null)
                        "$providerName: ${sel.officialName}"
                    else
                        stringResource(Res.string.select_model)
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = {
                            if (models.isEmpty()) showSettings = true
                            else modelExpanded = true
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (models.isNotEmpty()) DropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false }
                        ) {
                            models.forEach { m ->
                                val pName = providers.find { it.id == m.providerId }?.name
                                val label = if (pName != null) "$pName: ${m.officialName}" else m.officialName
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        scope.launch { modelSelectionUseCase.set(m.id) }
                                        modelExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    if (loading) {
                        CircularProgressIndicator(
                            Modifier.size(24.dp).clickable { sendJob?.cancel() }
                        )
                    } else {
                        Button(onClick = {
                            val p = prompt.trim()
                            if (p.isEmpty()) return@Button
                            val chat = currentChat ?: return@Button
                            sendJob = scope.launch {
                                loading = true
                                error = null
                                try {
                                    if (chatName.isNotBlank() && chatName != chat.title) {
                                        chatsUseCase.update(chat.copy(title = chatName))
                                    }
                                    sendUseCase(chat.copy(title = chatName), p, selectedModel)
                                        .onSuccess { scope.launch { lastPromptUseCase.set(p) } }
                                        .onFailure { e -> if (e !is kotlinx.coroutines.CancellationException) error = e.message }
                                } finally {
                                    loading = false
                                    sendJob = null
                                }
                            }
                        }) { Text(stringResource(Res.string.send)) }
                    }
                }

                error?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } // top area Column

            // Draggable splitter between prompt area and responses
            var dragging by remember { mutableStateOf(false) }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(
                        if (dragging) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .pointerInput(columnHeightPx) {
                        detectVerticalDragGestures(
                            onDragStart = { dragging = true },
                            onDragEnd = {
                                dragging = false
                                scope.launch { splitFractionUseCase.set(splitFraction) }
                            },
                            onDragCancel = { dragging = false },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val delta = dragAmount / columnHeightPx
                                splitFraction = (splitFraction + delta).coerceIn(0.2f, 0.8f)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) {
                        Box(
                            Modifier
                                .size(4.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }

            @Suppress("DEPRECATION")
            val clipboard = LocalClipboardManager.current

            val dotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(1f - splitFraction)
                    .drawBehind {
                        val spacing = 20.dp.toPx()
                        val radius = 2.dp.toPx()
                        val cols = (size.width / spacing).toInt() + 2
                        val rows = (size.height / spacing).toInt() + 2
                        for (col in 0..cols) {
                            for (row in 0..rows) {
                                drawCircle(
                                    color = dotColor,
                                    radius = radius,
                                    center = androidx.compose.ui.geometry.Offset(
                                        x = col * spacing,
                                        y = row * spacing
                                    )
                                )
                            }
                        }
                    }
            ) {
                val history = currentChat?.history?.reversed() ?: emptyList()
                itemsIndexed(history) { i, entry ->
                    val isFirst = i == 0
                    val wordCount = entry.response.split(Regex("\\s+")).count { it.isNotBlank() }
                    var respExpanded by remember { mutableStateOf(isFirst || wordCount < RESPONSE_AUTO_EXPAND_WORD_LIMIT) }
                    var promptExpanded by remember { mutableStateOf(false) }
                    var promptOverflows by remember { mutableStateOf(false) }
                    var respContentHeightPx by remember { mutableStateOf(0) }
                    val windowHeightPx = LocalWindowInfo.current.containerSize.height
                    OutlinedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(4.dp)) {
                            // Prompt row: hidden for first item when both prompt and model match current selection
                            if (!isFirst || entry.prompt != prompt || entry.modelId != selectedModel) {
                                BoxWithConstraints(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 4.dp)
                                ) {
                                    val promptNarrow = maxWidth < 630.dp
                                    if (promptNarrow || promptExpanded) {
                                        // Narrow or expanded: buttons above prompt
                                        Column(Modifier.fillMaxWidth()) {
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (promptExpanded || promptOverflows) {
                                                    TooltipBox(
                                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                                        tooltip = { PlainTooltip { Text(stringResource(if (promptExpanded) Res.string.collapse_prompt else Res.string.expand_prompt)) } },
                                                        state = rememberTooltipState()
                                                    ) {
                                                        TextButton(onClick = {
                                                            promptExpanded = !promptExpanded
                                                        }) {
                                                            Icon(
                                                                if (promptExpanded) AppIcons.Collapse else AppIcons.Expand,
                                                                contentDescription = stringResource(if (promptExpanded) Res.string.collapse else Res.string.expand),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                TooltipBox(
                                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                                    tooltip = { PlainTooltip { Text(stringResource(Res.string.use_as_prompt)) } },
                                                    state = rememberTooltipState()
                                                ) {
                                                    TextButton(onClick = {
                                                        prompt = entry.prompt
                                                    }) {
                                                        Icon(
                                                            AppIcons.UseAsPrompt,
                                                            contentDescription = stringResource(Res.string.use_as_prompt),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                                TooltipBox(
                                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                                    tooltip = { PlainTooltip { Text(stringResource(Res.string.append_to_prompt)) } },
                                                    state = rememberTooltipState()
                                                ) {
                                                    TextButton(onClick = {
                                                        prompt = "$prompt\n${entry.prompt}"
                                                    }) {
                                                        Icon(
                                                            AppIcons.AppendToPrompt,
                                                            contentDescription = stringResource(Res.string.append_to_prompt),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                                TooltipBox(
                                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                                    tooltip = { PlainTooltip { Text(stringResource(Res.string.copy_prompt)) } },
                                                    state = rememberTooltipState()
                                                ) {
                                                    TextButton(onClick = {
                                                        clipboard.setText(AnnotatedString(entry.prompt))
                                                    }) {
                                                        Icon(
                                                            AppIcons.CopyToClipboard,
                                                            contentDescription = stringResource(Res.string.copy_prompt),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                entry.prompt,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = if (promptExpanded) Int.MAX_VALUE else 1,
                                                overflow = TextOverflow.Ellipsis,
                                                onTextLayout = {
                                                    promptOverflows = it.hasVisualOverflow
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    } else {
                                        // Wide + collapsed: buttons to the right of prompt
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                entry.prompt,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                onTextLayout = {
                                                    promptOverflows = it.hasVisualOverflow
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (promptOverflows) {
                                                TooltipBox(
                                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                                    tooltip = { PlainTooltip { Text(stringResource(Res.string.expand_prompt)) } },
                                                    state = rememberTooltipState()
                                                ) {
                                                    TextButton(onClick = {
                                                        promptExpanded = true
                                                    }) {
                                                        Icon(
                                                            AppIcons.Expand,
                                                            contentDescription = stringResource(Res.string.expand),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            TooltipBox(
                                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                                tooltip = { PlainTooltip { Text(stringResource(Res.string.use_as_prompt)) } },
                                                state = rememberTooltipState()
                                            ) {
                                                TextButton(onClick = { prompt = entry.prompt }) {
                                                    Icon(
                                                        AppIcons.UseAsPrompt,
                                                        contentDescription = stringResource(Res.string.use_as_prompt),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            TooltipBox(
                                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                                tooltip = { PlainTooltip { Text(stringResource(Res.string.append_to_prompt)) } },
                                                state = rememberTooltipState()
                                            ) {
                                                TextButton(onClick = {
                                                    prompt = "$prompt\n${entry.prompt}"
                                                }) {
                                                    Icon(
                                                        AppIcons.AppendToPrompt,
                                                        contentDescription = stringResource(Res.string.append_to_prompt),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            TooltipBox(
                                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                                tooltip = { PlainTooltip { Text(stringResource(Res.string.copy_prompt)) } },
                                                state = rememberTooltipState()
                                            ) {
                                                TextButton(onClick = {
                                                    clipboard.setText(AnnotatedString(entry.prompt))
                                                }) {
                                                    Icon(
                                                        AppIcons.CopyToClipboard,
                                                        contentDescription = stringResource(Res.string.copy_prompt),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                            } // end prompt row
                            // Response action buttons — always shown above response
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Collapse/Expand — left-aligned
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                    tooltip = { PlainTooltip { Text(stringResource(if (respExpanded) Res.string.collapse_response else Res.string.expand_response)) } },
                                    state = rememberTooltipState()
                                ) {
                                    TextButton(onClick = { respExpanded = !respExpanded }) {
                                        Icon(
                                            if (respExpanded) AppIcons.Collapse else AppIcons.Expand,
                                            contentDescription = stringResource(if (respExpanded) Res.string.collapse else Res.string.expand),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                if (respExpanded) {
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                        tooltip = { PlainTooltip { Text(stringResource(if (!entry.showMarkdown) Res.string.switch_to_markdown else Res.string.switch_to_raw)) } },
                                        state = rememberTooltipState()
                                    ) {
                                        TextButton(onClick = {
                                        scope.launch {
                                            currentChat?.let { chat ->
                                                chatsUseCase.toggleEntryMarkdown(chat.id, entry.timestamp)
                                                responseDisplayUseCase.setDefaultShowMarkdown(!entry.showMarkdown)
                                            }
                                        }
                                    }) {
                                            Icon(
                                                if (!entry.showMarkdown) AppIcons.MarkdownView else AppIcons.RawView,
                                                contentDescription = "Toggle raw/markdown",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                    tooltip = { PlainTooltip { Text(stringResource(Res.string.use_as_prompt)) } },
                                    state = rememberTooltipState()
                                ) {
                                    TextButton(onClick = { prompt = entry.response }) {
                                        Icon(
                                            AppIcons.UseAsPrompt,
                                            contentDescription = stringResource(Res.string.use_as_prompt),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                    tooltip = { PlainTooltip { Text(stringResource(Res.string.append_to_prompt)) } },
                                    state = rememberTooltipState()
                                ) {
                                    TextButton(onClick = {
                                        prompt = "$prompt\n${entry.response}"
                                    }) {
                                        Icon(
                                            AppIcons.AppendToPrompt,
                                            contentDescription = stringResource(Res.string.append_to_prompt),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                    tooltip = { PlainTooltip { Text(stringResource(Res.string.copy_response)) } },
                                    state = rememberTooltipState()
                                ) {
                                    TextButton(onClick = { clipboard.setText(AnnotatedString(entry.response)) }) {
                                        Icon(
                                            AppIcons.CopyToClipboard,
                                            contentDescription = stringResource(Res.string.copy_response),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            // Response content
                            if (respExpanded) {
                                // Raw view keeps its selection in a TextFieldValue so Cmd+C can
                                // copy the real selection (see interceptor below).
                                var rawFieldValue by remember(entry.response) {
                                    mutableStateOf(TextFieldValue(entry.response))
                                }
                                var showMarkdownCopyWarning by remember { mutableStateOf(false) }
                                // Response text — selectable on capable targets.
                                val responseBody = @Composable {
                                    if (supportsTextSelection) {
                                        if (entry.showMarkdown) {
                                            SelectionContainer {
                                                MarkdownResponse(
                                                    content = entry.response,
                                                    modifier = Modifier.fillMaxWidth().onGloballyPositioned {
                                                        respContentHeightPx = it.size.height
                                                    }
                                                )
                                            }
                                        } else {
                                            BasicTextField(
                                                value = rawFieldValue,
                                                // readOnly blocks edits; selection changes still
                                                // arrive here, keeping rawFieldValue.selection live.
                                                onValueChange = { rawFieldValue = it.copy(text = entry.response) },
                                                readOnly = true,
                                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                modifier = Modifier.fillMaxWidth().onGloballyPositioned {
                                                    respContentHeightPx = it.size.height
                                                }
                                            )
                                        }
                                    } else {
                                        if (entry.showMarkdown) {
                                            MarkdownResponse(
                                                content = entry.response,
                                                modifier = Modifier.fillMaxWidth().onGloballyPositioned {
                                                    respContentHeightPx = it.size.height
                                                }
                                            )
                                        } else {
                                            Text(
                                                entry.response,
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                                modifier = Modifier.fillMaxWidth().onGloballyPositioned {
                                                    respContentHeightPx = it.size.height
                                                }
                                            )
                                        }
                                    }
                                }
                                if (needsCopyKeyInterceptor) {
                                    // Compose 1.11.0 macOS native: key-event classification
                                    // (isCopyKeyEvent) is unimplemented, so native Cmd+C crashes on
                                    // any selection-capable widget. Intercept Key.C ourselves.
                                    //  - Raw view: copy the real selection from the TextFieldValue.
                                    //  - Markdown view: SelectionContainer exposes no public way to
                                    //    read its selection, so warn the user (right-click Copy,
                                    //    which the framework handles, still works in both views).
                                    Box(
                                        modifier = Modifier.onPreviewKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyDown && event.key == Key.C) {
                                                if (entry.showMarkdown) {
                                                    showMarkdownCopyWarning = true
                                                } else {
                                                    val sel = rawFieldValue.selection
                                                    val copied = if (sel.collapsed) entry.response
                                                        else entry.response.substring(sel.min, sel.max)
                                                    clipboard.setText(AnnotatedString(copied))
                                                }
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                    ) {
                                        responseBody()
                                    }
                                    if (showMarkdownCopyWarning) {
                                        AlertDialog(
                                            onDismissRequest = { showMarkdownCopyWarning = false },
                                            confirmButton = {
                                                TextButton(onClick = { showMarkdownCopyWarning = false }) {
                                                    Text(stringResource(Res.string.ok))
                                                }
                                            },
                                            title = { Text(stringResource(Res.string.cmd_c_markdown_title)) },
                                            text = { Text(stringResource(Res.string.cmd_c_markdown_message)) },
                                        )
                                    }
                                } else {
                                    responseBody()
                                }
                            } else {
                                Text(
                                    entry.response.take(200),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            // Bottom bar: info only
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                    tooltip = { PlainTooltip { Text(stringResource(Res.string.remove_from_history)) } },
                                    state = rememberTooltipState()
                                ) {
                                    TextButton(onClick = {
                                        scope.launch {
                                            currentChat?.let { chat ->
                                                val origIdx = chat.history.size - 1 - i
                                                chatsUseCase.removeHistoryEntry(chat.id, origIdx)
                                            }
                                        }
                                    }) {
                                        Icon(
                                            AppIcons.RemoveFromHistory,
                                            contentDescription = stringResource(Res.string.remove_from_history),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    formatTimestamp(entry.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val entryModel = models.find { it.id == entry.modelId }
                                val entryProviderName = providers.find { it.id == entryModel?.providerId }?.name
                                val entryModelLabel = if (entryProviderName != null)
                                    "$entryProviderName: ${entry.modelName}"
                                else
                                    entry.modelName
                                Text(
                                    " · $entryModelLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Text(
                                    " · ${formatDuration(entry.durationMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Bottom repeat buttons when expanded content taller than screen
                            if (respExpanded && respContentHeightPx > windowHeightPx) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Collapse/Expand — left-aligned
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                        tooltip = { PlainTooltip { Text(stringResource(if (respExpanded) Res.string.collapse_response else Res.string.expand_response)) } },
                                        state = rememberTooltipState()
                                    ) {
                                        TextButton(onClick = { respExpanded = !respExpanded }) {
                                            Icon(
                                                if (respExpanded) AppIcons.Collapse else AppIcons.Expand,
                                                contentDescription = stringResource(if (respExpanded) Res.string.collapse else Res.string.expand),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                        tooltip = { PlainTooltip { Text(stringResource(if (!entry.showMarkdown) Res.string.switch_to_markdown else Res.string.switch_to_raw)) } },
                                        state = rememberTooltipState()
                                    ) {
                                        TextButton(onClick = {
                                        scope.launch {
                                            currentChat?.let { chat ->
                                                chatsUseCase.toggleEntryMarkdown(chat.id, entry.timestamp)
                                                responseDisplayUseCase.setDefaultShowMarkdown(!entry.showMarkdown)
                                            }
                                        }
                                    }) {
                                            Icon(
                                                if (!entry.showMarkdown) AppIcons.MarkdownView else AppIcons.RawView,
                                                contentDescription = "Toggle raw/markdown",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                        tooltip = { PlainTooltip { Text(stringResource(Res.string.use_as_prompt)) } },
                                        state = rememberTooltipState()
                                    ) {
                                        TextButton(onClick = { prompt = entry.response }) {
                                            Icon(
                                                AppIcons.UseAsPrompt,
                                                contentDescription = stringResource(Res.string.use_as_prompt),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                        tooltip = { PlainTooltip { Text(stringResource(Res.string.append_to_prompt)) } },
                                        state = rememberTooltipState()
                                    ) {
                                        TextButton(onClick = {
                                            prompt = "$prompt\n${entry.response}"
                                        }) {
                                            Icon(
                                                AppIcons.AppendToPrompt,
                                                contentDescription = stringResource(Res.string.append_to_prompt),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                        tooltip = { PlainTooltip { Text(stringResource(Res.string.copy_response)) } },
                                        state = rememberTooltipState()
                                    ) {
                                        TextButton(onClick = {
                                            clipboard.setText(AnnotatedString(entry.response))
                                        }) {
                                            Icon(
                                                AppIcons.CopyToClipboard,
                                                contentDescription = stringResource(Res.string.copy_response),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(epochMs: Long): String {
    val now = currentTimeMillis()
    val diff = now - epochMs
    return when {
        diff < 60_000 -> "now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}

private fun formatDuration(ms: Long): String = when {
    ms < 1000 -> "${ms}ms"
    ms < 10000 -> "${ms / 1000}.${(ms % 1000) / 100}s"
    else -> "${ms / 1000}s"
}
