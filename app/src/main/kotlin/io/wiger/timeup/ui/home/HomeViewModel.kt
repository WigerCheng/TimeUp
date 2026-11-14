package io.wiger.timeup.ui.home

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.wiger.timeup.data.model.TimeUp
import io.wiger.timeup.data.repository.TimeUpRepository
import io.wiger.timeup.notification.AlertService
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val timeUpRepository: TimeUpRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    private val _deletionState = MutableStateFlow(DeletionState())
    private var pendingDeleteJob: Job? = null
    private var restoreJob: Job? = null

    val uiState = combine(
        _uiState,
        timeUpRepository.allTimeUpsFlow,
        _deletionState,
    ) { uiState, timeUps, deletionState ->
        uiState.copy(
            timeUps = timeUps
                .filterNot { timeUp ->
                    val id = timeUp.id
                    id != null && (
                        id in deletionState.deletingIds ||
                            id == deletionState.pending?.id
                        )
                }
                .toImmutableList(),
            pendingDelete = deletionState.pending,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun toggleTimeUp(context: Context, timeUp: TimeUp, isEnabled: Boolean) {
        if (isEnabled && timeUp.alertTime <= 0L) return

        val now = System.currentTimeMillis()
        val updatedTimeUp = if (isEnabled) {
            timeUp.copy(
                isEnabled = true,
                endsAtMillis = now + timeUp.alertTime
            )
        } else {
            timeUp.copy(
                isEnabled = false,
                endsAtMillis = null
            )
        }
        viewModelScope.launch {
            timeUpRepository.updateTimeUp(updatedTimeUp)

            val serviceIntent = Intent(context, AlertService::class.java).apply {
                action = if (isEnabled) {
                    AlertService.ACTION_START_ALERT
                } else {
                    AlertService.ACTION_STOP_ALERT
                }
                putExtra(
                    AlertService.EXTRA_TIME_ALERT,
                    if (isEnabled) updatedTimeUp else timeUp,
                )
            }
            if (isEnabled) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    /**
     * Re-creates notifications for alerts that were still running when the app/service process
     * died. Called whenever the app comes to the foreground.
     */
    fun restoreRunningAlerts() {
        if (restoreJob?.isActive == true) return

        restoreJob = viewModelScope.launch {
            val now = System.currentTimeMillis()
            val running = runCatching { timeUpRepository.getEnabledTimeUps() }
                .getOrDefault(emptyList())

            running.forEach { timeUp ->
                val endsAt = timeUp.endsAtMillis
                if (endsAt != null && endsAt > now) {
                    val intent = Intent(appContext, AlertService::class.java).apply {
                        action = AlertService.ACTION_START_ALERT
                        putExtra(AlertService.EXTRA_TIME_ALERT, timeUp)
                    }
                    runCatching { ContextCompat.startForegroundService(appContext, intent) }
                } else {
                    runCatching {
                        timeUpRepository.updateTimeUp(
                            timeUp.copy(isEnabled = false, endsAtMillis = null)
                        )
                    }
                }
            }
        }
    }

    /**
     * Hides the row immediately and schedules a permanent deletion after the undo grace period.
     */
    fun requestDelete(timeUp: TimeUp) {
        if (timeUp.id == null) return
        if (_deletionState.value.pending != null) return

        _deletionState.update { state ->
            state.copy(pending = timeUp)
        }
        pendingDeleteJob?.cancel()
        pendingDeleteJob = viewModelScope.launch {
            delay(DELETE_GRACE_MILLIS)
            commitPendingDelete()
        }
    }

    fun undoPendingDelete() {
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        _deletionState.update { state ->
            state.copy(pending = null)
        }
    }

    fun commitPendingDelete() {
        val pending = _deletionState.value.pending ?: return
        val id = pending.id
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null

        if (id == null) {
            _deletionState.update { it.copy(pending = null) }
            return
        }

        _deletionState.update { state ->
            state.copy(
                pending = null,
                deletingIds = state.deletingIds + id,
            )
        }

        if (pending.isEnabled) {
            stopAlertForDeletion(pending)
        }

        viewModelScope.launch {
            runCatching { timeUpRepository.removeTimeUp(id) }
            _deletionState.update { state ->
                state.copy(deletingIds = state.deletingIds - id)
            }
        }
    }

    private fun stopAlertForDeletion(timeUp: TimeUp) {
        val serviceIntent = Intent(appContext, AlertService::class.java).apply {
            action = AlertService.ACTION_STOP_ALERT
            putExtra(AlertService.EXTRA_TIME_ALERT, timeUp)
        }
        runCatching { appContext.startService(serviceIntent) }
    }

    companion object {
        internal const val DELETE_GRACE_MILLIS = 3_000L
    }
}

data class HomeUiState(
    val timeUps: ImmutableList<TimeUp> = persistentListOf(),
    val pendingDelete: TimeUp? = null,
)

private data class DeletionState(
    val pending: TimeUp? = null,
    val deletingIds: Set<Long> = emptySet(),
)
