package com.example.skales

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.skales.ui.navigation.SkalesNavGraph
import com.example.skales.ui.theme.SkalesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as SkalesApplication
        enableEdgeToEdge()
        setContent {
            SkalesTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
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
