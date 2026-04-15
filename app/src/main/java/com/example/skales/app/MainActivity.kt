package com.example.skales.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.example.skales.app.navigation.SkalesNavGraph
import com.example.skales.app.theme.SkalesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as SkalesApplication
        enableEdgeToEdge()
        setContent {
            SkalesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    SkalesNavGraph(
                        scaleRepository = app.scaleRepository,
                        pianoSoundPlayer = app.pianoSoundPlayer,
                        scaleStepper = app.scaleStepper,
                        scaleAutoPlayer = app.scaleAutoPlayer,
                    )
                }
            }
        }
    }
}
