package me.sandbad.medireminder.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import me.sandbad.medireminder.core.model.DoseStatus
import me.sandbad.medireminder.core.model.DoseWithMedication
import me.sandbad.medireminder.resources.*
import me.sandbad.medireminder.ui.theme.BrandPrimary
import me.sandbad.medireminder.ui.theme.MissedRose
import me.sandbad.medireminder.ui.theme.SkippedAmber
import me.sandbad.medireminder.ui.theme.TakenGreen
import me.sandbad.medireminder.ui.viewmodel.OffScheduleConfirm
import me.sandbad.medireminder.ui.viewmodel.TodayViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

private val Ink = Color(0xFF092457)
private val Purple = Color(0xFF7028EE)
private val Green = Color(0xFF10C77A)
private val Blue = Color(0xFF238CF3)
private val Pink = Color(0xFFF54291)
private val Amber = Color(0xFFFFB900)
private val Muted = Color(0xFF4E5872)

@Composable
fun TodayScreen(
    onAddMedication: () -> Unit,
    onOpenHistory: () -> Unit,
    onEditMedication: (Long) -> Unit = {}
) {
    val viewModel = koinViewModel<TodayViewModel>()
    val state by viewModel.state.collectAsState()

    val doses = state.doses
    val nextDose = state.upcoming.firstOrNull()
    val takenToday = doses.count { it.dose.status == DoseStatus.TAKEN }
    val rows = doses.mapIndexed { i, dose -> dose.toHomeDose(i) }

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 14.dp)
    ) {
        item { BrandHeader() }
        item {
            StreakCard(
                streak = state.streakDays,
                medicationCount = doses.map { it.medication.id }.distinct().size,
                takenToday = takenToday,
                totalToday = doses.size,
                onClick = onOpenHistory
            )
        }
        item { SectionTitle("Next Medication", 16.dp) }
        item {
            if (nextDose != null) {
                NextMedicationCard(
                    name = nextDose.medication.displayName,
                    dose = nextDose.dose.quantity.doseLabel(nextDose.medication.form.unitLabel),
                    time = nextDose.dose.scheduledAt.formatClockTime(),
                    onTake = { viewModel.requestMarkTaken(nextDose.dose.id) }
                )
            } else {
                AllDoneCard(hasDoses = doses.isNotEmpty())
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Reminder List", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    "View All", color = Purple, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onOpenHistory).padding(vertical = 4.dp)
                )
            }
        }
        if (rows.isEmpty() && !state.isLoading) {
            item { EmptyReminders() }
        }
        rows.forEachIndexed { index, row ->
            item(key = row.id) {
                SwipeableReminderRow(
                    row = row,
                    onTake = { viewModel.requestMarkTaken(row.id) },
                    onUndo = { viewModel.undo(row.id) },
                    onEdit = { onEditMedication(row.medicationId) },
                    onArchive = { viewModel.archive(row.medicationId) }
                )
                if (index != rows.lastIndex) Spacer(Modifier.height(7.dp))
            }
        }
        if (state.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(6.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Purple, strokeWidth = 2.dp)
                }
            }
        }
        item { AddMedicationButton(onAddMedication) }
    }

        state.offScheduleConfirm?.let { confirm ->
            OffScheduleDialog(
                confirm = confirm,
                onConfirm = viewModel::confirmOffScheduleTake,
                onDismiss = viewModel::dismissOffScheduleConfirm
            )
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(Modifier.fillMaxWidth().height(82.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(Res.drawable.medirem_logo), "MediRem logo", Modifier.size(43.dp))
        Spacer(Modifier.width(9.dp))
        Text("MediRem", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.7).sp)
        Spacer(Modifier.weight(1f))
        Image(painterResource(Res.drawable.notification_bell), "Notifications", Modifier.size(39.dp))
    }
}

