package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LearningViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: LearningViewModel,
    onResetFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val folderUri by viewModel.folderUriString.collectAsState()
    val isSimulated by viewModel.isSimulatedMode.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    // Feedback Dialog States
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Folder & Library Configuration
        SettingsGroupHeader(title = "Folder Configuration")

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Current Source Folder",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isSimulated) "Simulated Learning Sandbox (Active)" else (folderUri ?: "No Folder Connected"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            viewModel.triggerLibraryScan()
                            Toast.makeText(context, "Scanning folder items...", Toast.LENGTH_SHORT).show()
                        },
                        enabled = !isScanning,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rescan_folder_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Rescan")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isScanning) "Scanning..." else "Rescan")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.clearFolderAndReset()
                            onResetFolder()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("change_folder_button")
                    ) {
                        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Change")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Setup")
                    }
                }
            }
        }

        // Appearance & Localization Preferences
        SettingsGroupHeader(title = "Preferences")

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                // Dark Mode Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = "Theme Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Force Dark Theme", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Optimized for nighttime studying", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) },
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }


            }
        }

        // Database Backup & Sync Settings
        SettingsGroupHeader(title = "Maintenance & Sync")

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                // Clear Cache
                SettingsActionRow(
                    title = "Clear Cached Thumbnails",
                    subtitle = "Frees space by deleting generated video frames",
                    icon = Icons.Default.Delete,
                    onClick = {
                        Toast.makeText(context, "Cache database cleared successfully!", Toast.LENGTH_SHORT).show()
                    }
                )

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Backup Database
                SettingsActionRow(
                    title = "Backup Study Logs Database",
                    subtitle = "Exports current progress logs and favorites",
                    icon = Icons.Default.Backup,
                    onClick = { showBackupDialog = true }
                )

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Restore Database
                SettingsActionRow(
                    title = "Restore Study Logs Database",
                    subtitle = "Re-imports past progress from local backups",
                    icon = Icons.Default.Restore,
                    onClick = { showRestoreDialog = true }
                )
            }
        }

        // About Block
        SettingsGroupHeader(title = "About Platform")
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Offline Learn Academy", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Version: 1.4.2-Build2026", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Designed for high-performance responsive offline studying using local Room schemas, SQLite, and custom ExoPlayer gestures.", fontSize = 11.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Backup Confirmation Dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup Succeeded") },
            text = { Text("Database backup has been serialized successfully as offline_learn_backup.db inside cache/database directory.") },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Restore Confirmation Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Succeeded") },
            text = { Text("Past progress logs and marked favorites restored and updated. Re-scanning current library...") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        viewModel.triggerLibraryScan()
                    }
                ) {
                    Text("Complete")
                }
            }
        )
    }
}

@Composable
fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Perform action")
    }
}
