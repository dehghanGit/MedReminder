package me.sandbad.medireminder.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalTime
import me.sandbad.medireminder.core.formatHm
import me.sandbad.medireminder.core.model.MedicationForm
import me.sandbad.medireminder.core.model.ScheduleType
import me.sandbad.medireminder.core.model.StrengthUnit
import me.sandbad.medireminder.resources.*
import me.sandbad.medireminder.ui.viewmodel.MedicationEditViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

private val Ink = Color(0xFF07154F)
private val Purple = Color(0xFF6C35F1)
private val FieldBorder = Color(0xFFD9DCF0)
private val Pale = Color(0xFFF7F6FF)
private val Muted = Color(0xFF9BA0BC)
private val ActionGradient = Brush.horizontalGradient(listOf(Color(0xFFB73CFF), Color(0xFFFF58AB)))

@Composable
fun MedicationEditScreen(medicationId: Long, onBack: () -> Unit) {
    val viewModel = koinViewModel<MedicationEditViewModel>()
    val state by viewModel.state.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }
    var alarmSelected by remember { mutableStateOf(true) }

    LaunchedEffect(medicationId) { viewModel.load(medicationId) }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.consumeSaved()
            onBack()
        }
    }

    Scaffold(
        containerColor = Color.White,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth().widthIn(max = 756.dp)
                    .padding(horizontal = 16.dp).height(56.dp),
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.Transparent,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Box(
                    Modifier.fillMaxSize().background(ActionGradient),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                if (state.isEditing) "Save changes" else "Add medication",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEditing) "Edit Medication" else "Add Medication",
                        color = Ink,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.MoreVert, "More options", tint = Ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { scaffoldPadding ->
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(scaffoldPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            val sidePadding = if (maxWidth > 700.dp) 32.dp else 16.dp
            Column(
                Modifier.fillMaxWidth().widthIn(max = 820.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = sidePadding, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FormSection(state.form, viewModel::setForm)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FormLabel("Name")
                    StyledTextField(
                        value = state.name,
                        onValueChange = viewModel::setName,
                        placeholder = "e.g. Amoxicillin",
                        isError = state.nameError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1.42f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FormLabel("Amount per dose")
                        QuantityStepper(
                            state.quantity,
                            onDecrease = { viewModel.setQuantity(stepQuantity(state.quantity, -1)) },
                            onIncrease = { viewModel.setQuantity(stepQuantity(state.quantity, 1)) }
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FormLabel("Strength", "Optional")
                        StyledTextField(
                            state.strength, viewModel::setStrength, "e.g. 500",
                            modifier = Modifier.fillMaxWidth(), numeric = true
                        )
                    }
                    Column(Modifier.weight(.82f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FormLabel("Unit", "Optional")
                        UnitDropdown(state.strengthUnit, viewModel::setStrengthUnit)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FormLabel("Schedule")
                    SegmentedChoice(
                        "Time based", "As needed",
                        state.scheduleType != ScheduleType.AS_NEEDED,
                        Icons.Outlined.Schedule, Icons.Filled.AutoAwesome,
                        { viewModel.setScheduleType(ScheduleType.DAILY) },
                        { viewModel.setScheduleType(ScheduleType.AS_NEEDED) }
                    )
                }

                if (state.needsTimes) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FormLabel("Frequency")
                        FrequencySelector(
                            state.scheduleType,
                            onDaily = viewModel::selectDaily,
                            onWeekly = viewModel::selectWeekly,
                            onMonthly = viewModel::selectMonthly
                        )
                        FrequencyDetail(state, viewModel)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FormLabel("Time")
                        state.times.forEach { time ->
                            TimeRow(time.formatHm()) { viewModel.removeTime(time) }
                        }
                        AddTimeButton { showTimePicker = true }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FormLabel("Reminder")
                        SegmentedChoice(
                            "Alarm", "Notification", alarmSelected,
                            Icons.Filled.Notifications, Icons.Outlined.ChatBubbleOutline,
                            { alarmSelected = true }, { alarmSelected = false }
                        )
                    }
                }

                Spacer(Modifier.height(84.dp))
            }
        }
    }

    if (showTimePicker) {
        WheelTimePickerDialog(
            initial = LocalTime(8, 0),
            onDismiss = { showTimePicker = false },
            onConfirm = { viewModel.addTime(it); showTimePicker = false }
        )
    }
}

@Composable
private fun FormSection(selected: MedicationForm, onSelect: (MedicationForm) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FormLabel("Medicine form")
        Row(
            Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(20.dp)).background(Pale)
                .horizontalScroll(rememberScrollState()).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FormOption("Capsule", MedicationForm.CAPSULE, selected, Res.drawable.form_capsule, onSelect)
            FormOption("Tablet", MedicationForm.TABLET, selected, null, onSelect)
            FormOption("Pill", MedicationForm.OTHER, selected, Res.drawable.form_pill, onSelect)
            FormOption("Chewable", MedicationForm.PATCH, selected, Res.drawable.form_chewable, onSelect)
            FormOption("Liquid", MedicationForm.LIQUID, selected, Res.drawable.form_liquid, onSelect)
        }
    }
}

