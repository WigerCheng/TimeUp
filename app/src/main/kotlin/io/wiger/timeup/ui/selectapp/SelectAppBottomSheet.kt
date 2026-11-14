@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package io.wiger.timeup.ui.selectapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.wiger.timeup.R
import io.wiger.timeup.data.model.AppInfo

@Composable
fun SelectAppBottomSheet(
    viewModel: SelectAppViewModel,
    onDismiss: () -> Unit,
    onSelectApp: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.fillMaxSize()
    ) {
        SelectAppSheetContent(
            viewModel = viewModel,
            onBack = onDismiss,
            onSelectApp = onSelectApp,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun SelectAppSheetContent(
    viewModel: SelectAppViewModel,
    onBack: () -> Unit = {},
    onSelectApp: (AppInfo) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    var expanded by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.init()
    }

    val topBarSubtitle: @Composable () -> Unit = if (isSearchActive) {
        {}
    } else {
        { Text(stringResource(R.string.select_app_subtitle)) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                subtitle = topBarSubtitle,
                title = {
                    if (isSearchActive) {
                        val focusRequester = remember { FocusRequester() }
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            placeholder = { Text(stringResource(R.string.search_app_hint)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    FilledTonalIconButton(
                                        onClick = { viewModel.onSearchQueryChange("") },
                                        shapes = IconButtonDefaults.shapes()
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.close),
                                            contentDescription = stringResource(R.string.clear)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(stringResource(R.string.select_app_title))
                    }
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = {
                            if (isSearchActive) {
                                isSearchActive = false
                                viewModel.onSearchQueryChange("")
                            } else {
                                onBack()
                            }
                        },
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(
                            painterResource(R.drawable.back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        FilledTonalIconButton(
                            onClick = { isSearchActive = true },
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(
                                painterResource(R.drawable.search),
                                contentDescription = stringResource(R.string.search)
                            )
                        }

                        Box {
                            FilledTonalIconButton(
                                onClick = { expanded = !expanded },
                                shapes = IconButtonDefaults.shapes()
                            ) {
                                Icon(
                                    painterResource(R.drawable.more_vert),
                                    contentDescription = null
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.refresh)) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.refresh),
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        viewModel.refresh()
                                        expanded = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.show_system_app)) },
                                    trailingIcon = {
                                        Checkbox(
                                            checked = state.isShowSystemApp,
                                            onCheckedChange = null
                                        )
                                    },
                                    onClick = {
                                        viewModel.setShowSystemApp(!state.isShowSystemApp)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (state.loading) {
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.filteredAppList.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_matching_apps),
                    style = typography.bodyLarge,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(modifier = Modifier.matchParentSize()) {
                    items(state.filteredAppList, key = { it.packageName }) { appInfo ->
                        ListItem(
                            onClick = { onSelectApp(appInfo) },
                            modifier = Modifier.fillParentMaxWidth(),
                            shapes = ListItemDefaults.shapes(),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            content = {
                                Text(appInfo.appName)
                            },
                            supportingContent = {
                                Text(appInfo.packageName)
                            },
                            leadingContent = {
                                packageManager.getApplicationIcon(appInfo.packageName)
                                    .toBitmapOrNull()
                                    ?.asImageBitmap()
                                    ?.let {
                                        Image(
                                            bitmap = it,
                                            contentDescription = null,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                            }
                        )
                    }
                }
            }
        }
    }
}