@Composable
private fun StreakCard(streak: Int, medicationCount: Int, takenToday: Int, totalToday: Int, onClick: () -> Unit) {
    val allDone = totalToday > 0 && takenToday == totalToday
    Box(
        Modifier.fillMaxWidth().height(205.dp).clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFF4EBFF), Color(0xFFEDE0FF))))
            .clickable(onClick = onClick)
    ) {
        Image(
            painterResource(Res.drawable.streak_mascot), null,
            Modifier.align(Alignment.BottomEnd).width(168.dp).height(187.dp), contentScale = ContentScale.Fit
        )
        Row(Modifier.fillMaxSize().padding(start = 24.dp, top = 20.dp, bottom = 20.dp, end = 138.dp)) {
            Column(Modifier.width(106.dp)) {
                Text("Your Streak", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(streak.toString(), color = Purple, fontSize = 62.sp, lineHeight = 64.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-2).sp)
                Text(if (streak == 1) "Day" else "Days", color = Purple, fontSize = 22.sp, lineHeight = 23.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (streak > 0) "Great job! 🎉\nYou’re building\nhealthy habits." else "Take a dose to\nstart your\nstreak today.",
                    color = Ink, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 7.dp)
                )
            }
            Box(Modifier.padding(top = 53.dp).width(1.dp).height(112.dp).background(Color(0xFFD9C6FC)))
            Column(Modifier.padding(start = 16.dp, top = 48.dp).widthIn(min = 94.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(Res.drawable.medication_purple), null, Modifier.size(36.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(medicationCount.toString(), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text(if (medicationCount == 1) "Medication" else "Medications", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Box(Modifier.padding(vertical = 10.dp).fillMaxWidth().height(1.dp).background(Color(0xFFD9C6FC)))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(Res.drawable.status_check), null, Modifier.size(36.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            if (totalToday == 0) "No doses" else if (allDone) "All Taken" else "$takenToday of $totalToday",
                            color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            if (totalToday == 0) "for today" else if (allDone) "Keep it up!" else "taken today",
                            color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, top: Dp) {
    Text(title, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = top, bottom = 8.dp))
}

@Composable
private fun NextMedicationCard(name: String, dose: String, time: String, onTake: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFFFF7DF), Color(0xFFFFF8E9))))
    ) {
        Image(
            painterResource(Res.drawable.medication_mascot), null,
            Modifier.align(Alignment.BottomEnd).width(166.dp).height(173.dp), contentScale = ContentScale.Fit
        )
        Column(Modifier.fillMaxSize().padding(start = 20.dp, top = 18.dp, end = 175.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(Res.drawable.clock_yellow), null, Modifier.size(47.dp))
                Spacer(Modifier.width(11.dp))
                Column {
                    Text(time, color = Ink, fontSize = 25.sp, lineHeight = 27.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Today", color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            Text(name, color = Ink, fontSize = 22.sp, lineHeight = 25.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 58.dp, top = 6.dp), maxLines = 1)
            Row(Modifier.padding(start = 58.dp, top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Medication, null, tint = Blue, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(dose, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = onTake,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(37.dp),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Ink),
                contentPadding = PaddingValues(0.dp)
            ) { Text("Take Now", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }
        }
    }
}

@Composable
private fun AllDoneCard(hasDoses: Boolean) {
    Box(
        Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFE7FBF1), Color(0xFFEFFCF6))))
    ) {
        Image(
            painterResource(Res.drawable.medication_mascot), null,
            Modifier.align(Alignment.BottomEnd).width(166.dp).height(173.dp), contentScale = ContentScale.Fit
        )
        Column(Modifier.fillMaxSize().padding(start = 20.dp, top = 26.dp, end = 175.dp, bottom = 16.dp)) {
            Image(painterResource(Res.drawable.status_check), null, Modifier.size(52.dp))
            Text(
                if (hasDoses) "All caught up!" else "Nothing due",
                color = Ink, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 10.dp), maxLines = 1
            )
            Text(
                if (hasDoses) "You’ve taken every dose\nscheduled for today. 🎉" else "No medications are\nscheduled for today.",
                color = Muted, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun EmptyReminders() {
    Box(
        Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF4F5FA)),
        contentAlignment = Alignment.Center
    ) {
        Text("No doses scheduled for today", color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AddMedicationButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(52.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF7529EF), Color(0xFF6324E2)))),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(Res.drawable.add_circle), null, Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Text("Add Medication", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OffScheduleDialog(confirm: OffScheduleConfirm, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val when0 = if (confirm.isLate) "after" else "before"
    val scheduled = confirm.dose.dose.scheduledAt.formatClockTime()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Image(painterResource(Res.drawable.clock_yellow), null, Modifier.size(40.dp)) },
        title = { Text("Not the scheduled time", color = Ink, fontWeight = FontWeight.ExtraBold) },
        text = {
            Text(
                "${confirm.dose.medication.displayName} is scheduled for $scheduled — that's " +
                    "${formatOffset(confirm.absMinutes)} $when0 now. Log it as taken anyway?",
                color = Muted, fontSize = 14.sp, lineHeight = 20.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Log anyway", color = Purple, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Muted, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White
    )
}

/** "45 minutes", "2 hours", "1 hour 20 minutes". */
private fun formatOffset(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    val parts = buildList {
        if (h > 0) add("$h hour${if (h == 1L) "" else "s"}")
        if (m > 0) add("$m minute${if (m == 1L) "" else "s"}")
    }
    return if (parts.isEmpty()) "0 minutes" else parts.joinToString(" ")
}

private data class HomeDose(
    val id: Long,
    val medicationId: Long,
    val name: String, val dose: String, val time: String,
    val status: DoseStatus,
    val medicationIcon: DrawableResource, val clockIcon: DrawableResource, val accent: Color
)

private data class SwipeAction(val label: String, val icon: ImageVector, val tint: Color, val onClick: () -> Unit)

private val RowHeight = 63.dp
private val RowShape = RoundedCornerShape(18.dp)
private val ActionWidth = 74.dp

/**
 * A reminder row that reveals actions on a left swipe (Undo/Edit/Archive) and, for a pending dose,
 * logs it as taken on a right swipe. The visible row sits opaque on top of both backdrops.
 */
@Composable
private fun SwipeableReminderRow(
    row: HomeDose,
    onTake: () -> Unit,
    onUndo: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit
) {
    val canTake = row.status == DoseStatus.PENDING
    val actions = buildList {
        if (!canTake) add(SwipeAction("Undo", Icons.Filled.Undo, Blue, onUndo))
        add(SwipeAction("Edit", Icons.Filled.Edit, Purple, onEdit))
        add(SwipeAction("Archive", Icons.Filled.Archive, Pink, onArchive))
    }

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val actionsWidthPx = with(density) { (ActionWidth * actions.size).toPx() }
    val maxRightPx = if (canTake) with(density) { 132.dp.toPx() } else 0f
    val takeTriggerPx = with(density) { 108.dp.toPx() }

    fun close() = scope.launch { offsetX.animateTo(0f) }
    fun runAction(action: () -> Unit) {
        action()
        close()
    }

    Box(Modifier.fillMaxWidth().height(RowHeight).clip(RowShape)) {
        // Backdrop shown while swiping right to take.
        if (offsetX.value > 0f) {
            Row(
                Modifier.matchParentSize().background(Green.copy(alpha = 0.16f)).padding(start = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(painterResource(Res.drawable.status_check), null, Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text("Mark as taken", color = Green, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        // Action backdrop revealed by a left swipe; buttons sit under the row's right edge.
        Row(Modifier.matchParentSize(), horizontalArrangement = Arrangement.End) {
            actions.forEach { action -> SwipeActionButton(action) { runAction(action.onClick) } }
        }
        // The opaque row itself, translated by the drag.
        Box(
            Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            offsetX.snapTo((offsetX.value + delta).coerceIn(-actionsWidthPx, maxRightPx))
                        }
                    },
                    onDragStopped = {
                        when {
                            canTake && offsetX.value >= takeTriggerPx -> {
                                onTake()
                                offsetX.animateTo(0f)
                            }
                            offsetX.value <= -actionsWidthPx / 2f -> offsetX.animateTo(-actionsWidthPx)
                            else -> offsetX.animateTo(0f)
                        }
                    }
                )
        ) {
            ReminderRow(row, onTake)
        }
    }
}

@Composable
private fun SwipeActionButton(action: SwipeAction, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxHeight().width(ActionWidth)
            .background(action.tint.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(action.icon, action.label, tint = action.tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(3.dp))
        Text(action.label, color = action.tint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReminderRow(row: HomeDose, onTake: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(RowHeight).clip(RowShape)
            .background(Color.White)
            .background(Brush.horizontalGradient(listOf(row.accent.copy(alpha = 0.10f), row.accent.copy(alpha = 0.045f))))
            .padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(row.medicationIcon), null, Modifier.size(42.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(row.name, color = Ink, fontSize = 16.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text(row.dose, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Row(Modifier.width(96.dp), verticalAlignment = Alignment.Top) {
            Image(painterResource(row.clockIcon), null, Modifier.size(18.dp))
            Spacer(Modifier.width(5.dp))
            Column {
                Text(row.time, color = Ink, fontSize = 15.sp, lineHeight = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text(row.status.label, color = row.status.accentColor(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.width(8.dp))
        DoseStatusControl(row.status, onTake)
    }
}

@Composable
private fun DoseStatusControl(status: DoseStatus, onTake: () -> Unit) {
    when (status) {
        DoseStatus.TAKEN -> Image(painterResource(Res.drawable.status_check), "Taken", Modifier.size(33.dp))
        DoseStatus.PENDING -> Box(
            Modifier.size(33.dp).clip(CircleShape)
                .background(Green.copy(alpha = 0.12f))
                .border(2.dp, Green, CircleShape)
                .clickable(onClick = onTake),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", color = Green, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
        DoseStatus.SKIPPED, DoseStatus.MISSED -> Box(
            Modifier.size(33.dp).clip(CircleShape).background(status.accentColor().copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (status == DoseStatus.SKIPPED) "–" else "!", color = status.accentColor(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private fun DoseWithMedication.toHomeDose(index: Int): HomeDose {
    val style = when (index % 3) {
        0 -> Triple(Res.drawable.medication_blue, Res.drawable.clock_blue, Blue)
        1 -> Triple(Res.drawable.medication_green, Res.drawable.clock_green, Green)
        else -> Triple(Res.drawable.medication_pink, Res.drawable.clock_pink, Pink)
    }
    return HomeDose(
        id = dose.id,
        medicationId = medication.id,
        name = medication.displayName,
        dose = dose.quantity.doseLabel(medication.form.unitLabel),
        time = dose.scheduledAt.formatClockTime(),
        status = dose.status,
        medicationIcon = style.first,
        clockIcon = style.second,
        accent = style.third
    )
}

private fun kotlinx.datetime.LocalDateTime.formatClockTime(): String {
    val hour = time.hour
    val displayHour = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }
    return "$displayHour:${time.minute.toString().padStart(2, '0')} ${if (hour >= 12) "PM" else "AM"}"
}

internal fun DoseStatus.accentColor(): Color = when (this) {
    DoseStatus.TAKEN -> TakenGreen
    DoseStatus.SKIPPED -> SkippedAmber
    DoseStatus.MISSED -> MissedRose
    DoseStatus.PENDING -> BrandPrimary
}

internal fun Double.doseLabel(unit: String): String {
    val amount = if (this % 1.0 == 0.0) toLong().toString() else toString()
    val plural = if (this == 1.0) unit else "${unit}s"
    return "$amount ${plural.replaceFirstChar { it.uppercase() }}"
}
