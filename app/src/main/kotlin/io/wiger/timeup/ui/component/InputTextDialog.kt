package io.wiger.timeup.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import io.wiger.timeup.R
import kotlinx.coroutines.delay

@Composable
fun InputTextDialog(
    title: String,
    text: String,
    hint: String? = null,
    modifier: Modifier = Modifier,
    callback: (String?) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        delay(100L)
        focusRequester.requestFocus()
    }
    val state = rememberTextFieldState(text)
    AlertDialog(
        title = { Text(title) },
        text = {
            TextField(
                state = state,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    showKeyboardOnFocus = true
                ),
                onKeyboardAction = {
                    callback(state.text.toString())
                },
                modifier = Modifier.focusRequester(focusRequester),
                placeholder = hint?.let { hintText -> { Text(hintText) } },
                lineLimits = TextFieldLineLimits.SingleLine
            )
        },
        confirmButton = {
            Button(
                onClick = { callback(state.text.toString()) },
                shapes = ButtonDefaults.shapes(),
                contentPadding = ButtonDefaults.SmallContentPadding
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { callback(null) },
                shapes = ButtonDefaults.shapes(),
                contentPadding = ButtonDefaults.SmallContentPadding
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        onDismissRequest = { callback(null) },
        modifier = modifier
    )

}
