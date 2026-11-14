package io.wiger.timeup.ui.settings

import android.Manifest
import android.os.Build
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.wiger.timeup.R
import io.wiger.timeup.core.AppSettings
import io.wiger.timeup.core.AppUtil
import io.wiger.timeup.core.ApplicationListUtil
import io.wiger.timeup.notification.NotificationUtil
import io.wiger.timeup.ui.component.PermissionDialog
import io.wiger.timeup.ui.component.TASwitch
import io.wiger.timeup.ui.theme.ThemeMode

@Composable
fun SettingDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = shapes.extraLarge,
            color = colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings)) },
                    actions = {
                        FilledTonalIconButton(
                            onClick = onDismiss,
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(
                                painterResource(R.drawable.close),
                                contentDescription = stringResource(R.string.close)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )

                SettingDialogContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
fun SettingDialogContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val powerManager = ContextCompat.getSystemService(context, PowerManager::class.java)
    val themeMode by AppSettings.themeMode.collectAsStateWithLifecycle()
    val isLiveUpdatesSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
    val itemCount = if (isLiveUpdatesSupported) 7 else 6

    var hasNotificationPermission by remember {
        mutableStateOf(NotificationUtil.hasPostNotificationPermission(context))
    }
    var hasAppListPermission by remember {
        mutableStateOf(ApplicationListUtil.hasGetInstallAppPermission(context))
    }
    var isIgnoringBatteryOptimizations by rememberSaveable { mutableStateOf(false) }
    var isLiveNotificationEnabled by rememberSaveable {
        mutableStateOf(NotificationUtil.isLiveNotificationEnabled(context))
    }
    var pendingLiveEnable by rememberSaveable { mutableStateOf(false) }

    var showLiveNotificationPermissionDialog by remember { mutableStateOf(false) }
    var showAppListGoToSettingsDialog by remember { mutableStateOf(false) }
    var showBackgroundProtectionDialog by remember { mutableStateOf(false) }
    var showAppLanguageDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        hasNotificationPermission = NotificationUtil.hasPostNotificationPermission(context)
        hasAppListPermission = ApplicationListUtil.hasGetInstallAppPermission(context)
        isIgnoringBatteryOptimizations =
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false

        if (pendingLiveEnable) {
            val canEnable = NotificationUtil.canPostPromotedNotifications(context) &&
                NotificationUtil.hasPostNotificationPermission(context)
            if (canEnable) {
                NotificationUtil.setLiveNotificationEnabled(context, true)
            }
            pendingLiveEnable = false
        }
        isLiveNotificationEnabled = NotificationUtil.isLiveNotificationEnabled(context)
        onPauseOrDispose { }
    }

    val requestNotificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        isLiveNotificationEnabled = NotificationUtil.isLiveNotificationEnabled(context)
    }

    val requestAppListPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            hasAppListPermission = true
        } else {
            showAppListGoToSettingsDialog = true
        }
    }

    if (showLiveNotificationPermissionDialog) {
        PermissionDialog(
            title = stringResource(R.string.live_notification_permission_title),
            description = stringResource(R.string.live_notification_permission_desc),
            confirmText = stringResource(R.string.action_go_to_setting),
            onConfirm = {
                showLiveNotificationPermissionDialog = false
                pendingLiveEnable = true
                AppUtil.toLiveUpdateSettingPage(context)
            },
            onDismiss = {
                showLiveNotificationPermissionDialog = false
            }
        )
    }

    if (showAppListGoToSettingsDialog) {
        PermissionDialog(
            title = stringResource(R.string.app_list_permission_title),
            description = stringResource(R.string.deny_get_install_app_permission_description),
            confirmText = stringResource(R.string.action_go_to_setting),
            onConfirm = {
                showAppListGoToSettingsDialog = false
                AppUtil.toPermissionSettingPage(context, ApplicationListUtil.GET_INSTALLED_APPS)
            },
            onDismiss = { showAppListGoToSettingsDialog = false }
        )
    }

    if (showBackgroundProtectionDialog) {
        AlertDialog(
            onDismissRequest = { showBackgroundProtectionDialog = false },
            title = { Text(stringResource(R.string.background_guard_dialog_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(stringResource(R.string.background_guard_dialog_desc))

                    TextButton(
                        onClick = {
                            showBackgroundProtectionDialog = false
                            AppUtil.toBatteryUsageDetailPage(context)
                        },
                        shapes = ButtonDefaults.shapes(),
                        contentPadding = ButtonDefaults.SmallContentPadding,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.background_guard_open_battery),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    TextButton(
                        onClick = {
                            showBackgroundProtectionDialog = false
                            AppUtil.toAppDetailsPage(context)
                        },
                        shapes = ButtonDefaults.shapes(),
                        contentPadding = ButtonDefaults.SmallContentPadding,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.background_guard_open_app_settings),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    TextButton(
                        onClick = {
                            showBackgroundProtectionDialog = false
                            AppUtil.openUrl(context, AppUtil.DONT_KILL_MY_APP_URL)
                        },
                        shapes = ButtonDefaults.shapes(),
                        contentPadding = ButtonDefaults.SmallContentPadding,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.background_guard_open_website),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showBackgroundProtectionDialog = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showAppLanguageDialog) {
        val currentLanguageTag = AppUtil.getAppLanguageTag()
        AlertDialog(
            onDismissRequest = { showAppLanguageDialog = false },
            title = { Text(stringResource(R.string.app_language_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LanguageOption(
                        label = stringResource(R.string.language_system_default),
                        selected = currentLanguageTag.isNullOrBlank(),
                        onClick = {
                            showAppLanguageDialog = false
                            AppUtil.setAppLanguage("")
                        }
                    )
                    LanguageOption(
                        label = stringResource(R.string.language_english),
                        selected = currentLanguageTag?.startsWith("en") == true,
                        onClick = {
                            showAppLanguageDialog = false
                            AppUtil.setAppLanguage("en")
                        }
                    )
                    LanguageOption(
                        label = stringResource(R.string.language_chinese),
                        selected = currentLanguageTag?.startsWith("zh") == true,
                        onClick = {
                            showAppLanguageDialog = false
                            AppUtil.setAppLanguage("zh")
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAppLanguageDialog = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showThemeModeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeModeDialog = false },
            title = { Text(stringResource(R.string.theme_mode_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LanguageOption(
                        label = stringResource(R.string.theme_light),
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = {
                            showThemeModeDialog = false
                            AppSettings.setThemeMode(ThemeMode.LIGHT)
                        }
                    )
                    LanguageOption(
                        label = stringResource(R.string.theme_dark),
                        selected = themeMode == ThemeMode.DARK,
                        onClick = {
                            showThemeModeDialog = false
                            AppSettings.setThemeMode(ThemeMode.DARK)
                        }
                    )
                    LanguageOption(
                        label = stringResource(R.string.theme_auto),
                        selected = themeMode == ThemeMode.AUTO,
                        onClick = {
                            showThemeModeDialog = false
                            AppSettings.setThemeMode(ThemeMode.AUTO)
                        }
                    )
                    LanguageOption(
                        label = stringResource(R.string.theme_material_you),
                        selected = themeMode == ThemeMode.MATERIAL_YOU,
                        onClick = {
                            showThemeModeDialog = false
                            AppSettings.setThemeMode(ThemeMode.MATERIAL_YOU)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showThemeModeDialog = false },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        // Notification Permission Item
        SegmentedListItem(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            shapes = ListItemDefaults.segmentedShapes(index = 0, count = itemCount),
            content = { Text(stringResource(R.string.notification_permission_title)) },
            supportingContent = { Text(stringResource(R.string.notification_permission_desc)) },
            trailingContent = {
                if (hasNotificationPermission) {
                    Text(
                        text = stringResource(R.string.status_granted),
                        color = colorScheme.primary,
                        style = typography.labelLarge
                    )
                } else {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        shapes = ButtonDefaults.shapes(),
                        contentPadding = ButtonDefaults.SmallContentPadding
                    ) {
                        Text(stringResource(R.string.action_grant))
                    }
                }
            },
            colors = ListItemDefaults.segmentedColors(containerColor = colorScheme.surfaceContainer)
        )

        // App List Permission Item
        SegmentedListItem(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            shapes = ListItemDefaults.segmentedShapes(index = 1, count = itemCount),
            content = { Text(stringResource(R.string.app_list_permission_title)) },
            supportingContent = { Text(stringResource(R.string.app_list_permission_desc)) },
            trailingContent = {
                if (hasAppListPermission) {
                    Text(
                        text = stringResource(R.string.status_granted),
                        color = colorScheme.primary,
                        style = typography.labelLarge
                    )
                } else {
                    Button(
                        onClick = {
                            requestAppListPermission.launch(ApplicationListUtil.GET_INSTALLED_APPS)
                        },
                        shapes = ButtonDefaults.shapes(),
                        contentPadding = ButtonDefaults.SmallContentPadding
                    ) {
                        Text(stringResource(R.string.action_grant))
                    }
                }
            },
            colors = ListItemDefaults.segmentedColors(containerColor = colorScheme.surfaceContainer)
        )

        // Battery Optimization Item
        SegmentedListItem(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            shapes = ListItemDefaults.segmentedShapes(index = 2, count = itemCount),
            content = { Text(stringResource(R.string.battery_optimization_title)) },
            supportingContent = { Text(stringResource(R.string.battery_optimization_desc)) },
            trailingContent = {
                if (isIgnoringBatteryOptimizations) {
                    Text(
                        text = stringResource(R.string.status_enabled),
                        color = colorScheme.primary,
                        style = typography.labelLarge
                    )
                } else {
                    Button(
                        onClick = { AppUtil.toIgnoreBatteryOptimizationsSetting(context) },
                        shapes = ButtonDefaults.shapes(),
                        contentPadding = ButtonDefaults.SmallContentPadding
                    ) {
                        Text(stringResource(R.string.action_go_to_setting))
                    }
                }
            },
            colors = ListItemDefaults.segmentedColors(containerColor = colorScheme.surfaceContainer)
        )

        // Background Protection Item
        SegmentedListItem(
            onClick = { showBackgroundProtectionDialog = true },
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            shapes = ListItemDefaults.segmentedShapes(index = 3, count = itemCount),
            content = { Text(stringResource(R.string.background_guard_title)) },
            supportingContent = { Text(stringResource(R.string.background_guard_desc)) },
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.arrow_right),
                    contentDescription = null
                )
            },
            colors = ListItemDefaults.segmentedColors(containerColor = colorScheme.surfaceContainer)
        )

        // App Language Item
        SegmentedListItem(
            onClick = { showAppLanguageDialog = true },
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            shapes = ListItemDefaults.segmentedShapes(index = 4, count = itemCount),
            content = { Text(stringResource(R.string.app_language_title)) },
            supportingContent = { Text(stringResource(R.string.app_language_desc)) },
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.arrow_right),
                    contentDescription = null
                )
            },
            colors = ListItemDefaults.segmentedColors(containerColor = colorScheme.surfaceContainer)
        )

        // Appearance Item
        SegmentedListItem(
            onClick = { showThemeModeDialog = true },
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            shapes = ListItemDefaults.segmentedShapes(index = 5, count = itemCount),
            content = { Text(stringResource(R.string.theme_mode_title)) },
            supportingContent = { Text(stringResource(R.string.theme_mode_desc)) },
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.arrow_right),
                    contentDescription = null
                )
            },
            colors = ListItemDefaults.segmentedColors(containerColor = colorScheme.surfaceContainer)
        )

        // Android 16+ Live Updates Switch Item
        if (isLiveUpdatesSupported) {
            SegmentedListItem(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                shapes = ListItemDefaults.segmentedShapes(index = 6, count = itemCount),
                content = { Text(stringResource(R.string.live_notification_title)) },
                supportingContent = {
                    Text(stringResource(R.string.live_notification_desc))
                },
                trailingContent = {
                    TASwitch(
                        checked = isLiveNotificationEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                val canEnable =
                                    NotificationUtil.canPostPromotedNotifications(context) &&
                                        NotificationUtil.hasPostNotificationPermission(context)
                                if (canEnable) {
                                    isLiveNotificationEnabled = true
                                    NotificationUtil.setLiveNotificationEnabled(context, true)
                                } else {
                                    showLiveNotificationPermissionDialog = true
                                }
                            } else {
                                isLiveNotificationEnabled = false
                                NotificationUtil.setLiveNotificationEnabled(context, false)
                            }
                        }
                    )
                },
                colors = ListItemDefaults.segmentedColors(
                    containerColor = colorScheme.surfaceContainer
                )
            )
        }
    }
}

@Composable
private fun LanguageOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        onClick = onClick,
        leadingContent = {
            RadioButton(
                selected = selected,
                onClick = null
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        content = { Text(label) }
    )
}
