package s4y.yopt.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import yopt.composeapp.generated.resources.Res
import yopt.composeapp.generated.resources.*
import s4y.yopt.domain.models.ApiStyle
import s4y.yopt.domain.models.AuthCredentials
import s4y.yopt.domain.models.AuthType
import s4y.yopt.domain.models.ProviderDef
import s4y.yopt.usecases.ExportImportUseCase
import s4y.yopt.usecases.ManageAuthUseCase
import s4y.yopt.usecases.ManageChatsUseCase
import s4y.yopt.usecases.ManageModelsUseCase
import s4y.yopt.usecases.ManageProvidersUseCase
import s4y.yopt.usecases.RefreshModelsUseCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    manageModelsUseCase: ManageModelsUseCase,
    manageChatsUseCase: ManageChatsUseCase,
    exportImportUseCase: ExportImportUseCase,
    manageAuthUseCase: ManageAuthUseCase,
    refreshModelsUseCase: RefreshModelsUseCase,
    manageProvidersUseCase: ManageProvidersUseCase,
    globalInstructions: String,
    onSetGlobalInstructions: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    val models by manageModelsUseCase.observeModels().collectAsState(emptyList())


    val chats by manageChatsUseCase.observeAll().collectAsState(emptyList())
    val creds by manageAuthUseCase.observeCredentials().collectAsState(emptyList())
    val providers by manageProvidersUseCase.observeProviders().collectAsState(emptyList())
    var newlyCreatedProviderId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(stringResource(Res.string.back)) } },
                state = rememberTooltipState()
            ) {
                TextButton(onClick = onDismiss) {
                    Icon(AppIcons.Back, contentDescription = stringResource(Res.string.back), modifier = Modifier.size(20.dp))
                }
            }
            Text(
                stringResource(Res.string.global_settings_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        PrimaryTabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = { PlainTooltip { Text(stringResource(Res.string.tab_providers)) } },
                    state = rememberTooltipState()
                ) { Text(stringResource(Res.string.tab_providers), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = { PlainTooltip { Text(stringResource(Res.string.tab_chats)) } },
                    state = rememberTooltipState()
                ) { Text(stringResource(Res.string.tab_chats), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = { PlainTooltip { Text(stringResource(Res.string.tab_global)) } },
                    state = rememberTooltipState()
                ) { Text(stringResource(Res.string.tab_global), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            })
            Tab(selected = tab == 3, onClick = { tab = 3 }, text = {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = { PlainTooltip { Text(stringResource(Res.string.tab_export)) } },
                    state = rememberTooltipState()
                ) { Text(stringResource(Res.string.tab_export), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            })
        }
        Spacer(Modifier.height(8.dp))
        when (tab) {
            0 -> LazyColumn(Modifier.weight(1f)) {
                items(providers, key = { it.id }) { provider ->
                    val cred = creds.find { it.providerId == provider.id }
                    val providerModels = models.filter { it.providerId == provider.id }
                    var expanded by remember { mutableStateOf(provider.id == newlyCreatedProviderId) }
                    var refreshing by remember { mutableStateOf(false) }
                    var refreshError by remember { mutableStateOf<String?>(null) }

                    Card(Modifier.fillMaxWidth().padding(4.dp)) {
                        Column(Modifier.padding(8.dp)) {
                            val hasKey = !cred?.apiKey.isNullOrBlank()
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(provider.name, style = MaterialTheme.typography.titleSmall)
                                    val status = when {
                                        hasKey -> stringResource(Res.string.api_key_set)
                                        else -> stringResource(Res.string.not_configured)
                                    }
                                    Text(
                                        status,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (hasKey) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.error,
                                        modifier = if (!hasKey) Modifier.clickable { expanded = true }
                                            else Modifier
                                    )
                                }
                                if (expanded) {
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                        tooltip = { PlainTooltip { Text(stringResource(Res.string.refresh_models)) } },
                                        state = rememberTooltipState()
                                    ) {
                                        TextButton(
                                            onClick = {
                                                refreshing = true
                                                refreshError = null
                                                scope.launch {
                                                    try {
                                                        refreshModelsUseCase.refresh(provider, cred?.apiKey)
                                                            .onFailure { if (it !is kotlinx.coroutines.CancellationException) refreshError = it.message }
                                                    } finally { refreshing = false }
                                                }
                                            },
                                            enabled = !refreshing && hasKey
                                        ) {
                                            if (refreshing) {
                                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                            } else {
                                                Icon(AppIcons.RefreshModels, contentDescription = stringResource(Res.string.refresh_models), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                    refreshError?.let {
                                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                    tooltip = { PlainTooltip { Text(stringResource(if (expanded) Res.string.collapse else Res.string.expand)) } },
                                    state = rememberTooltipState()
                                ) {
                                    TextButton(onClick = { expanded = !expanded }) {
                                        Icon(
                                            if (expanded) AppIcons.Collapse else AppIcons.Expand,
                                            contentDescription = stringResource(if (expanded) Res.string.collapse else Res.string.expand),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            if (expanded) {
                                Spacer(Modifier.height(8.dp))
                                if (!provider.predefined) {
                                    var editName by remember { mutableStateOf(provider.name) }
                                    var editBaseUrl by remember { mutableStateOf(provider.baseUrl) }
                                    var editApiStyle by remember { mutableStateOf(provider.apiStyle) }
                                    var editApiKey by remember(cred) { mutableStateOf(cred?.apiKey ?: "") }
                                    var styleExpanded by remember { mutableStateOf(false) }
                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        label = { Text(stringResource(Res.string.provider_name)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = editBaseUrl,
                                        onValueChange = { editBaseUrl = it },
                                        label = { Text(stringResource(Res.string.base_url)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${stringResource(Res.string.api_style)}: ${editApiStyle.name}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Box {
                                            TextButton(onClick = { styleExpanded = true }) {
                                                Text(stringResource(Res.string.change))
                                            }
                                            DropdownMenu(
                                                expanded = styleExpanded,
                                                onDismissRequest = { styleExpanded = false }
                                            ) {
                                                ApiStyle.entries.forEach { style ->
                                                    DropdownMenuItem(
                                                        text = { Text(style.name) },
                                                        onClick = {
                                                            editApiStyle = style
                                                            styleExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    val keyLabel = when (val t = provider.authType) {
                                        is AuthType.ApiKey -> t.keyName
                                    }
                                    OutlinedTextField(
                                        value = editApiKey,
                                        onValueChange = { editApiKey = it },
                                        label = { Text(keyLabel) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = {
                                        scope.launch {
                                            manageProvidersUseCase.updateCustomProvider(
                                                provider.copy(
                                                    name = editName,
                                                    baseUrl = editBaseUrl,
                                                    apiStyle = editApiStyle,
                                                    authType = AuthType.ApiKey("$editName API Key")
                                                )
                                            )
                                            if (editApiKey.isNotBlank()) {
                                                manageAuthUseCase.saveApiKey(provider.id, editApiKey)
                                                if (providerModels.isEmpty()) {
                                                    refreshing = true
                                                    refreshError = null
                                                    try {
                                                        refreshModelsUseCase.refresh(provider, editApiKey)
                                                            .onFailure { if (it !is kotlinx.coroutines.CancellationException) refreshError = it.message }
                                                    } finally { refreshing = false }
                                                }
                                            } else {
                                                manageAuthUseCase.deleteCredentials(provider.id)
                                                manageModelsUseCase.clearModels(provider.id)
                                            }
                                        }
                                    }) {
                                        Text(stringResource(Res.string.save))
                                    }
                                } else {
                                    when (provider.authType) {
                                        is AuthType.ApiKey -> ApiKeyAuth(
                                            provider = provider,
                                            cred = cred,
                                            onSave = { key ->
                                                scope.launch {
                                                    manageAuthUseCase.saveApiKey(provider.id, key)
                                                    if (providerModels.isEmpty()) {
                                                        refreshing = true
                                                        refreshError = null
                                                        try {
                                                            refreshModelsUseCase.refresh(provider, key)
                                                                .onFailure { if (it !is kotlinx.coroutines.CancellationException) refreshError = it.message }
                                                        } finally { refreshing = false }
                                                    }
                                                }
                                            },
                                            onClear = {
                                                scope.launch {
                                                    manageAuthUseCase.deleteCredentials(provider.id)
                                                    manageModelsUseCase.clearModels(provider.id)
                                                }
                                            }
                                        )
                                    }
                                }
                                if (!provider.predefined) {
                                    Spacer(Modifier.height(8.dp))
                                    TextButton(onClick = {
                                        scope.launch {
                                            manageProvidersUseCase.deleteCustomProvider(provider.id)
                                        }
                                    }) {
                                        Icon(AppIcons.DeleteChat, contentDescription = stringResource(Res.string.delete_provider), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(stringResource(Res.string.delete_provider), color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                if (providerModels.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    var filter by remember { mutableStateOf("") }
                                    val filtered = providerModels
                                        .filter { filter.isBlank() || it.officialName.contains(filter, ignoreCase = true) }
                                        .sortedByDescending { it.enabled }
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = filter,
                                            onValueChange = { filter = it },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            placeholder = { Text(stringResource(Res.string.filter_models)) }
                                        )
                                        TextButton(onClick = {
                                            scope.launch {
                                                filtered.forEach { manageModelsUseCase.setModelEnabled(it.id, false) }
                                            }
                                        }) {
                                            Text(stringResource(Res.string.all_off), style = MaterialTheme.typography.labelSmall)
                                        }
                                        TextButton(onClick = {
                                            scope.launch {
                                                filtered.forEach { manageModelsUseCase.setModelEnabled(it.id, true) }
                                            }
                                        }) {
                                            Text(stringResource(Res.string.all_on), style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        stringResource(Res.string.models_section),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    filtered.forEach { model ->
                                        val enabled = model.enabled
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                        ) {
                                            Checkbox(
                                                checked = enabled,
                                                onCheckedChange = {
                                                    scope.launch { manageModelsUseCase.setModelEnabled(model.id, !enabled) }
                                                }
                                            )
                                            Text(
                                                model.officialName,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "add-custom") {
                    var expanded by remember { mutableStateOf(false) }
                    var newName by remember { mutableStateOf("") }
                    var newBaseUrl by remember { mutableStateOf("") }
                    var newApiStyle by remember { mutableStateOf(ApiStyle.OPENAI) }
                    var newApiKey by remember { mutableStateOf("") }
                    var styleExpanded by remember { mutableStateOf(false) }
                    var refreshing by remember { mutableStateOf(false) }
                    var refreshError by remember { mutableStateOf<String?>(null) }
                    val defaultProviderName = stringResource(Res.string.custom_provider)

                    Card(Modifier.fillMaxWidth().padding(4.dp)) {
                        Column(Modifier.padding(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(Res.string.custom_provider), style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        stringResource(Res.string.not_configured),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.clickable { expanded = true }
                                    )
                                }
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                    tooltip = { PlainTooltip { Text(stringResource(if (expanded) Res.string.collapse else Res.string.expand)) } },
                                    state = rememberTooltipState()
                                ) {
                                    TextButton(onClick = { expanded = !expanded }) {
                                        Icon(
                                            if (expanded) AppIcons.Collapse else AppIcons.Expand,
                                            contentDescription = stringResource(if (expanded) Res.string.collapse else Res.string.expand),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            if (expanded) {
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = newName,
                                    onValueChange = { newName = it },
                                    label = { Text(stringResource(Res.string.provider_name)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = newBaseUrl,
                                    onValueChange = { newBaseUrl = it },
                                    label = { Text(stringResource(Res.string.base_url)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${stringResource(Res.string.api_style)}: ${newApiStyle.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box {
                                        TextButton(onClick = { styleExpanded = true }) {
                                            Text(stringResource(Res.string.change))
                                        }
                                        DropdownMenu(
                                            expanded = styleExpanded,
                                            onDismissRequest = { styleExpanded = false }
                                        ) {
                                            ApiStyle.entries.forEach { style ->
                                                DropdownMenuItem(
                                                    text = { Text(style.name) },
                                                    onClick = {
                                                        newApiStyle = style
                                                        styleExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = newApiKey,
                                    onValueChange = { newApiKey = it },
                                    label = { Text(stringResource(Res.string.new_api_key)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(onClick = {
                                        val name = newName.ifBlank { defaultProviderName }
                                        val url = newBaseUrl.ifBlank { "https://api.example.com" }
                                        scope.launch {
                                            val def = manageProvidersUseCase.addCustomProvider(
                                                name = name,
                                                apiStyle = newApiStyle,
                                                baseUrl = url
                                            )
                                            newlyCreatedProviderId = def.id
                                            if (newApiKey.isNotBlank()) {
                                                manageAuthUseCase.saveApiKey(def.id, newApiKey)
                                                refreshing = true
                                                refreshError = null
                                                try {
                                                    refreshModelsUseCase.refresh(def, newApiKey)
                                                        .onFailure { if (it !is kotlinx.coroutines.CancellationException) refreshError = it.message }
                                                } finally { refreshing = false }
                                            }
                                        }
                                        newName = ""
                                        newBaseUrl = ""
                                        newApiKey = ""
                                        expanded = false
                                    }, enabled = newName.isNotBlank() && newBaseUrl.isNotBlank()) {
                                        if (refreshing) {
                                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text(stringResource(Res.string.save))
                                        }
                                    }
                                }
                                refreshError?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
            1 -> Column(Modifier.weight(1f)) {
                val allLabels = remember(chats) { chats.flatMap { it.labels }.distinct().sorted() }
                var checkedLabels by remember { mutableStateOf(setOf<String>()) }
                if (allLabels.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (label in allLabels) {
                            val checked = label in checkedLabels
                            if (checked) {
                                Button(
                                    onClick = { checkedLabels = checkedLabels - label },
                                    modifier = Modifier.height(28.dp)
                                ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                            } else {
                                OutlinedButton(
                                    onClick = { checkedLabels = checkedLabels + label },
                                    modifier = Modifier.height(28.dp)
                                ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                val filteredChats = if (checkedLabels.isEmpty()) chats
                    else chats.filter { chat -> checkedLabels.all { it in chat.labels } }
                LazyColumn(Modifier.weight(1f)) {
                    items(filteredChats) { chat ->
                        var editing by remember { mutableStateOf(false) }
                        var t by remember(chat.title) { mutableStateOf(chat.title) }
                        var i by remember(chat.instructions) { mutableStateOf(chat.instructions) }
                        var editingLabels by remember(chat.labels) { mutableStateOf(chat.labels) }
                        var showAddTagDialog by remember { mutableStateOf(false) }
                        var newTagText by remember { mutableStateOf("") }
                        Card(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Column(Modifier.padding(8.dp)) {
                                if (editing) {
                                    OutlinedTextField(t, { t = it }, label = { Text(stringResource(Res.string.title_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(i, { i = it }, label = { Text(stringResource(Res.string.instructions)) }, modifier = Modifier.fillMaxWidth().height(60.dp), maxLines = 3)
                                    Spacer(Modifier.height(4.dp))
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
                                    Row {
                                        TextButton(onClick = {
                                            scope.launch {
                                                manageChatsUseCase.update(chat.copy(
                                                    title = t, instructions = i,
                                                    labels = editingLabels
                                                ))
                                            }
                                            editing = false
                                        }) { Text(stringResource(Res.string.save)) }
                                        TextButton(onClick = { editing = false }) { Text(stringResource(Res.string.cancel)) }
                                    }
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
                                                TextButton(onClick = { showAddTagDialog = false; newTagText = "" }) { Text(stringResource(Res.string.cancel)) }
                                            }
                                        )
                                    }
                                } else {
                                    Text(chat.title)
                                    if (chat.labels.isNotEmpty()) {
                                        Row(
                                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            chat.labels.forEach { label ->
                                                Text(
                                                    label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(chat.instructions.take(100), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row {
                                        TextButton(onClick = { editing = true }) { Text(stringResource(Res.string.edit)) }
                                        TextButton(onClick = {
                                            scope.launch {
                                                manageChatsUseCase.delete(chat.id)
                                            }
                                        }) { Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> Column(Modifier.weight(1f)) {
                var instr by remember(globalInstructions) { mutableStateOf(globalInstructions) }
                OutlinedTextField(
                    value = instr,
                    onValueChange = {
                        instr = it
                        onSetGlobalInstructions(it)
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    placeholder = { Text(stringResource(Res.string.global_instructions_hint)) }
                )
            }
            3 -> {
                var exportError by remember { mutableStateOf<String?>(null) }
                var importReplaceError by remember { mutableStateOf<String?>(null) }
                var importAppendError by remember { mutableStateOf<String?>(null) }
                var dialogTitle by remember { mutableStateOf<String?>(null) }
                var dialogText by remember { mutableStateOf<String?>(null) }
                val exportTitle = stringResource(Res.string.tab_export)
                val exportSuccessMsg = stringResource(Res.string.export_success)
                val exportWarningMsg = stringResource(Res.string.export_warning)
                val importTitle = stringResource(Res.string.import_title)
                val importSuccessMsg = stringResource(Res.string.import_success)
                val importAppendSuccessMsg = stringResource(Res.string.import_append_success)
                /*
                Text(
                    exportWarningMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )*/
                Spacer(Modifier.height(8.dp))
                SaveFileButton {
                    exportError = null
                    try {
                        exportImportUseCase.export()
                        dialogTitle = exportTitle
                        dialogText = exportSuccessMsg + "\n\n" + exportWarningMsg
                        ""
                    } catch (e: Exception) {
                        exportError = e.message
                        ""
                    }
                }
                exportError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                OpenFileButton(stringResource(Res.string.load_replace)) { content ->
                    if (content != null) {
                        importReplaceError = null
                        try {
                            exportImportUseCase.import(content)
                            dialogTitle = importTitle
                            dialogText = importSuccessMsg
                        } catch (e: Exception) { importReplaceError = e.message }
                    }
                }
                importReplaceError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                OpenFileButton(stringResource(Res.string.load_append)) { content ->
                    if (content != null) {
                        importAppendError = null
                        try {
                            exportImportUseCase.importAppend(content)
                            dialogTitle = importTitle
                            dialogText = importAppendSuccessMsg
                        } catch (e: Exception) { importAppendError = e.message }
                    }
                }
                importAppendError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (dialogText != null) {
                    AlertDialog(
                        onDismissRequest = { dialogText = null },
                        title = { Text(dialogTitle ?: "") },
                        text = { Text(dialogText!!) },
                        confirmButton = {
                            TextButton(onClick = { dialogText = null }) { Text(stringResource(Res.string.ok)) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiKeyAuth(
    provider: ProviderDef,
    cred: AuthCredentials?,
    onSave: (String) -> Unit,
    onClear: () -> Unit
) {
    val hasKey = !cred?.apiKey.isNullOrBlank()
    var editing by remember { mutableStateOf(false) }
    var key by remember { mutableStateOf("") }

    if (hasKey && !editing) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { editing = true }) { Text(stringResource(Res.string.change)) }
            OutlinedButton(onClick = onClear) { Text(stringResource(Res.string.clear)) }
        }
    } else {
        val label = if (hasKey) stringResource(Res.string.new_api_key) else when (val t = provider.authType) {
            is AuthType.ApiKey -> t.keyName
        }
        val uriHandler = LocalUriHandler.current
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (!hasKey) {
            TextButton(onClick = {
                val url = when (provider.id) {
                    "openai" -> "https://platform.openai.com/api-keys"
                    "anthropic" -> "https://console.anthropic.com/settings/keys"
                    "google" -> "https://aistudio.google.com/apikey"
                    "openrouter" -> "https://openrouter.ai/keys"
                    "deepseek" -> "https://platform.deepseek.com/api_keys"
                    "qwen" -> "https://bailian.console.aliyun.com/?apiKey=1"
                    "huggingface" -> "https://huggingface.co/settings/tokens"
                    else -> return@TextButton
                }
                uriHandler.openUri(url)
            }) {
                Text(
                    stringResource(Res.string.get_api_key),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                onSave(key)
                key = ""
                editing = false
            }) { Text(stringResource(Res.string.save)) }
            if (hasKey) {
                TextButton(onClick = {
                    key = ""
                    editing = false
                }) { Text(stringResource(Res.string.cancel)) }
            }
        }
    }
}

