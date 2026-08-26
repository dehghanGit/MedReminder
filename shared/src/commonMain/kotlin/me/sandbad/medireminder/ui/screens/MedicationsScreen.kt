package me.sandbad.medireminder.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.sandbad.medireminder.core.formatHm
import me.sandbad.medireminder.core.model.Medication
import me.sandbad.medireminder.core.model.Schedule
import me.sandbad.medireminder.core.model.ScheduleType
import me.sandbad.medireminder.ui.components.EmptyState
import me.sandbad.medireminder.ui.components.MedicationAvatar
import me.sandbad.medireminder.ui.theme.RefillOrange
import me.sandbad.medireminder.ui.theme.TextSecondary
import me.sandbad.medireminder.ui.viewmodel.MedicationsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MedicationsScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val viewModel = koinViewModel<MedicationsViewModel>()
    val state by viewModel.state.collectAsState()
    var pendingDelete by remember { mutableStateOf<Medication?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Medications") },
                actions = {
                    TextButton(onClick = viewModel::toggleArchivedVisible) {
                        Text(if (state.showArchived) "Hide archived" else "Show archived")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add medication")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        if (state.medications.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "No medications yet",
                    message = "Add the first one to start getting reminders.",
                    action = { Button(onClick = onAdd) { Text("Add medication") } }
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(state.medications, key = { it.id }) { medication ->
                MedicationCard(
                    medication = medication,
                    schedules = state.schedulesByMedication[medication.id].orEmpty(),
                    onClick = { onEdit(medication.id) },
                    onArchive = { viewModel.archive(medication.id) },
                    onUnarchive = { viewModel.unarchive(medication.id) },
                    onDelete = { pendingDelete = medication }
                )
            }
        }
    }

    pendingDelete?.let { medication ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${medication.name}?") },
            text = { Text("This also removes its schedule and dose history. Archiving keeps the history instead.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(medication.id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MedicationCard(
    medication: Medication,
    schedules: List<Schedule>,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MedicationAvatar(medication.color, medication.form)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    medication.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    schedules.summary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (medication.needsRefill) {
                    Text(
                        "Refill soon · ${medication.stockCount?.doseLabel(medication.form.unitLabel)} left",
                        style = MaterialTheme.typography.bodySmall,
                        color = RefillOrange
                    )
                }
                if (medication.isArchived) {
                    Text("Archived", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { menuOpen = false; onClick() }
                    )
                    if (medication.isArchived) {
                        DropdownMenuItem(
                            text = { Text("Restore") },
                            onClick = { menuOpen = false; onUnarchive() }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Archive") },
                            onClick = { menuOpen = false; onArchive() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
}

/** "Every day · 08:00, 20:00" — one line describing when this medication is due. */
private fun List<Schedule>.summary(): String {
    if (isEmpty()) return "No schedule"
    return joinToString(" · ") { schedule ->
        val cadence = when (schedule.scheduleType) {
            ScheduleType.DAILY -> "Every day"
            ScheduleType.SPECIFIC_DAYS -> schedule.daysOfWeek.sorted().joinToString(", ") { dayName(it) }
            ScheduleType.INTERVAL_DAYS -> "Every ${schedule.intervalDays ?: 1} days"
            ScheduleType.AS_NEEDED -> "As needed"
        }
        val times = schedule.timesOfDay.joinToString(", ") { it.formatHm() }
        if (times.isBlank()) cadence else "$cadence · $times"
    }
}

internal fun dayName(isoDay: Int): String = when (isoDay) {
    1 -> "Mon"; 2 -> "Tue"; 3 -> "Wed"; 4 -> "Thu"; 5 -> "Fri"; 6 -> "Sat"; else -> "Sun"
}
