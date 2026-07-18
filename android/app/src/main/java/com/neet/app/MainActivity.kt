package com.neet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.neet.app.di.AppContainer
import com.neet.app.navigation.NeetNavHost
import com.neet.app.ui.theme.NeetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NeetTheme {
                Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                    NeetNavHost(
                        repository = AppContainer.questionRepository,
                        historyRepository = AppContainer.historyRepository,
                        mockTestRepository = AppContainer.mockTestRepository,
                        authRepository = AppContainer.authRepository,
                        syncRepository = AppContainer.syncRepository,
                        notesRepository = AppContainer.notesRepository,
                        examDateStore = AppContainer.examDateStore,
                    )
                }
            }
        }
    }
}
