package io.wiger.timeup.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.MediumFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wiger.timeup.R
import io.wiger.timeup.core.AppUtil
import io.wiger.timeup.data.model.TimeUp
import io.wiger.timeup.notification.NotificationUtil
import io.wiger.timeup.ui.addalert.AddAlertBottomSheet
import io.wiger.timeup.ui.addalert.AddAlertViewModel
import io.wiger.timeup.ui.component.PermissionDialog
import io.wiger.timeup.ui.component.TASwitch
import io.wiger.timeup.ui.settings.SettingDialog
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.ceil
import kotlin.math.roundToLong

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var showNotificationGoToSettingsDialog by remember { mutableStateOf(false) }
    var showSettingDialog by remember { mutableStateOf(false) }
    var showAddAlertSheet by remember { mutableStateOf(false) }
    var editingTimeUp by remember { mutableStateOf<TimeUp?>(null) }

    val requestNotificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showNotificationGoToSettingsDialog = true
        }
    }

    LifecycleStartEffect(Unit) {
        viewModel.restoreRunningAlerts()
        if (!NotificationUtil.hasPostNotificationPermission(context)) {
            showNotificationPermissionDialog = true
        }
        onStopOrDispose { }
    }

    if (showNotificationPermissionDialog) {
        PermissionDialog(
            title = stringResource(R.string.notification_permission_title),
            description = stringResource(R.string.notification_permission_desc),
            onConfirm = {
                showNotificationPermissionDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismiss = { showNotificationPermissionDialog = false }
        )
    }

    if (showNotificationGoToSettingsDialog) {
        PermissionDialog(
            title = stringResource(R.string.notification_permission_title),
            description = stringResource(R.string.notification_permission_desc),
            confirmText = stringResource(R.string.action_go_to_setting),
            onConfirm = {
                showNotificationGoToSettingsDialog = false
                AppUtil.toNotificationSettingPage(context)
            },
            onDismiss = { showNotificationGoToSettingsDialog = false }
        )
    }

    if (showSettingDialog) {
        SettingDialog(onDismiss = { showSettingDialog = false })
    }

    if (showAddAlertSheet) {
        val addAlertViewModel: AddAlertViewModel = viewModel()
        AddAlertBottomSheet(
            viewModel = addAlertViewModel,
            editingTimeUp = editingTimeUp,
            onDismiss = {
                showAddAlertSheet = false
                editingTimeUp = null
            }
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val pendingDelete = state.pendingDelete
    val untitledCountdown = stringResource(R.string.untitled_countdown)
    val pendingDeleteName = pendingDelete?.alertName
        ?.takeUnless(String::isBlank)
        ?: untitledCountdown
    val pendingDeleteMessage = stringResource(
        R.string.countdown_delete_pending,
        pendingDeleteName
    )
    val undoLabel = stringResource(R.string.undo)

    LaunchedEffect(pendingDelete?.id, pendingDeleteMessage, undoLabel) {
        if (pendingDelete == null) return@LaunchedEffect
        when (
            snackbarHostState.showSnackbar(
                message = pendingDeleteMessage,
                actionLabel = undoLabel,
                withDismissAction = false,
                duration = SnackbarDuration.Indefinite,
            )
        ) {
            SnackbarResult.ActionPerformed -> viewModel.undoPendingDelete()
            SnackbarResult.Dismissed -> viewModel.commitPendingDelete()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackbarData ->
                CountdownDeleteSnackbar(snackbarData)
            }
        },
        floatingActionButton = {
            MediumFloatingActionButton(
                onClick = {
                    editingTimeUp = null
                    showAddAlertSheet = true
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = stringResource(R.string.add_alert)
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        HomeContent(
            timeUps = state.timeUps,
            onSettingsClick = { showSettingDialog = true },
            onToggleTimeUp = { timeUp, isEnabled ->
                viewModel.toggleTimeUp(context, timeUp, isEnabled)
            },
            onRemoveTimeUp = viewModel::requestDelete,
            onEditTimeUp = { timeUp ->
                if (timeUp.isEnabled) {
                    Toast.makeText(context, R.string.cannot_edit_running, Toast.LENGTH_SHORT).show()
                } else {
                    editingTimeUp = timeUp
                    showAddAlertSheet = true
                }
            },
            isDeletionPending = state.pendingDelete != null,
            contentPadding = innerPadding
        )
    }
}

@Composable
fun HomeContent(
    timeUps: ImmutableList<TimeUp>,
    onSettingsClick: () -> Unit,
    onToggleTimeUp: (TimeUp, Boolean) -> Unit,
    onRemoveTimeUp: (TimeUp) -> Unit,
    onEditTimeUp: (TimeUp) -> Unit,
    isDeletionPending: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val deleteLabel = stringResource(R.string.delete)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.surfaceContainer)
            .padding(contentPadding)
            .consumeWindowInsets(contentPadding),
    ) {
        HomeHeader(
            countdownCount = timeUps.size,
            onSettingsClick = onSettingsClick,
        )

        if (timeUps.isEmpty()) {
            EmptyContent(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    // Leave room for the floating action button.
                    bottom = 112.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(timeUps, key = { it.id ?: 0L }) { timeUp ->
                    val dismissState = rememberSwipeToDismissBoxState()
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = dismissState.progress }
                                    .clip(ListItemDefaults.shapes().shape)
                                    .background(colorScheme.error),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.delete),
                                    contentDescription = stringResource(R.string.delete),
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(end = 24.dp)
                                        .size(24.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        gesturesEnabled = !isDeletionPending,
                        onDismiss = { onRemoveTimeUp(timeUp) }
                    ) {
                        TimeUpListItem(
                            timeUp = timeUp,
                            packageManager = packageManager,
                            onToggleTimeUp = onToggleTimeUp,
                            onEditTimeUp = onEditTimeUp,
                            modifier = Modifier.semantics {
                                customActions = listOf(
                                    CustomAccessibilityAction(deleteLabel) {
                                        onRemoveTimeUp(timeUp)
                                        true
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    countdownCount: Int,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = typography.displayLargeEmphasized,
                color = colorScheme.onSurface
            )

            val countText = if (countdownCount == 0) {
                stringResource(R.string.no_countdown)
            } else {
                pluralStringResource(
                    R.plurals.countdown_count,
                    countdownCount,
                    countdownCount
                )
            }
            Text(
                text = countText,
                style = typography.bodyLarge,
                color = colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        FilledTonalIconButton(
            onClick = onSettingsClick,
            shapes = IconButtonDefaults.shapes()
        ) {
            Icon(
                painterResource(R.drawable.settings),
                contentDescription = stringResource(R.string.settings)
            )
        }
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.alarm),
            contentDescription = null,
            tint = colorScheme.outline,
            modifier = Modifier.size(56.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.add_new_countdown),
            style = typography.titleMedium,
            color = colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.add_countdown_description),
            style = typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TimeUpListItem(
    timeUp: TimeUp,
    packageManager: PackageManager,
    onToggleTimeUp: (TimeUp, Boolean) -> Unit,
    onEditTimeUp: (TimeUp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appNameText = timeUp.appName?.let { pkg ->
        runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        }.getOrNull()
    }

    ListItem(
        onClick = { onEditTimeUp(timeUp) },
        modifier = modifier.fillMaxWidth(),
        leadingContent = {
            val appIcon = timeUp.appName?.let { pkg ->
                runCatching {
                    packageManager.getApplicationIcon(pkg).toBitmapOrNull()
                }.getOrNull()
            }
            if (appIcon != null) {
                Image(
                    bitmap = appIcon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.alarm),
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        supportingContent = {
            val timeText = DateUtils.formatElapsedTime((timeUp.alertTime / 1000f).roundToLong())
            val detail = if (appNameText != null) "$timeText • $appNameText" else timeText
            Text(detail)
        },
        trailingContent = {
            TASwitch(
                checked = timeUp.isEnabled,
                onCheckedChange = { isChecked ->
                    onToggleTimeUp(timeUp, isChecked)
                }
            )
        },
        shapes = ListItemDefaults.shapes(),
        colors = ListItemDefaults.colors(
            containerColor = colorScheme.surfaceContainerHigh
        ),
        content = {
            val displayName = timeUp.alertName.ifBlank { stringResource(R.string.untitled_countdown) }
            Text(
                text = displayName,
                style = typography.titleMedium
            )
        }
    )
}

@Composable
private fun CountdownDeleteSnackbar(snackbarData: SnackbarData) {
    val progress = remember(snackbarData) { Animatable(1f) }
    val totalSeconds = (HomeViewModel.DELETE_GRACE_MILLIS / 1_000L).toInt().coerceAtLeast(1)
    val secondsLeft by remember(snackbarData) {
        derivedStateOf {
            ceil(progress.value * totalSeconds)
                .toInt()
                .coerceIn(1, totalSeconds)
        }
    }

    LaunchedEffect(snackbarData) {
        progress.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = HomeViewModel.DELETE_GRACE_MILLIS.toInt(),
                easing = LinearEasing
            )
        )
    }

    Snackbar(
        modifier = Modifier.padding(12.dp),
        action = {
            TextButton(
                onClick = { snackbarData.performAction() },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorScheme.inversePrimary
                )
            ) {
                Text(snackbarData.visuals.actionLabel.orEmpty())
            }
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progress.value },
                            modifier = Modifier.fillMaxSize(),
                            color = colorScheme.inversePrimary,
                            trackColor = colorScheme.inverseOnSurface.copy(alpha = 0.24f),
                            strokeWidth = 2.5.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            text = secondsLeft.toString(),
                            style = typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = snackbarData.visuals.message,
                        style = typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}
