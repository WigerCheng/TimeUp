package io.wiger.timeup.ui.addalert.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.wiger.timeup.ui.theme.EndItemShape
import io.wiger.timeup.ui.theme.MiddleItemShape
import io.wiger.timeup.ui.theme.StartItemShape

@Composable
fun TimeInputView(
    state: TextFieldState,
    title: String,
    inputShape: Shape,
    max: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier
    ) {
        Text(title, style = typography.titleMediumEmphasized)
        Spacer(Modifier.height(2.dp))
        TimeInputField(
            state, max = max, shape = inputShape, modifier = Modifier.weight(1f)
        )
    }
}

@Preview
@Composable
private fun TimeInputViewPreview() {
    Row(
        modifier = Modifier.height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        TimeInputView(rememberTextFieldState("1"), "hour", StartItemShape, 23)
        TimeInputView(rememberTextFieldState("2"), "minute", MiddleItemShape, 23)
        TimeInputView(rememberTextFieldState("3"), "second", EndItemShape, 23)
    }
}
