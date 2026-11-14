package io.wiger.timeup.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.wiger.timeup.R
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards that [HomeContent] consumes the full window inset padding it is given.
 *
 * On a landscape device the display cutout lives on the start/end edge, so dropping those
 * values would draw the header and the list into the cutout area.
 */
@RunWith(AndroidJUnit4::class)
class HomeContentInsetsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentPadding_isAppliedOnAllSides() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val appName = context.getString(R.string.app_name)
        val settingsLabel = context.getString(R.string.settings)
        val startInset = 96.dp
        val topInset = 48.dp
        val endInset = 64.dp

        composeRule.setContent {
            HomeContent(
                timeUps = persistentListOf(),
                onSettingsClick = {},
                onToggleTimeUp = { _, _ -> },
                onRemoveTimeUp = {},
                onEditTimeUp = {},
                isDeletionPending = false,
                contentPadding = PaddingValues(
                    start = startInset,
                    top = topInset,
                    end = endInset,
                    bottom = 40.dp,
                ),
            )
        }

        composeRule.waitForIdle()

        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val title = composeRule.onNodeWithText(appName).getUnclippedBoundsInRoot()
        val settings = composeRule.onNodeWithContentDescription(settingsLabel)
            .getUnclippedBoundsInRoot()

        assertTrue("Title must clear the start inset", title.left >= startInset)
        assertTrue("Title must clear the top inset", title.top >= topInset)
        assertTrue(
            "Settings button must clear the end inset",
            settings.right <= root.right - endInset,
        )
    }
}
