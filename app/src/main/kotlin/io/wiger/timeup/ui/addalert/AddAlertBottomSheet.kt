@file:OptIn(ExperimentalMaterial3Api::class)

package io.wiger.timeup.ui.addalert

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wiger.timeup.R
import io.wiger.timeup.core.AppUtil
import io.wiger.timeup.core.ApplicationListUtil
import io.wiger.timeup.data.model.TimeUp
import io.wiger.timeup.ui.addalert.components.TimeInputView
import io.wiger.timeup.ui.component.InputTextDialog
import io.wiger.timeup.ui.component.PermissionDialog
import io.wiger.timeup.ui.selectapp.SelectAppBottomSheet
import io.wiger.timeup.ui.selectapp.SelectAppViewModel
import io.wiger.timeup.ui.theme.EndItemShape
import io.wiger.timeup.ui.theme.MiddleItemShape
import io.wiger.timeup.ui.theme.StartItemShape

@Composable
fun AddAlertBottomSheet(
    viewModel: AddAlertViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    editingTimeUp: TimeUp? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier
    ) {
        LaunchedEffect(editingTimeUp?.id) {
            if (editingTimeUp != null) {
                viewModel.processIntent(AlertFormIntent.Edit(editingTimeUp))
            } else {
                viewModel.processIntent(AlertFormIntent.ResetForm)
            }
        }

        AddAlertSheetContent(
            viewModel = viewModel, close = onDismiss
        )
    }
}

@Composable
fun AddAlertSheetContent(
    viewModel: AddAlertViewModel,
    close: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showAppListPermissionDialog by remember { mutableStateOf(false) }
    var showGoToSettingsDialog by remember { mutableStateOf(false) }
    var showSelectAppSheet by remember { mutableStateOf(false) }
    var showNameDialog by rememberSaveable { mutableStateOf(false) }

    val requestAppListPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showSelectAppSheet = true
        } else {
            showGoToSettingsDialog = true
        }
    }

    val invalidDurationMsg = stringResource(R.string.invalid_duration_toast)
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                is AlertFormEvent.SubmitDone -> close()
                is AlertFormEvent.InvalidDuration -> {
                    Toast.makeText(context, invalidDurationMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showAppListPermissionDialog) {
        PermissionDialog(
            title = stringResource(R.string.app_list_permission_title),
            description = stringResource(R.string.app_list_permission_desc),
            onConfirm = {
                showAppListPermissionDialog = false
                requestAppListPermission.launch(ApplicationListUtil.GET_INSTALLED_APPS)
            },
            onDismiss = { showAppListPermissionDialog = false })
    }

    if (showGoToSettingsDialog) {
        PermissionDialog(
            title = stringResource(R.string.app_list_permission_title),
            description = stringResource(R.string.deny_get_install_app_permission_description),
            confirmText = stringResource(R.string.action_go_to_setting),
            onConfirm = {
                showGoToSettingsDialog = false
                AppUtil.toPermissionSettingPage(context, ApplicationListUtil.GET_INSTALLED_APPS)
            },
            onDismiss = { showGoToSettingsDialog = false })
    }

    if (showSelectAppSheet) {
        val appListViewModel: SelectAppViewModel = viewModel()
        SelectAppBottomSheet(
            viewModel = appListViewModel,
            onDismiss = { showSelectAppSheet = false },
            onSelectApp = { appInfo ->
                viewModel.processIntent(AlertFormIntent.UpdateSelectApp(appInfo))
                showSelectAppSheet = false
            })
    }

    if (showNameDialog) {
        InputTextDialog(
            title = stringResource(R.string.alert_name),
            text = state.alertName,
            hint = stringResource(R.string.alert_name_placeholder)
        ) { result ->
            result?.let {
                viewModel.processIntent(AlertFormIntent.UpdateAlertName(it))
            }
            showNameDialog = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = {
            Text(
                text = stringResource(
                    if (state.editingTimeUp != null) R.string.edit_alert else R.string.add_alert
                )
            )
        }, subtitle = {
            Text(
                text = stringResource(
                    if (state.editingTimeUp != null) R.string.edit_alert_subtitle
                    else R.string.add_alert_subtitle
                )
            )
        }, navigationIcon = {
            FilledTonalIconButton(
                onClick = close, shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.close)
                )
            }
        }, actions = {
            FilledTonalIconButton(
                onClick = { viewModel.processIntent(AlertFormIntent.Submit) },
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(
                    painterResource(R.drawable.check),
                    contentDescription = stringResource(R.string.confirm)
                )
            }
        }, colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .height(120.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                TimeInputView(
                    state = viewModel.hourTextFieldState,
                    title = stringResource(R.string.hour),
                    inputShape = StartItemShape,
                    max = 23,
                    modifier = Modifier.weight(1f)
                )

                TimeInputView(
                    state = viewModel.minuteTextFieldState,
                    title = stringResource(R.string.minute),
                    max = 59,
                    inputShape = MiddleItemShape,
                    modifier = Modifier.weight(1f)
                )

                TimeInputView(
                    state = viewModel.secondTextFieldState,
                    title = stringResource(R.string.second),
                    max = 59,
                    inputShape = EndItemShape,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                SegmentedListItem(
                    onClick = { showNameDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                    supportingContent = {
                        Text(
                            state.alertName.ifBlank {
                                stringResource(R.string.alert_name_hint)
                            })
                    },
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = colorScheme.surfaceContainerHigh,
                    ),
                    content = {
                        Text(stringResource(R.string.alert_name))
                    })

                SegmentedListItem(
                    onClick = {
                        if (ApplicationListUtil.hasGetInstallAppPermission(context)) {
                            showSelectAppSheet = true
                        } else {
                            showAppListPermissionDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2),
                    leadingContent = state.selectApp?.let { app ->
                        {
                            packageManager.getApplicationIcon(app.packageName).toBitmapOrNull()
                                ?.asImageBitmap()?.let { bitmap ->
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                        }
                    },
                    supportingContent = {
                        val text =
                            state.selectApp?.appName ?: stringResource(R.string.target_app_none)
                        Text(text)
                    },
                    trailingContent = state.selectApp?.let {
                        {
                            IconButton(
                                onClick = { viewModel.processIntent(AlertFormIntent.ClearSelectApp) }) {
                                Icon(
                                    painterResource(R.drawable.close),
                                    contentDescription = stringResource(R.string.clear)
                                )
                            }
                        }
                    },
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = colorScheme.surfaceContainerHigh,
                    ),
                    content = {
                        Text(stringResource(R.string.target_app))
                    })
            }
        }
    }
}
