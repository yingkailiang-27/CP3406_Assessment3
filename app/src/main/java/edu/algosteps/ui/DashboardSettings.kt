package edu.algosteps.ui

import android.Manifest
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.algosteps.data.ReviewWorker

private fun notificationsAllowed(context: Context): Boolean {
    val service = requireNotNull(context.getSystemService(NotificationManager::class.java))
    if (Build.VERSION.SDK_INT >= 33 &&
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        return false
    }
    return service.areNotificationsEnabled() &&
        service.getNotificationChannel(ReviewWorker.CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
}

private fun notificationSettings(context: Context): Intent {
    val service = requireNotNull(context.getSystemService(NotificationManager::class.java))
    val channelBlocked = service.areNotificationsEnabled() &&
        service.getNotificationChannel(ReviewWorker.CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE
    return if (channelBlocked) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, ReviewWorker.CHANNEL_ID)
    } else {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
}

@Composable
fun SettingsScreen(viewModel: DashboardViewModel, onBack: () -> Unit) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var allowed by remember { mutableStateOf(notificationsAllowed(context)) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var optInPending by rememberSaveable { mutableStateOf(false) }
    var settingsUnavailable by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) allowed = notificationsAllowed(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    val openDeviceSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        allowed = notificationsAllowed(context)
        if (allowed && optInPending) viewModel.setReminders(true)
        optInPending = false
    }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = notificationsAllowed(context)
        if (granted && allowed && optInPending) {
            viewModel.setReminders(true)
            optInPending = false
        }
    }
    val showDeviceLink = !allowed && (attempted || prefs?.reminders == true)
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { inset ->
        val current = prefs
        if (current == null) {
            Column(Modifier.padding(inset).padding(20.dp)) {
                PageTitle("Settings", onBack)
                Text("Loading preferences…")
            }
        } else {
            SettingsContent(
                modifier = Modifier.padding(inset),
                showHints = current.showHints, reminders = current.reminders,
                showDeviceLink = showDeviceLink, settingsUnavailable = settingsUnavailable,
                onBack = onBack, onHints = viewModel::setHints,
                onReminders = { wanted ->
                    if (!wanted) {
                        optInPending = false
                        attempted = false
                        viewModel.setReminders(false)
                    } else {
                        allowed = notificationsAllowed(context)
                        optInPending = !allowed
                        if (allowed) {
                            viewModel.setReminders(true)
                        } else if (Build.VERSION.SDK_INT >= 33 && !attempted &&
                            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            attempted = true
                            askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            attempted = true
                        }
                    }
                },
                onDeviceSettings = {
                    try {
                        settingsUnavailable = false
                        optInPending = true
                        openDeviceSettings.launch(notificationSettings(context))
                    } catch (_: ActivityNotFoundException) {
                        optInPending = false
                        settingsUnavailable = true
                    }
                },
                onClear = viewModel::clearProgress
            )
        }
    }
}

@Composable
private fun Preference(
    name: String, detail: String, checked: Boolean, changed: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = changed)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
internal fun SettingsContent(
    modifier: Modifier = Modifier, showHints: Boolean, reminders: Boolean,
    showDeviceLink: Boolean = false, settingsUnavailable: Boolean = false,
    onBack: () -> Unit, onHints: (Boolean) -> Unit, onReminders: (Boolean) -> Unit,
    onDeviceSettings: () -> Unit, onClear: () -> Unit
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle("Settings", onBack) }
        item {
            Preference("Practice hints", "Show teaching hints while practising code.", showHints, onHints)
            HorizontalDivider()
            Preference("Review reminders", "A daily check can remind you about an attempted problem you have yet to solve.",
                reminders, onReminders)
            if (showDeviceLink) {
                Text("Device notifications are blocked. Allow them in system settings to receive reminders.")
                TextButton(onClick = onDeviceSettings) { Text("Open notification settings") }
            }
            if (settingsUnavailable) Text("Open your device’s Settings, then Apps, AlgoSteps and Notifications.")
        }
        item {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Learning data", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() })
                    Text("Your code drafts and results are saved on this device. Clearing progress removes both.")
                    Text("Online readings send the article title to Wikipedia. Your code and scores stay on this device.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { confirmClear = true }) { Text("Clear progress") }
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(onDismissRequest = { confirmClear = false },
            title = { Text("Clear learning progress?") },
            text = { Text("All saved submissions and code drafts on this device will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClear() }) { Text("Delete progress") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            })
    }
}
