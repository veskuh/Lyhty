package net.veskuh.lyhty.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import net.veskuh.lyhty.ui.state.ReaderTheme
import androidx.compose.material3.Switch
import net.veskuh.lyhty.util.LogLevel
import net.veskuh.lyhty.util.LyhtyLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPane(
    initialServerUrl: String,
    initialApiKey: String,
    currentLogLevel: LogLevel,
    fontSizeScale: Float,
    readerTheme: ReaderTheme,
    showOnlyUnreadFeeds: Boolean = true,
    historyCount: Int = 0,
    isLoading: Boolean = false,
    hasError: Boolean = false,
    onSaveConfig: (String, String) -> Unit,
    onSaveLogLevel: (LogLevel) -> Unit,
    onSetTheme: (ReaderTheme) -> Unit,
    onSetFontSizeScale: (Float) -> Unit,
    onSetShowOnlyUnreadFeeds: ((Boolean) -> Unit)? = null,
    onClearHistory: (() -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var serverUrl by remember(initialServerUrl) { mutableStateOf(initialServerUrl) }
    var apiKey by remember(initialApiKey) { mutableStateOf(initialApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    var urlValidationError by remember { mutableStateOf<String?>(null) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var isSavingServerConfig by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading, hasError) {
        if (isSavingServerConfig && !isLoading) {
            isSavingServerConfig = false
            if (!hasError) {
                android.widget.Toast.makeText(
                    context,
                    "Miniflux server connection updated successfully!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                onBack()
            }
        }
    }

    var localFontSize by remember(fontSizeScale) { mutableFloatStateOf(fontSizeScale) }

    var selectedLogLevel by remember(currentLogLevel) { mutableStateOf(currentLogLevel) }
    var isLogLevelDropdownExpanded by remember { mutableStateOf(false) }
    val logLevels = LogLevel.entries

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "App Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                ) {
                    // --- SECTION 1: Reader Appearance & Typography ---
                    Text(
                        text = "Reader Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reader Theme",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ThemeCard(
                                    title = "OLED Dark",
                                    bgColor = Color(0xFF000000),
                                    textColor = Color(0xFFE0E0E0),
                                    isSelected = readerTheme == ReaderTheme.OLED_DARK,
                                    onClick = { onSetTheme(ReaderTheme.OLED_DARK) },
                                    modifier = Modifier.weight(1f)
                                )
                                ThemeCard(
                                    title = "Sepia",
                                    bgColor = Color(0xFFFBF0D9),
                                    textColor = Color(0xFF5F4B32),
                                    isSelected = readerTheme == ReaderTheme.SEPIA,
                                    onClick = { onSetTheme(ReaderTheme.SEPIA) },
                                    modifier = Modifier.weight(1f)
                                )
                                ThemeCard(
                                    title = "Light",
                                    bgColor = Color(0xFFFFFFFF),
                                    textColor = Color(0xFF1A1A1A),
                                    isSelected = readerTheme == ReaderTheme.LIGHT,
                                    onClick = { onSetTheme(ReaderTheme.LIGHT) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Font Size (${String.format("%.1fx", localFontSize)})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Slider(
                                value = localFontSize,
                                onValueChange = { scale ->
                                    localFontSize = scale
                                },
                                onValueChangeFinished = {
                                    onSetFontSizeScale(localFontSize)
                                },
                                valueRange = 0.8f..1.8f,
                                steps = 10,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Live Preview Box matching ReaderContent base (16sp / 24sp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Preview: Typography line height and text scaling for comfortable reading on foldable screens.",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = (16 * localFontSize).sp,
                                        lineHeight = (24 * localFontSize).sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- SECTION 2: Feed Navigation ---
                    Text(
                        text = "Feed Navigation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hide feeds with no unread items",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Only show feeds and categories that currently have unread articles.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (onSetShowOnlyUnreadFeeds != null) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Switch(
                                    checked = showOnlyUnreadFeeds,
                                    onCheckedChange = onSetShowOnlyUnreadFeeds
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- SECTION 3: Miniflux Connection ---
                    Text(
                        text = "Miniflux Server Connection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = serverUrl,
                                onValueChange = {
                                    serverUrl = it
                                    urlValidationError = null
                                },
                                label = { Text("Server URL") },
                                leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) },
                                isError = urlValidationError != null,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (urlValidationError != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = urlValidationError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = {
                                    apiKey = it
                                    urlValidationError = null
                                },
                                label = { Text("API Key") },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                        Icon(
                                            imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isApiKeyVisible) "Hide API key" else "Show API key"
                                        )
                                    }
                                },
                                visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val trimmedUrl = serverUrl.trim()
                                    val trimmedKey = apiKey.trim()
                                    if (trimmedUrl.isBlank() || (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://"))) {
                                        urlValidationError = "Server URL must start with http:// or https://"
                                    } else if (trimmedKey.isBlank()) {
                                        urlValidationError = "API Key cannot be empty"
                                    } else {
                                        urlValidationError = null
                                        showConfirmationDialog = true
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                if (isSavingServerConfig && isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connecting...")
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Connection")
                                }
                            }
                        }
                    }

                    if (showConfirmationDialog) {
                        AlertDialog(
                            onDismissRequest = { showConfirmationDialog = false },
                            title = { Text("Reconfigure Server Connection?") },
                            text = {
                                Text("Updating your server credentials will clear existing cached articles and sync fresh categories and feeds from your Miniflux server.")
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showConfirmationDialog = false
                                        isSavingServerConfig = true
                                        onSaveConfig(serverUrl.trim(), apiKey.trim())
                                    }
                                ) {
                                    Text("Save & Sync")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showConfirmationDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- SECTION 3: Reading History ---
                    Text(
                        text = "Reading History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val historyText = when (historyCount) {
                                    0 -> "No reading history recorded"
                                    1 -> "1 article in reading history"
                                    else -> "$historyCount articles in reading history"
                                }
                                Text(
                                    text = historyText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (historyCount > 0 && onClearHistory != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = { showClearHistoryDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Clear Reading History")
                                }
                            }
                        }
                    }

                    if (showClearHistoryDialog) {
                        AlertDialog(
                            onDismissRequest = { showClearHistoryDialog = false },
                            title = { Text("Clear Reading History?") },
                            text = { Text("This will remove all recorded reading history from this device. Your articles and unread/read status on the server will not be changed.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showClearHistoryDialog = false
                                        onClearHistory?.invoke()
                                        android.widget.Toast.makeText(context, "Reading history cleared", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Clear", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showClearHistoryDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- SECTION 4: Diagnostics & Utilities ---
                    Text(
                        text = "Diagnostics & System",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Log Severity Level",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            ExposedDropdownMenuBox(
                                expanded = isLogLevelDropdownExpanded,
                                onExpandedChange = { isLogLevelDropdownExpanded = !isLogLevelDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedLogLevel.name,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isLogLevelDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = isLogLevelDropdownExpanded,
                                    onDismissRequest = { isLogLevelDropdownExpanded = false }
                                ) {
                                    logLevels.forEach { level ->
                                        DropdownMenuItem(
                                            text = { Text(level.name) },
                                            onClick = {
                                                selectedLogLevel = level
                                                onSaveLogLevel(level)
                                                isLogLevelDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val logFile = LyhtyLogger.getLogFile()
                                    if (logFile != null && logFile.exists()) {
                                        try {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                logFile
                                            )
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Share Lyhty Diagnostic Logs"))
                                        } catch (_: Exception) {
                                            val text = LyhtyLogger.readLogContent()
                                            if (text.isNotBlank()) {
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, text)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Share Lyhty Diagnostic Logs"))
                                            } else {
                                                android.widget.Toast.makeText(context, "No diagnostic log file available yet.", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        android.widget.Toast.makeText(context, "No diagnostic log file available yet.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Diagnostic Logs")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    title: String,
    bgColor: Color,
    textColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
