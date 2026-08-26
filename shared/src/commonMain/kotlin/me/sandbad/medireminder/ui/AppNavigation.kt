package me.sandbad.medireminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import me.sandbad.medireminder.ui.screens.HistoryScreen
import me.sandbad.medireminder.ui.screens.MedicationEditScreen
import me.sandbad.medireminder.ui.screens.MedicationsScreen
import me.sandbad.medireminder.ui.screens.OnboardingScreen
import me.sandbad.medireminder.ui.screens.SettingsScreen
import me.sandbad.medireminder.ui.screens.TodayScreen
import me.sandbad.medireminder.ui.viewmodel.OnboardingViewModel
import me.sandbad.medireminder.resources.Res
import me.sandbad.medireminder.resources.nav_history
import me.sandbad.medireminder.resources.nav_home
import me.sandbad.medireminder.resources.nav_profile
import me.sandbad.medireminder.resources.nav_progress
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MediReminderApp() {
    val onboardingVm = koinViewModel<OnboardingViewModel>()
    val state by onboardingVm.state.collectAsState()

    when {
        state.isLoading -> Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        state.needsOnboarding -> OnboardingScreen(onboardingVm)

        else -> Navigator(MainScreen) { CurrentScreen() }
    }
}

private object TodayTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(0u, "Home", painterResource(Res.drawable.nav_home))

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow.let { it.parent ?: it }
        TodayScreen(
            onAddMedication = { navigator.push(MedicationEditVoyagerScreen(0L)) },
            onOpenHistory = { navigator.push(HistoryVoyagerScreen) }
        )
    }
}

private object MedicationsTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(2u, "Progress", painterResource(Res.drawable.nav_progress))

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow.let { it.parent ?: it }
        MedicationsScreen(
            onAdd = { navigator.push(MedicationEditVoyagerScreen(0L)) },
            onEdit = { id -> navigator.push(MedicationEditVoyagerScreen(id)) }
        )
    }
}

private object HistoryTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(1u, "History", painterResource(Res.drawable.nav_history))

    @Composable
    override fun Content() = HistoryScreen()
}

private object SettingsTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(3u, "Profile", painterResource(Res.drawable.nav_profile))

    @Composable
    override fun Content() = SettingsScreen()
}

private object MainScreen : Screen {
    @Composable
    override fun Content() {
        TabNavigator(TodayTab) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(containerColor = androidx.compose.ui.graphics.Color.White, tonalElevation = 0.dp) {
                        listOf(TodayTab, HistoryTab, MedicationsTab, SettingsTab).forEach { TabItem(it) }
                    }
                }
            ) { innerPadding ->
                Box(Modifier.padding(innerPadding)) { CurrentTab() }
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current
    NavigationBarItem(
        icon = { tab.options.icon?.let { Icon(painter = it, contentDescription = tab.options.title) } },
        label = { Text(tab.options.title, style = MaterialTheme.typography.labelSmall) },
        selected = tabNavigator.current == tab,
        onClick = { tabNavigator.current = tab },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = androidx.compose.ui.graphics.Color(0xFF7028EE),
            selectedTextColor = androidx.compose.ui.graphics.Color(0xFF7028EE),
            indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF8D92A7),
            unselectedTextColor = androidx.compose.ui.graphics.Color(0xFF8D92A7)
        )
    )
}

private data class MedicationEditVoyagerScreen(val medicationId: Long) : Screen {
    override val key: String get() = "medication-edit-$medicationId"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        MedicationEditScreen(medicationId = medicationId, onBack = { navigator.pop() })
    }
}

private object HistoryVoyagerScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        HistoryScreen(onBack = { navigator.pop() })
    }
}