@Composable
private fun FormOption(
    label: String,
    value: MedicationForm,
    selected: MedicationForm,
    image: DrawableResource?,
    onSelect: (MedicationForm) -> Unit
) {
    val active = value == selected
    Column(
        Modifier.width(94.dp).fillMaxHeight().clip(RoundedCornerShape(16.dp))
            .then(if (active) Modifier.border(2.dp, Purple, RoundedCornerShape(16.dp)) else Modifier)
            .background(if (active) Color.White.copy(alpha = .58f) else Color.Transparent)
            .clickable { onSelect(value) }.padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (image != null) {
            Image(painterResource(image), null, Modifier.size(50.dp), contentScale = ContentScale.Fit)
        } else {
            Box(
                Modifier.size(44.dp).background(
                    Brush.linearGradient(listOf(Color(0xFFFFA1D2), Color(0xFFFF5EAB))),
                    CircleShape
                )
            )
        }
        Text(label, color = if (active) Purple else Ink, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FormLabel(text: String, suffix: String? = null) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(text, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        suffix?.let { Text("($it)", color = Color(0xFF46549A), fontSize = 13.sp) }
    }
}

@Composable
private fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier,
    numeric: Boolean = false,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(56.dp),
        placeholder = { Text(placeholder, color = Muted, fontSize = 15.sp) },
        singleLine = true,
        isError = isError,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Purple,
            unfocusedBorderColor = FieldBorder,
            focusedTextColor = Ink,
            unfocusedTextColor = Ink,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
    )
}

@Composable
private fun QuantityStepper(value: String, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(Pale),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton(Icons.Filled.Remove, "Decrease", onDecrease)
        Box(
            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(.75f)),
            contentAlignment = Alignment.Center
        ) {
            Text(value, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        StepButton(Icons.Filled.Add, "Increase", onIncrease)
    }
}

@Composable
private fun RowScope.StepButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(Modifier.width(54.dp).fillMaxHeight().clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Box(Modifier.size(40.dp).background(Color(0xFFE9E4FF), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, description, tint = Purple, modifier = Modifier.size(23.dp))
        }
    }
}

@Composable
private fun UnitDropdown(selected: StrengthUnit, onSelect: (StrengthUnit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().height(56.dp).border(1.dp, FieldBorder, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp)).clickable { expanded = true }.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(selected.label.ifBlank { "–" }, Modifier.weight(1f), color = Ink, fontSize = 16.sp)
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = Ink)
        }
        DropdownMenu(expanded, { expanded = false }) {
            StrengthUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.label.ifBlank { "None" }) },
                    onClick = { onSelect(unit); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun SegmentedChoice(
    leftLabel: String,
    rightLabel: String,
    leftSelected: Boolean,
    leftIcon: ImageVector,
    rightIcon: ImageVector,
    onLeft: () -> Unit,
    onRight: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).border(1.dp, FieldBorder, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
    ) {
        Segment(leftLabel, leftIcon, leftSelected, onLeft, Modifier.weight(1f))
        Segment(rightLabel, rightIcon, !leftSelected, onRight, Modifier.weight(1f))
    }
}

