package me.sandbad.medireminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime
import me.sandbad.medireminder.core.formatHm
import me.sandbad.medireminder.core.model.MedColor
import me.sandbad.medireminder.core.model.MedicationForm
import me.sandbad.medireminder.core.model.ScheduleType
import me.sandbad.medireminder.core.model.StrengthUnit
import me.sandbad.medireminder.core.parseHm
import me.sandbad.medireminder.ui.components.LabeledField
import me.sandbad.medireminder.ui.components.PrimaryButton
import me.sandbad.medireminder.ui.components.SelectableChip
import me.sandbad.medireminder.ui.theme.TextSecondary
import me.sandbad.medireminder.ui.viewmodel.MedicationEditViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MedicationEditScreen(
    medicationId: Long,
    onBack: () -> Unit
) {
    val viewModel = koinViewModel<MedicationEditViewModel>()
    val state by viewModel.state.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(medicationId) { viewModel.load(medicationId) }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.consumeSaved()
            onBack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit medication" else "New medication") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            LabeledField(
                label = "Name",
                value = state.name,
                onValueChange = viewModel::setName,
                placeholder = "e.g. Metformin",
                isError = state.nameError != null,
                supportingText = state.nameError
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    LabeledField(
                        label = "Strength",
                        value = state.strength,
                        onValueChange = viewModel::setStrength,
                        placeholder = "500",
                        numeric = true
                    )
                }
                Box(Modifier.weight(1f)) {
                    EnumDropdown(
                        label = "Unit",
                        options = StrengthUnit.entries,
                        selected = state.strengthUnit,
                        optionLabel = { it.label.ifBlank { "none" } },
                        onSelect = viewModel::setStrengthUnit
                    )
                }
            }

            LabelledGroup("Form") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MedicationForm.entries.forEach { form ->
                        SelectableChip(
                            label = form.label,
                            selected = state.form == form,
                            onClick = { viewModel.setForm(form) }
                        )
                    }
                }
            }

            LabelledGroup("Colour") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MedColor.entries.forEach { color ->
                        val selected = state.color == color
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(color.hex))
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setColor(color) }
                        )
                    }
                }
            }

            HorizontalDivider()

            LabelledGroup("Schedule") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScheduleType.entries.forEach { type ->
                        SelectableChip(
                            label = type.label,
                            selected = state.scheduleType == type,
                            onClick = { viewModel.setScheduleType(type) }
                        )
                    }
                }
            }

            if (state.scheduleType == ScheduleType.SPECIFIC_DAYS) {
                LabelledGroup("Days") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..7).forEach { day ->
                            SelectableChip(
                                label = dayName(day),
                                selected = day in state.daysOfWeek,
                                onClick = { viewModel.toggleDay(day) }
                            )
                        }
                    }
                }
            }

            if (state.scheduleType == ScheduleType.INTERVAL_DAYS) {
                LabeledField(
                    label = "Repeat every (days)",
                    value = state.intervalDays,
                    onValueChange = viewModel::setIntervalDays,
                    numeric = true
                )
            }

            if (state.needsTimes) {
                LabelledGroup("Times") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.times.forEach { time ->
                            InputChip(
                                selected = false,
                                onClick = { viewModel.removeTime(time) },
                                label = { Text(time.formatHm()) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove ${time.formatHm()}",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                        AssistChip(
                            onClick = { showTimePicker = true },
                            label = { Text("Add time") },
                            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) }
                        )
                    }
                }
            }

            LabeledField(
                label = "Amount per dose",
                value = state.quantity,
                onValueChange = viewModel::setQuantity,
                numeric = true,
                placeholder = "1"
            )

            HorizontalDivider()

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    LabeledField(
                        label = "In stock",
                        value = state.stockCount,
                        onValueChange = viewModel::setStockCount,
                        numeric = true,
                        placeholder = "optional"
                    )
                }
                Box(Modifier.weight(1f)) {
                    LabeledField(
                        label = "Refill alert at",
                        value = state.refillAt,
                        onValueChange = viewModel::setRefillAt,
                        numeric = true,
                        placeholder = "optional"
                    )
                }
            }

            LabeledField(
                label = "Instructions",
                value = state.instructions,
                onValueChange = viewModel::setInstructions,
                placeholder = "e.g. with food"
            )

            LabeledField(
                label = "Notes",
                value = state.notes,
                onValueChange = viewModel::setNotes,
                singleLine = false
            )

            PrimaryButton(
                text = if (state.isEditing) "Save changes" else "Add medication",
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                loading = state.isSaving
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showTimePicker) {
        TimeEntryDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                viewModel.addTime(time)
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun LabelledGroup(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        content()
    }
}

@Composable
private fun <T> EnumDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(optionLabel(selected), Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Compose Multiplatform has no shared time picker, so times are typed as HH:mm.
 * Kept deliberately small — the field validates before the dialog will close.
 */
@Composable
private fun TimeEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    var text by remember { mutableStateOf("08:00") }
    val parsed = parseHm(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a time") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    isError = parsed == null,
                    label = { Text("HH:mm") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("08:00", "12:00", "18:00", "22:00").forEach { preset ->
                        SelectableChip(preset, selected = text == preset, onClick = { text = preset })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = parsed != null
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
