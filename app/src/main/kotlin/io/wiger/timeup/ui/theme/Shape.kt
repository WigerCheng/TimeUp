package io.wiger.timeup.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.runtime.Composable

val StartItemShape: RoundedCornerShape
    @Composable get() = RoundedCornerShape(
        topStart = shapes.extraLarge.topStart,
        topEnd = shapes.extraSmall.topEnd,
        bottomStart = shapes.extraLarge.bottomStart,
        bottomEnd = shapes.extraSmall.bottomEnd
    )

val MiddleItemShape: RoundedCornerShape
    @Composable get() = RoundedCornerShape(shapes.extraSmall.topStart)

val EndItemShape: RoundedCornerShape
    @Composable get() = RoundedCornerShape(
        topStart = shapes.extraSmall.topStart,
        topEnd = shapes.extraLarge.topEnd,
        bottomStart = shapes.extraSmall.bottomStart,
        bottomEnd = shapes.extraLarge.bottomEnd
    )
