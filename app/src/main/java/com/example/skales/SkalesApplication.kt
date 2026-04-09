package com.example.skales

import android.app.Application
import com.example.skales.audio.PianoSoundPlayer
import com.example.skales.audio.ScaleAutoPlayer
import com.example.skales.audio.ScaleStepper
import com.example.skales.data.local.SkalesDatabase
import com.example.skales.data.repository.ScaleRepository

class SkalesApplication : Application() {
    val database: SkalesDatabase by lazy {
        SkalesDatabase.create(this)
    }

    val scaleRepository: ScaleRepository by lazy {
        ScaleRepository(database.scaleDao())
    }

    val pianoSoundPlayer: PianoSoundPlayer by lazy {
        PianoSoundPlayer(this)
    }

    val scaleStepper: ScaleStepper by lazy {
        ScaleStepper(pianoSoundPlayer)
    }

    val scaleAutoPlayer: ScaleAutoPlayer by lazy {
        ScaleAutoPlayer(scaleStepper)
    }

    override fun onTerminate() {
        super.onTerminate()
        pianoSoundPlayer.release()
    }
}
