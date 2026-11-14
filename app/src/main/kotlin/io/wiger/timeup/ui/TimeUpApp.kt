package io.wiger.timeup.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import io.wiger.timeup.ui.home.HomeScreen
import io.wiger.timeup.ui.home.HomeViewModel

@Composable
fun TimeUpApp() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        val homeViewModel = viewModel<HomeViewModel>()
        HomeScreen(viewModel = homeViewModel)
    }
}
