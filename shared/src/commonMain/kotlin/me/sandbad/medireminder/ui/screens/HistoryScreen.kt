package me.sandbad.medireminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.sandbad.medireminder.core.formatHm
import me.sandbad.medireminder.ui.components.EmptyState
import me.sandbad.medireminder.ui.components.MedicationAvatar
import me.sandbad.medireminder.ui.components.RingProgress
import me.sandbad.medireminder.ui.components.SelectableChip
import me.sandbad.medireminder.ui.components.StatCard
import me.sandbad.medireminder.ui.theme.MissedRose
import me.sandbad.medireminder.ui.theme.SkippedAmber
import me.sandbad.medireminder.ui.theme.TakenGreen
import me.sandbad.medireminder.ui.theme.TextSecondary
import me.sandbad.medireminder.ui.viewmodel.HistoryRange
import me.sandbad.medireminder.ui.viewmodel.HistoryViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HistoryScreen(onBack: (() -> Unit)? = null) {
    val viewModel = koinViewModel<HistoryViewModel>()
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("History") },
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryRange.entries.forEach { range ->
                        SelectableChip(
                            label = range.label,
                            selected = state.range == range,
                            onClick = { viewModel.setRange(range) }
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        RingProgress(
                            progress = state.stats.rate,
                            modifier = Modifier.size(88.dp),
                            strokeWidth = 10.dp
                        ) {
                            Text(
                                "${(state.stats.rate * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Adherence", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${state.stats.taken} of ${state.stats.scored} doses taken",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                if (state.streakDays > 0) "${state.streakDays} day streak" else "No streak yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard("Taken", state.stats.taken.toString(), Icons.Filled.Check, TakenGreen, Modifier.weight(1f))
                    StatCard("Skipped", state.stats.skipped.toString(), Icons.Filled.Redo, SkippedAmber, Modifier.weight(1f))
                    StatCard("Missed", state.stats.missed.toString(), Icons.Filled.Warning, MissedRose, Modifier.weight(1f))
                }
            }

            if (state.dailyRates.isNotEmpty()) {
                item { AdherenceBars(state.dailyRates.map { it.second }) }
            }

            if (!state.isLoading && state.entriesByDate.isEmpty()) {
                item {
                    EmptyState(
                        title = "No history yet",
                        message = "Once you start logging doses they will show up here."
                    )
                }
            }

            state.entriesByDate.forEach { (date, doses) ->
                item(key = "header-$date") {
                    Text(
                        date.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
                items(doses.size, key = { index -> doses[index].dose.id }) { index ->
                    val entry = doses[index]
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MedicationAvatar(entry.medication.color, entry.medication.form, size = 34.dp)
                        Column(Modifier.weight(1f)) {
                            Text(entry.medication.displayName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                entry.dose.scheduledAt.formatHm(),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Text(
                            entry.dose.status.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = entry.dose.status.accentColor(),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/** Compact per-day adherence bars — enough signal without pulling in a chart library. */
@Composable
private fun AdherenceBars(rates: List<Float>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Daily adherence", style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth().height(72.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                rates.forEach { rate ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(rate.coerceIn(0.04f, 1f))
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (rate >= 0.8f) TakenGreen
                                else if (rate >= 0.5f) SkippedAmber
                                else MissedRose
                            )
                    )
                }
            }
        }
    }
}
