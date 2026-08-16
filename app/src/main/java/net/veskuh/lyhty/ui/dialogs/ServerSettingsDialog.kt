package net.veskuh.lyhty.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.veskuh.lyhty.util.LogLevel

@Composable
fun ServerSettingsDialog(
    initialServerUrl: String,
    initialApiKey: String,
    currentLogLevel: LogLevel = LogLevel.DEBUG,
    onDismiss: () -> Unit,
    onSaveConfig: (String, String) -> Unit,
    onSaveLogLevel: (LogLevel) -> Unit = {},
    onShareLogs: () -> Unit = {}
) {
    var serverUrl by remember(initialServerUrl) { mutableStateOf(initialServerUrl) }
    var apiKey by remember(initialApiKey) { mutableStateOf(initialApiKey) }
    var selectedLogLevel by remember(currentLogLevel) { mutableStateOf(currentLogLevel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings & Diagnostics") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Miniflux Server Credentials",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("https://reader.example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key / Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Diagnostic Log Level",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LogLevel.entries.forEach { level ->
                        FilterChip(
                            selected = level == selectedLogLevel,
                            onClick = {
                                selectedLogLevel = level
                                onSaveLogLevel(level)
                            },
                            label = { Text(level.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Enforcing Icon + Text Label UX rule for share action button
                OutlinedButton(
                    onClick = onShareLogs,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Diagnostic Logs")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveConfig(serverUrl.trim(), apiKey.trim())
                    onDismiss()
                }
            ) {
                Text("Save & Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
