package me.sandbad.medireminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.sandbad.medireminder.ui.components.LabeledField
import me.sandbad.medireminder.ui.components.PrimaryButton
import me.sandbad.medireminder.ui.theme.TextSecondary
import me.sandbad.medireminder.ui.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel) {
    val state by viewModel.state.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Medication,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text("MediReminder", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(
            "Never miss a dose",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )

        Spacer(Modifier.height(32.dp))

        Feature(Icons.Filled.Schedule, "Flexible schedules", "Daily, specific weekdays, or every N days.")
        Feature(Icons.Filled.NotificationsActive, "Reminders that stick", "Take or snooze straight from the notification.")
        Feature(Icons.Filled.Inventory2, "Refill alerts", "Track stock so you reorder before you run out.")

        Spacer(Modifier.height(32.dp))

        LabeledField(
            label = "Who are these medications for?",
            value = state.ownerName,
            onValueChange = viewModel::setOwnerName,
            placeholder = "optional"
        )

        Spacer(Modifier.height(20.dp))

        PrimaryButton(
            text = "Get started",
            onClick = viewModel::complete,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, description: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Start
            )
        }
    }
}
