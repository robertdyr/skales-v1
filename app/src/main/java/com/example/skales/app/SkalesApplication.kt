package com.example.skales.app

import android.app.Application
import com.example.skales.player.PianoSoundPlayer
import com.example.skales.player.ScaleAutoPlayer
import com.example.skales.player.ScaleStepper
import com.example.skales.storage.local.SkalesDatabase
import com.example.skales.storage.ScaleRepository

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
