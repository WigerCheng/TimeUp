package io.wiger.timeup.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.then
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.text.isDigitsOnly

private class DigitOnlyInputTransformation : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        if (!asCharSequence().isDigitsOnly()) {
            revertAllChanges()
        }
    }
}

private class MaxValueInputTransformation(private val maxValue: Int) : InputTransformation {

    override val keyboardOptions: KeyboardOptions
        get() = KeyboardOptions(keyboardType = KeyboardType.Number)

    override fun TextFieldBuffer.transformInput() {
        val number = asCharSequence().toString().toIntOrNull()
        if (number != null && number > maxValue) {
            revertAllChanges()
        }
    }
}

@Stable
fun InputTransformation.digitOnly(): InputTransformation =
    this.then(DigitOnlyInputTransformation())

@Stable
fun InputTransformation.maxValue(maxValue: Int): InputTransformation =
    this.then(MaxValueInputTransformation(maxValue))