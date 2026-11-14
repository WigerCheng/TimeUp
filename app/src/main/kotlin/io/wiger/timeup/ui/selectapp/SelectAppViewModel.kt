@file:OptIn(ExperimentalCoroutinesApi::class)

package io.wiger.timeup.ui.selectapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.wiger.timeup.data.model.AppInfo
import io.wiger.timeup.data.repository.AppInfoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectAppViewModel @Inject constructor(
    private val appInfoRepository: AppInfoRepository,
) : ViewModel() {
    private val _viewState: MutableStateFlow<SelectAppUiState> = MutableStateFlow(SelectAppUiState())
    private val isShowSystemAppFlow = _viewState.map { it.isShowSystemApp }.distinctUntilChanged()
    val viewState: StateFlow<SelectAppUiState> =
        isShowSystemAppFlow.flatMapLatest { appInfoRepository.getInstalledAppList(it) }
            .onEach { appInfos ->
                if (appInfos.isNotEmpty()) {
                    _viewState.update { it.copy(loading = false) }
                }
            }
            .combine(_viewState) { list, state ->
                state.copy(appInfoList = list)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SelectAppUiState()
            )

    fun init() {
        // The sheet is re-opened with an activity scoped ViewModel, so always start
        // from a clean, unfiltered list.
        onSearchQueryChange("")
        if (viewState.value.appInfoList.isEmpty()) {
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _viewState.update { it.copy(loading = true) }
            appInfoRepository.refreshAppList()
        }
    }

    fun setShowSystemApp(isShowSystemApp: Boolean) {
        _viewState.update { it.copy(isShowSystemApp = isShowSystemApp) }
    }

    fun onSearchQueryChange(query: String) {
        _viewState.update { it.copy(searchQuery = query) }
    }
}

data class SelectAppUiState(
    val appInfoList: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val isShowSystemApp: Boolean = false,
    val loading: Boolean = false,
) {
    val filteredAppList: List<AppInfo>
        get() {
            if (searchQuery.isBlank()) return appInfoList
            return appInfoList.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
}
