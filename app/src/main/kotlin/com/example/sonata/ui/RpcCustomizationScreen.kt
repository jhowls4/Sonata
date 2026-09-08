package com.example.sonata.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sonata.data.*
import com.example.sonata.util.StringTemplateParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RpcCustomizationScreen(
    viewModel: RpcCustomizationViewModel,
    onBack: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    val presets by viewModel.presets.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RPC Customization") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                RpcPreviewCard(config)
            }

            item {
                ConfigSection(title = "General & App ID", icon = Icons.Default.Info) {
                    OutlinedTextField(
                        value = config.applicationId,
                        onValueChange = { viewModel.updateConfig(config.copy(applicationId = it)) },
                        label = { Text("Discord Application ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = config.customActivityName ?: "",
                        onValueChange = { viewModel.updateConfig(config.copy(customActivityName = it.ifBlank { null })) },
                        label = { Text("Custom Activity Name") },
                        placeholder = { Text("Default: App Name (e.g. Spotify)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ActivityTypeDropdown(config.activityType) {
                        viewModel.updateConfig(config.copy(activityType = it))
                    }
                }
            }

            item {
                ConfigSection(title = "Text Templates", icon = Icons.Default.Edit) {
                    TemplateField("Details Line", config.detailsTemplate) { viewModel.updateConfig(config.copy(detailsTemplate = it)) }
                    TemplateField("State Line", config.stateTemplate) { viewModel.updateConfig(config.copy(stateTemplate = it)) }
                    TemplateField("Large Image Hover Text", config.largeImageHoverTemplate) { viewModel.updateConfig(config.copy(largeImageHoverTemplate = it)) }
                    TemplateField("Small Image Hover Text", config.smallImageHoverTemplate) { viewModel.updateConfig(config.copy(smallImageHoverTemplate = it)) }
                }
            }

            item {
                ConfigSection(title = "Timestamps & Artwork", icon = Icons.Default.DateRange) {
                    Text("Timestamp Mode", style = MaterialTheme.typography.labelLarge)
                    TimestampModeSelector(config.timestampMode) {
                        viewModel.updateConfig(config.copy(timestampMode = it))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Cover Art Source", style = MaterialTheme.typography.labelLarge)
                    CoverArtSourceSelector(config.coverArtSource) {
                        viewModel.updateConfig(config.copy(coverArtSource = it))
                    }
                    AnimatedVisibility(visible = config.coverArtSource == CoverArtSource.CUSTOM) {
                        OutlinedTextField(
                            value = config.customImageUrl ?: "",
                            onValueChange = { viewModel.updateConfig(config.copy(customImageUrl = it)) },
                            label = { Text("Custom Image URL") },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                    OutlinedTextField(
                        value = config.fallbackAssetKey,
                        onValueChange = { viewModel.updateConfig(config.copy(fallbackAssetKey = it)) },
                        label = { Text("Fallback Asset Key") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Checkbox(checked = config.showSmallBadge, onCheckedChange = { viewModel.updateConfig(config.copy(showSmallBadge = it)) })
                        Text("Show Media Player Badge")
                    }
                }
            }

            item {
                ConfigSection(title = "Interactive Buttons", icon = Icons.Default.List) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enable Buttons", modifier = Modifier.weight(1f))
                        Switch(checked = config.buttonsEnabled, onCheckedChange = { viewModel.updateConfig(config.copy(buttonsEnabled = it)) })
                    }
                    if (config.buttonsEnabled) {
                        config.buttons.forEachIndexed { index, button ->
                            ButtonEditRow(button, 
                                onUpdate = { updated ->
                                    val newList = config.buttons.toMutableList()
                                    newList[index] = updated
                                    viewModel.updateConfig(config.copy(buttons = newList))
                                },
                                onDelete = {
                                    val newList = config.buttons.toMutableList()
                                    newList.removeAt(index)
                                    viewModel.updateConfig(config.copy(buttons = newList))
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (config.buttons.size < 2) {
                            Button(onClick = {
                                viewModel.updateConfig(config.copy(buttons = config.buttons + RpcButtonConfig("Label", "https://")))
                            }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text("Add Button")
                            }
                        }
                    }
                }
            }

            item {
                PresetManagerSection(presets, onSave = { viewModel.savePreset(it) }, onDelete = { viewModel.deletePreset(it) }, onApply = { viewModel.applyPreset(it) })
            }
        }
    }
}

@Composable
fun RpcPreviewCard(config: RpcCustomizationConfig) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2D31)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("LIVE PREVIEW", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB5BAC1))
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(64.dp)) {
                    AsyncImage(
                        model = "https://via.placeholder.com/128",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    if (config.showSmallBadge) {
                        Box(modifier = Modifier.size(24.dp).align(Alignment.BottomEnd).clip(RoundedCornerShape(12.dp)).padding(2.dp)) {
                            AsyncImage(
                                model = "https://via.placeholder.com/48",
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Sonata", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = StringTemplateParser.parse(config.detailsTemplate, "Sample Title", "Sample Artist"),
                        color = Color.White, fontSize = 12.sp
                    )
                    Text(
                        text = StringTemplateParser.parse(config.stateTemplate, "Sample Title", "Sample Artist"),
                        color = Color(0xFFB5BAC1), fontSize = 12.sp
                    )
                    if (config.timestampMode != TimestampMode.OFF) {
                        Text(text = "01:23 elapsed", color = Color(0xFFB5BAC1), fontSize = 11.sp)
                    }
                }
            }
            if (config.buttonsEnabled && config.buttons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                config.buttons.forEach { btn ->
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(text = StringTemplateParser.parse(btn.label, "Sample Title"), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun PresetManagerSection(presets: List<RpcPreset>, onSave: (String) -> Unit, onDelete: (String) -> Unit, onApply: (RpcPreset) -> Unit) {
    var newPresetName by remember { mutableStateOf("") }
    ConfigSection(title = "Preset Manager", icon = Icons.Default.Star) {
        presets.forEach { preset ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(preset.name, modifier = Modifier.weight(1f))
                TextButton(onClick = { onApply(preset) }) { Text("Apply") }
                IconButton(onClick = { onDelete(preset.name) }) { Icon(Icons.Default.Delete, contentDescription = null) }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = newPresetName, onValueChange = { newPresetName = it }, label = { Text("New Preset Name") }, modifier = Modifier.weight(1f))
            IconButton(onClick = { if (newPresetName.isNotBlank()) { onSave(newPresetName); newPresetName = "" } }) {
                Icon(Icons.Default.Check, contentDescription = "Save")
            }
        }
    }
}

@Composable
fun ConfigSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
            }
            if (expanded) {
                HorizontalDivider()
                Column(modifier = Modifier.padding(8.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun TemplateField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        supportingText = { Text("Placeholders: {title}, {artist}, {album}, {app}, {duration}, {progress}") }
    )
}

@Composable
fun ActivityTypeDropdown(current: DiscordActivityType, onSelect: (DiscordActivityType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Activity Type: ${current.name}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DiscordActivityType.entries.forEach { type ->
                DropdownMenuItem(text = { Text(type.name) }, onClick = { onSelect(type); expanded = false })
            }
        }
    }
}

@Composable
fun TimestampModeSelector(current: TimestampMode, onSelect: (TimestampMode) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        TimestampMode.entries.forEach { mode ->
            FilterChip(
                selected = current == mode,
                onClick = { onSelect(mode) },
                label = { Text(mode.name) }
            )
        }
    }
}

@Composable
fun CoverArtSourceSelector(current: CoverArtSource, onSelect: (CoverArtSource) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        CoverArtSource.entries.forEach { source ->
            FilterChip(
                selected = current == source,
                onClick = { onSelect(source) },
                label = { Text(source.name.replace("_", " ")) }
            )
        }
    }
}

@Composable
fun ButtonEditRow(button: RpcButtonConfig, onUpdate: (RpcButtonConfig) -> Unit, onDelete: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).padding(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = button.label, onValueChange = { onUpdate(button.copy(label = it)) }, label = { Text("Label") }, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
        }
        OutlinedTextField(value = button.urlTemplate, onValueChange = { onUpdate(button.copy(urlTemplate = it)) }, label = { Text("URL Template") }, modifier = Modifier.fillMaxWidth())
    }
}
