package me.sandbad.medireminder.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.sandbad.medireminder.core.model.DoseStatus
import me.sandbad.medireminder.core.model.DoseWithMedication
import me.sandbad.medireminder.resources.*
import me.sandbad.medireminder.ui.theme.BrandPrimary
import me.sandbad.medireminder.ui.theme.MissedRose
import me.sandbad.medireminder.ui.theme.SkippedAmber
import me.sandbad.medireminder.ui.theme.TakenGreen
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
fun TodayScreen(onAddMedication: () -> Unit, onOpenHistory: () -> Unit) {
    val viewModel = koinViewModel<TodayViewModel>()
    val state by viewModel.state.collectAsState()
    val nextDose = state.upcoming.firstOrNull()
    val rows = if (state.doses.isEmpty()) previewDoses else state.doses.take(3).mapIndexed { i, dose -> dose.toHomeDose(i) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 14.dp)
    ) {
        item { BrandHeader() }
        item {
            StreakCard(
                streak = state.streakDays.takeIf { it > 0 } ?: 12,
                medicationCount = state.doses.map { it.medication.id }.distinct().size.takeIf { it > 0 } ?: 3,
                onClick = onOpenHistory
            )
        }
        item { SectionTitle("Next Medication", 16.dp) }
        item {
            NextMedicationCard(
                name = nextDose?.medication?.displayName ?: "Vitamin D",
                dose = nextDose?.let { it.dose.quantity.doseLabel(it.medication.form.unitLabel) } ?: "1 Tablet",
                time = nextDose?.dose?.scheduledAt?.formatClockTime() ?: "2:30 PM",
                onTake = { nextDose?.let { viewModel.markTaken(it.dose.id) } }
            )
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
        rows.forEachIndexed { index, row ->
            item {
                ReminderRow(row)
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
private fun StreakCard(streak: Int, medicationCount: Int, onClick: () -> Unit) {
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
                Text("Days", color = Purple, fontSize = 22.sp, lineHeight = 23.sp, fontWeight = FontWeight.ExtraBold)
                Text("Great job! 🎉\nYou’re building\nhealthy habits.", color = Ink, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 7.dp))
            }
            Box(Modifier.padding(top = 53.dp).width(1.dp).height(112.dp).background(Color(0xFFD9C6FC)))
            Column(Modifier.padding(start = 16.dp, top = 48.dp).widthIn(min = 94.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(Res.drawable.medication_purple), null, Modifier.size(36.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(medicationCount.toString(), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Medications", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Box(Modifier.padding(vertical = 10.dp).fillMaxWidth().height(1.dp).background(Color(0xFFD9C6FC)))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(Res.drawable.status_check), null, Modifier.size(36.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("All Taken", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Keep it up!", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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

private data class HomeDose(
    val name: String, val dose: String, val time: String,
    val medicationIcon: DrawableResource, val clockIcon: DrawableResource, val accent: Color
)

private val previewDoses = listOf(
    HomeDose("Omega 3", "1 Capsule", "8:00 AM", Res.drawable.medication_blue, Res.drawable.clock_blue, Blue),
    HomeDose("Vitamin D", "1 Tablet", "2:30 PM", Res.drawable.medication_green, Res.drawable.clock_green, Green),
    HomeDose("Calcium", "1 Tablet", "8:00 PM", Res.drawable.medication_pink, Res.drawable.clock_pink, Pink)
)

@Composable
private fun ReminderRow(row: HomeDose) {
    Row(
        Modifier.fillMaxWidth().height(63.dp).clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(row.accent.copy(alpha = 0.10f), row.accent.copy(alpha = 0.045f))))
            .padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(row.medicationIcon), null, Modifier.size(42.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(row.name, color = Ink, fontSize = 16.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text(row.dose, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Row(Modifier.width(112.dp), verticalAlignment = Alignment.Top) {
            Image(painterResource(row.clockIcon), null, Modifier.size(18.dp))
            Spacer(Modifier.width(5.dp))
            Column {
                Text(row.time, color = Ink, fontSize = 15.sp, lineHeight = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text("Everyday", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        Image(painterResource(Res.drawable.status_check), null, Modifier.size(33.dp))
    }
}

private fun DoseWithMedication.toHomeDose(index: Int): HomeDose {
    val style = when (index % 3) {
        0 -> Triple(Res.drawable.medication_blue, Res.drawable.clock_blue, Blue)
        1 -> Triple(Res.drawable.medication_green, Res.drawable.clock_green, Green)
        else -> Triple(Res.drawable.medication_pink, Res.drawable.clock_pink, Pink)
    }
    return HomeDose(medication.displayName, dose.quantity.doseLabel(medication.form.unitLabel), dose.scheduledAt.formatClockTime(), style.first, style.second, style.third)
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