@Composable
private fun Segment(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val background = if (active) {
        Brush.horizontalGradient(listOf(Color(0xFF8651FA), Color(0xFF7135ED)))
    } else {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }
    Row(
        modifier.fillMaxHeight().background(background).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (active) Color.White else if (label == "As needed") Purple else Ink, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(9.dp))
        Text(label, color = if (active) Color.White else Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FrequencySelector(
    selected: ScheduleType,
    onDaily: () -> Unit,
    onWeekly: () -> Unit,
    onMonthly: () -> Unit
) {
    val dailyActive = selected == ScheduleType.DAILY || selected == ScheduleType.INTERVAL_DAYS
    Row(
        Modifier.fillMaxWidth().height(60.dp).border(1.dp, FieldBorder, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
    ) {
        FrequencyItem("Daily", Icons.Outlined.CalendarToday, dailyActive, Modifier.weight(1f), onDaily)
        FrequencyItem("Weekly", Icons.Outlined.DateRange, selected == ScheduleType.SPECIFIC_DAYS, Modifier.weight(1f), onWeekly)
        FrequencyItem("Monthly", Icons.Outlined.CalendarMonth, selected == ScheduleType.MONTHLY_DAYS, Modifier.weight(1f), onMonthly)
    }
}

/** The secondary control under the frequency selector: interval, weekday chips, or day-of-month grid. */
@Composable
private fun FrequencyDetail(state: me.sandbad.medireminder.ui.viewmodel.MedicationEditState, viewModel: MedicationEditViewModel) {
    when (state.scheduleType) {
        ScheduleType.DAILY, ScheduleType.INTERVAL_DAYS ->
            IntervalStepper(state.dailyInterval, viewModel::setDailyInterval)
        ScheduleType.SPECIFIC_DAYS ->
            WeekdayPicker(state.daysOfWeek, viewModel::toggleDay)
        ScheduleType.MONTHLY_DAYS ->
            MonthDayPicker(state.daysOfMonth, viewModel::toggleDayOfMonth)
        ScheduleType.AS_NEEDED -> Unit
    }
}

@Composable
private fun IntervalStepper(days: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(Pale),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepButton(Icons.Filled.Remove, "Repeat less often", { onChange(days - 1) })
        Box(
            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(.75f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (days <= 1) "Every day" else "Every $days days",
                color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold
            )
        }
        StepButton(Icons.Filled.Add, "Repeat more often", { onChange(days + 1) })
    }
}

@Composable
private fun WeekdayPicker(selected: Set<Int>, onToggle: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S").forEach { (iso, label) ->
            val active = iso in selected
            Box(
                Modifier.size(42.dp).clip(CircleShape)
                    .background(if (active) Purple else Pale)
                    .clickable { onToggle(iso) },
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (active) Color.White else Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthDayPicker(selected: Set<Int>, onToggle: (Int) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        maxItemsInEachRow = 7
    ) {
        (1..31).forEach { day ->
            val active = day in selected
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (active) Purple else Pale)
                    .clickable { onToggle(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(day.toString(), color = if (active) Color.White else Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FrequencyItem(label: String, icon: ImageVector, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxHeight().background(if (active) Color(0xFFEDE7FF) else Color.White).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (active) Purple else Ink, modifier = Modifier.size(23.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (active) Purple else Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TimeRow(label: String, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).border(1.dp, FieldBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)).background(Pale).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Schedule, null, tint = Purple, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(formatDisplayTime(label), Modifier.weight(1f), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Icon(
            Icons.Filled.Close, "Remove time", tint = Color(0xFF59639C),
            modifier = Modifier.size(22.dp).clip(CircleShape).clickable(onClick = onRemove)
        )
    }
}

@Composable
private fun AddTimeButton(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEDE9FF)).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Add, null, tint = Purple, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text("Add time", color = Purple, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatDisplayTime(value: String): String {
    val parts = value.split(':')
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return value
    val minute = parts.getOrNull(1) ?: return value
    val period = if (hour < 12) "AM" else "PM"
    val twelveHour = when (hour) { 0 -> 12; in 13..23 -> hour - 12; else -> hour }
    return "$twelveHour:$minute $period"
}

private fun stepQuantity(value: String, amount: Int): String {
    val result = ((value.toDoubleOrNull() ?: 1.0) + amount).coerceAtLeast(1.0)
    return if (result % 1.0 == 0.0) result.toInt().toString() else result.toString()
}

private val WheelItemHeight = 54.dp

@Composable
private fun WheelTimePickerDialog(initial: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val hours = remember { (1..12).map { it.toString() } }
    val minutes = remember { (0..59).map { it.toString().padStart(2, '0') } }
    val periods = remember { listOf("AM", "PM") }

    val initHour12 = ((initial.hour + 11) % 12) + 1
    val hourState = rememberLazyListState(hours.indexOf(initHour12.toString()).coerceAtLeast(0))
    val minuteState = rememberLazyListState(initial.minute)
    val periodState = rememberLazyListState(if (initial.hour < 12) 0 else 1)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Pick a time", color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Box(Modifier.fillMaxWidth().height(WheelItemHeight * 3), contentAlignment = Alignment.Center) {
                // Centre highlight band behind the wheels.
                Box(
                    Modifier.fillMaxWidth().height(WheelItemHeight).clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFEDE9FF))
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    WheelColumn(hours, hourState, Modifier.weight(1f))
                    Text(":", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    WheelColumn(minutes, minuteState, Modifier.weight(1f))
                    WheelColumn(periods, periodState, Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h12 = hours[hourState.firstVisibleItemIndex.coerceIn(hours.indices)].toInt()
                val min = minuteState.firstVisibleItemIndex.coerceIn(minutes.indices)
                val pm = periodState.firstVisibleItemIndex == 1
                val hour24 = when {
                    h12 == 12 -> if (pm) 12 else 0
                    pm -> h12 + 12
                    else -> h12
                }
                onConfirm(LocalTime(hour24, min))
            }) { Text("Add", color = Purple, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) } }
    )
}

@Composable
private fun WheelColumn(items: List<String>, state: LazyListState, modifier: Modifier) {
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    LazyColumn(
        modifier = modifier.height(WheelItemHeight * 3),
        state = state,
        flingBehavior = fling,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Spacer(Modifier.height(WheelItemHeight)) }
        itemsIndexed(items) { index, label ->
            val selected = state.firstVisibleItemIndex == index
            Box(Modifier.fillMaxWidth().height(WheelItemHeight), contentAlignment = Alignment.Center) {
                Text(
                    label,
                    color = if (selected) Ink else Muted,
                    fontSize = if (selected) 30.sp else 19.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
        item { Spacer(Modifier.height(WheelItemHeight)) }
    }
}
