package io.wiger.timeup.ui.addalert

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.wiger.timeup.R
import io.wiger.timeup.data.local.dao.TimeUpDao
import io.wiger.timeup.data.local.entity.TimeUpEntity
import io.wiger.timeup.data.repository.TimeUpRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards that the add-alert sheet body scrolls when it is given less height than its content
 * needs, e.g. on a landscape phone.
 */
@RunWith(AndroidJUnit4::class)
class AddAlertSheetScrollTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun targetAppRow_isReachableWhenHeightIsLimited() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewModel = AddAlertViewModel(context, TimeUpRepository(FakeTimeUpDao()))
        val targetApp = context.getString(R.string.target_app)

        composeRule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                AddAlertSheetContent(viewModel = viewModel, close = {})
            }
        }

        composeRule.onNodeWithText(targetApp)
            .performScrollTo()
            .assertIsDisplayed()
    }
}

private class FakeTimeUpDao : TimeUpDao {

    override suspend fun insertTimeUps(vararg timeUp: TimeUpEntity) = Unit

    override suspend fun updateTimeUp(vararg timeUp: TimeUpEntity) = Unit

    override suspend fun removeTimeUp(id: Long) = Unit

    override fun loadAllTimeUps(): Flow<List<TimeUpEntity>> = flowOf(emptyList())

    override suspend fun loadEnabledTimeUps(): List<TimeUpEntity> = emptyList()
}
