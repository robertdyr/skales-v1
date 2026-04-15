package com.example.skales.analyzer

import android.media.MediaMetadataRetriever
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealAudioFixtureSmokeTest {
    @Test
    fun soundExampleFixture_isPackagedAndReadable() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val descriptor = context.assets.openFd(FixtureCatalog.soundExampleAssetPath)
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()

            assertNotNull(durationMs)
            assertTrue(durationMs != null && durationMs > 0L)
        } finally {
            retriever.release()
            descriptor.close()
        }
    }
}

object FixtureCatalog {
    const val soundExampleAssetPath = "fixtures/soundexample.m4a"
}
