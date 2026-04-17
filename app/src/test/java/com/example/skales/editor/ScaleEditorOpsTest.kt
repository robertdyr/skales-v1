package com.example.skales.editor

import com.example.skales.model.ScaleSet
import com.example.skales.model.ScaleSound
import com.example.skales.model.ScaleSoundKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScaleEditorOpsTest {
    @Test
    fun addChordPreCue_prependsCueFromDistinctNotes() {
        val sets = listOf(
            ScaleSet(
                sounds = listOf(
                    ScaleSound(notes = listOf(60), kind = ScaleSoundKind.Note),
                    ScaleSound(notes = listOf(64), kind = ScaleSoundKind.Note),
                    ScaleSound(notes = listOf(67), kind = ScaleSoundKind.Note),
                ),
            ),
        )

        val updated = ScaleEditorOps.addChordPreCueToSelectedSet(sets, 0)

        assertEquals(ScaleSoundKind.Cue, updated.first().sounds.first().kind)
        assertEquals(listOf(60, 64, 67), updated.first().sounds.first().notes)
    }

    @Test
    fun deleteSelectedSet_keepsAtLeastOneEmptySet() {
        val (updatedSets, selectedIndex) = ScaleEditorOps.deleteSelectedSet(ScaleEditorOps.defaultSets(), 0)

        assertEquals(1, updatedSets.size)
        assertEquals(emptyList<Int>(), updatedSets.single().sounds.flatMap { it.notes })
        assertEquals(0, selectedIndex)
    }

    @Test
    fun buildSavableScale_filtersEmptySoundsAndTrimsName() {
        val scale = ScaleEditorOps.buildSavableScale(
            scaleId = "",
            name = "  Test Scale  ",
            sets = listOf(
                ScaleSet(
                    sounds = listOf(
                        ScaleSound(notes = emptyList(), kind = ScaleSoundKind.Note),
                        ScaleSound(notes = listOf(60), kind = ScaleSoundKind.Note),
                    ),
                ),
            ),
            bpm = 92,
        )

        assertNotNull(scale)
        assertEquals("Test Scale", scale?.name)
        assertEquals(listOf(60), scale?.sets?.single()?.sounds?.flatMap { it.notes })
    }

    @Test
    fun buildSavableScale_returnsNullForBlankName() {
        val scale = ScaleEditorOps.buildSavableScale(
            scaleId = null,
            name = "   ",
            sets = listOf(ScaleSet(sounds = listOf(ScaleSound(notes = listOf(60), kind = ScaleSoundKind.Note)))),
            bpm = 92,
        )

        assertNull(scale)
    }
}
