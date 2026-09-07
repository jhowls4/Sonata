package com.example.sonata.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
                ConfigSection(title = "Activity Info", icon = Icons.Default.Info) {
                    OutlinedTextField(
                        value = config.applicationId,
                        onValueChange = { viewModel.updateConfig(config.copy(applicationId = it)) },
                        label = { Text("Application ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ActivityTypeDropdown(config.activityType) {
                        viewModel.updateConfig(config.copy(activityType = it))
                    }
                }
            }

            item {
                ConfigSection(title = "Templates", icon = Icons.Default.Edit) {
                    TemplateField("Details", config.detailsTemplate) { viewModel.updateConfig(config.copy(detailsTemplate = it)) }
                    TemplateField("State", config.stateTemplate) { viewModel.updateConfig(config.copy(stateTemplate = it)) }
                    TemplateField("Large Image Hover", config.largeImageHoverTemplate) { viewModel.updateConfig(config.copy(largeImageHoverTemplate = it)) }
                    TemplateField("Small Image Hover", config.smallImageHoverTemplate) { viewModel.updateConfig(config.copy(smallImageHoverTemplate = it)) }
                }
            }

            item {
                ConfigSection(title = "Timestamps", icon = Icons.Default.DateRange) {
                    TimestampModeSelector(config.timestampMode) {
                        viewModel.updateConfig(config.copy(timestampMode = it))
                    }
                }
            }

            item {
                ConfigSection(title = "Assets & Art", icon = Icons.Default.ThumbUp) {
                    CoverArtSourceSelector(config.coverArtSource) {
                        viewModel.updateConfig(config.copy(coverArtSource = it))
                    }
                    if (config.coverArtSource == CoverArtSource.CUSTOM) {
                        OutlinedTextField(
                            value = config.customImageUrl ?: "",
                            onValueChange = { viewModel.updateConfig(config.copy(customImageUrl = it)) },
                            label = { Text("Custom Image URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = config.fallbackAssetKey,
                        onValueChange = { viewModel.updateConfig(config.copy(fallbackAssetKey = it)) },
                        label = { Text("Fallback/Small Asset Key") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = config.showSmallBadge, onCheckedChange = { viewModel.updateConfig(config.copy(showSmallBadge = it)) })
                        Text("Show Small Badge")
                    }
                }
            }

            item {
                ConfigSection(title = "Buttons", icon = Icons.Default.List) {
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
    }
}

@Composable
fun RpcPreviewCard(config: RpcCustomizationConfig) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2D31)), // Discord dark
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(64.dp)) {
                AsyncImage(
                    model = "https://via.placeholder.com/128", // Placeholder for art
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                if (config.showSmallBadge) {
                    Box(modifier = Modifier.size(24.dp).align(Alignment.BottomEnd).clip(RoundedCornerShape(12.dp)).padding(2.dp)) {
                        AsyncImage(
                            model = "https://via.placeholder.com/48", // Placeholder for small badge
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Sonata",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = StringTemplateParser.parse(config.detailsTemplate, "Sample Title", "Sample Artist"),
                    color = Color.White,
                    fontSize = 12.sp
                )
                Text(
                    text = StringTemplateParser.parse(config.stateTemplate, "Sample Title", "Sample Artist"),
                    color = Color(0xFFB5BAC1),
                    fontSize = 12.sp
                )
                if (config.timestampMode != TimestampMode.OFF) {
                    Text(
                        text = "01:23 elapsed",
                        color = Color(0xFFB5BAC1),
                        fontSize = 11.sp
                    )
                }
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
        supportingText = { Text("Available: {title}, {artist}, {album}, {app}, {duration}, {progress}") }
    )
}

@Composable
fun ActivityTypeDropdown(current: Int, onSelect: (Int) -> Unit) {
    val types = listOf(0 to "Playing", 2 to "Listening", 3 to "Watching", 5 to "Competing")
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Activity Type: ${types.find { it.first == current }?.second ?: "Unknown"}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            types.forEach { (value, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { onSelect(value); expanded = false })
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
