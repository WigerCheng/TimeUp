package io.wiger.timeup.ui.addalert

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.wiger.timeup.data.model.AppInfo
import io.wiger.timeup.data.model.TimeUp
import io.wiger.timeup.data.repository.TimeUpRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class AddAlertViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeUpRepository: TimeUpRepository,
) : ViewModel() {

    private suspend fun addTimeUp(timeUp: TimeUp) {
        timeUpRepository.addTimeUp(timeUp)
    }

    private val _state = MutableStateFlow(AlertFormUiState())
    val state = _state.asStateFlow()

    private val _event: MutableSharedFlow<AlertFormEvent> = MutableSharedFlow()
    val event = _event.asSharedFlow()

    val hourTextFieldState by lazy { TextFieldState(DEFAULT_TIME_VALUE) }
    val minuteTextFieldState by lazy { TextFieldState(DEFAULT_TIME_VALUE) }
    val secondTextFieldState by lazy { TextFieldState(DEFAULT_TIME_VALUE) }

    fun processIntent(intent: AlertFormIntent) {
        when (intent) {
            is AlertFormIntent.Submit -> submit()
            is AlertFormIntent.Edit -> edit(intent.timeUp)
            is AlertFormIntent.ResetForm -> resetForm()
            is AlertFormIntent.UpdateSelectApp -> _state.update {
                it.copy(selectApp = intent.app, appCleared = false)
            }
            is AlertFormIntent.ClearSelectApp -> _state.update {
                it.copy(selectApp = null, appCleared = true)
            }
            is AlertFormIntent.UpdateAlertName -> _state.update { it.copy(alertName = intent.alertName) }
        }
    }

    /**
     * Restores the form to its initial state so that every sheet opening starts fresh.
     */
    fun resetForm() {
        _state.value = AlertFormUiState()
        listOf(hourTextFieldState, minuteTextFieldState, secondTextFieldState)
            .forEach { textFieldState ->
                textFieldState.edit {
                    replace(0, length, DEFAULT_TIME_VALUE)
                }
            }
    }

    private fun edit(timeUp: TimeUp) {
        _state.value = AlertFormUiState(
            alertName = timeUp.alertName,
            selectApp = resolveAppInfo(timeUp.appName),
            editingTimeUp = timeUp,
        )

        val totalSeconds = (timeUp.alertTime / 1_000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        listOf(
            hourTextFieldState to hours,
            minuteTextFieldState to minutes,
            secondTextFieldState to seconds,
        ).forEach { (state, value) ->
            state.edit {
                replace(0, length, if (value == 0L) "" else value.toString())
            }
        }
    }

    private fun resolveAppInfo(packageName: String?): AppInfo? {
        if (packageName.isNullOrBlank()) return null
        return runCatching {
            val applicationInfo = context.packageManager.getApplicationInfo(packageName, 0)
            AppInfo(
                appName = context.packageManager.getApplicationLabel(applicationInfo).toString(),
                packageName = packageName,
                isSystemApp = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            )
        }.getOrElse {
            AppInfo(appName = packageName, packageName = packageName, isSystemApp = false)
        }
    }

    private fun submit() {
        val state = _state.value
        val hourVal = hourTextFieldState.text.toString().toIntOrNull() ?: 0
        val minVal = minuteTextFieldState.text.toString().toIntOrNull() ?: 0
        val secVal = secondTextFieldState.text.toString().toIntOrNull() ?: 0

        val totalDuration = hourVal.hours + minVal.minutes + secVal.seconds
        val timeMs = totalDuration.inWholeMilliseconds

        if (timeMs <= 0) {
            viewModelScope.launch {
                _event.emit(AlertFormEvent.InvalidDuration)
            }
            return
        }

        val name = state.alertName
        val editing = state.editingTimeUp
        val appPackageName = when {
            state.appCleared -> null
            state.selectApp != null -> state.selectApp.packageName
            editing != null -> editing.appName
            else -> null
        }
        viewModelScope.launch {
            if (editing != null) {
                timeUpRepository.updateTimeUp(
                    editing.copy(
                        alertName = name,
                        alertTime = timeMs,
                        isEnabled = false,
                        appName = appPackageName,
                        endsAtMillis = null,
                    )
                )
            } else {
                addTimeUp(TimeUp(name, timeMs, false, appPackageName))
            }
            _event.emit(AlertFormEvent.SubmitDone)
        }
    }
}

sealed interface AlertFormEvent {
    data object SubmitDone : AlertFormEvent
    data object InvalidDuration : AlertFormEvent
}

sealed interface AlertFormIntent {
    data class Edit(val timeUp: TimeUp) : AlertFormIntent
    data class UpdateAlertName(val alertName: String) : AlertFormIntent
    data class UpdateSelectApp(val app: AppInfo) : AlertFormIntent
    data object ClearSelectApp : AlertFormIntent
    data object ResetForm : AlertFormIntent
    data object Submit : AlertFormIntent
}

data class AlertFormUiState(
    val alertName: String = "",
    val selectApp: AppInfo? = null,
    val editingTimeUp: TimeUp? = null,
    val appCleared: Boolean = false,
)

private const val DEFAULT_TIME_VALUE = ""
