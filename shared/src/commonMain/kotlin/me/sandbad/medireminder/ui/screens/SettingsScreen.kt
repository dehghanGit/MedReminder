package me.sandbad.medireminder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.sandbad.medireminder.ui.components.LabeledField
import me.sandbad.medireminder.ui.components.SelectableChip
import me.sandbad.medireminder.ui.theme.RefillOrange
import me.sandbad.medireminder.ui.theme.RefillOrangeLight
import me.sandbad.medireminder.ui.theme.TextSecondary
import me.sandbad.medireminder.ui.viewmodel.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(onBack: (() -> Unit)? = null) {
    val viewModel = koinViewModel<SettingsViewModel>()
    val state by viewModel.state.collectAsState()
    val settings = state.settings

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    onBack?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!state.hasNotificationPermission) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = RefillOrangeLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.NotificationsOff, contentDescription = null, tint = RefillOrange)
                        Column(Modifier.weight(1f)) {
                            Text("Notifications are off", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Reminders cannot reach you until notifications are allowed.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = viewModel::requestNotificationPermission) { Text("Allow") }
                    }
                }
            }

            LabeledField(
                label = "Your name",
                value = settings.ownerName.orEmpty(),
                onValueChange = { name -> viewModel.update { it.copy(ownerName = name.ifBlank { null }) } },
                placeholder = "optional"
            )

            SettingsSection("Reminders") {
                ToggleRow(
                    title = "Dose reminders",
                    subtitle = "Notify me when a dose is due",
                    checked = settings.remindersEnabled,
                    onCheckedChange = { value -> viewModel.update { it.copy(remindersEnabled = value) } }
                )
                ToggleRow(
                    title = "Sound",
                    checked = settings.soundEnabled,
                    onCheckedChange = { value -> viewModel.update { it.copy(soundEnabled = value) } }
                )
                ToggleRow(
                    title = "Vibration",
                    checked = settings.vibrationEnabled,
                    onCheckedChange = { value -> viewModel.update { it.copy(vibrationEnabled = value) } }
                )
                ToggleRow(
                    title = "Refill alerts",
                    subtitle = "Warn me when stock runs low",
                    checked = settings.refillAlertsEnabled,
                    onCheckedChange = { value -> viewModel.update { it.copy(refillAlertsEnabled = value) } }
                )
            }

            SettingsSection("Snooze length") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 30).forEach { minutes ->
                        SelectableChip(
                            label = "$minutes min",
                            selected = settings.snoozeMinutes == minutes,
                            onClick = { viewModel.update { it.copy(snoozeMinutes = minutes) } }
                        )
                    }
                }
            }

            SettingsSection("Mark as missed after") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 60, 120, 240).forEach { minutes ->
                        SelectableChip(
                            label = if (minutes < 60) "$minutes min" else "${minutes / 60} h",
                            selected = settings.missedAfterMinutes == minutes,
                            onClick = { viewModel.update { it.copy(missedAfterMinutes = minutes) } }
                        )
                    }
                }
            }

            Text(
                "MediReminder keeps everything on this device — no account, no cloud sync.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(Modifier.padding(vertical = 6.dp, horizontal = 16.dp), content = content)
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
