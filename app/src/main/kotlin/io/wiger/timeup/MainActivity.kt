package io.wiger.timeup

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.wiger.timeup.core.AppSettings
import io.wiger.timeup.ui.TimeUpApp
import io.wiger.timeup.ui.theme.TimeUpTheme

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by AppSettings.themeMode.collectAsStateWithLifecycle()
            TimeUpTheme(themeMode = themeMode) {
                TimeUpApp()
            }
        }
    }
}
