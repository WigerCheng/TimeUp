package io.wiger.timeup.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.wiger.timeup.R

@Composable
fun TASwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked,
        onCheckedChange,
        modifier,
        thumbContent = {
            val icon = if (checked) R.drawable.check else R.drawable.close
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
        enabled
    )
}

@Composable
@PreviewLightDark
private fun TASwitchPreview() {
    Column {
        TASwitch(true, {})
        Spacer(Modifier.height(2.dp))
        TASwitch(false, {})
    }
}
