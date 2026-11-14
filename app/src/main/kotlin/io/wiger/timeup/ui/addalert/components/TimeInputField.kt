package io.wiger.timeup.ui.addalert.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import io.wiger.timeup.ui.component.digitOnly
import io.wiger.timeup.ui.component.maxValue

@Composable
fun TimeInputField(
    state: TextFieldState,
    max: Int,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
) {
    val textStyle = TextStyle(
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.W600,
        fontSize = 56.sp,
        color = colorScheme.onSurface
    )
    BasicTextField(
        state = state,
        modifier = modifier,
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next,
            showKeyboardOnFocus = true
        ),
        inputTransformation = InputTransformation
            .digitOnly()
            .maxLength(2)
            .maxValue(max),
        textStyle = textStyle,
        cursorBrush = SolidColor(colorScheme.onSurface),
        decorator = { innerTextField ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(colorScheme.surfaceContainer, shape)
            ) {
                if (state.text.isEmpty()) {
                    Text(
                        text = "0",
                        style = textStyle.copy(color = colorScheme.outline),
                        textAlign = TextAlign.Center
                    )
                }
                innerTextField()
            }
        }
    )

}

@Preview
@Composable
private fun TimeInputFieldPreview() {
    val state = rememberTextFieldState("22")
    TimeInputField(state, 24)
}
