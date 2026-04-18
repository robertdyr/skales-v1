package com.example.skales.editor

import com.example.skales.model.ScaleSet
import com.example.skales.model.ScaleSound
import com.example.skales.model.ScaleSoundKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaleEditorOpsTest {
    @Test
    fun addSoundToSelectedSetAtColumn_storesAbsoluteStep() {
        val updated = ScaleEditorOps.addSoundToSelectedSetAtColumn(
            sets = listOf(ScaleSet(sounds = emptyList())),
            selectedSetIndex = 0,
            midi = 60,
            column = 3,
            kind = ScaleSoundKind.Note,
            stepBeats = SetGridOps.FineStepBeats,
        )

        assertEquals(3, updated.first().sounds.single().step)
    }

    @Test
    fun moveSetBoundary_shiftsTargetAndLaterSetsTogether() {
        val sets = listOf(
            ScaleSet(sounds = listOf(ScaleSound(notes = listOf(60), kind = ScaleSoundKind.Note, step = 0))),
            ScaleSet(sounds = listOf(ScaleSound(notes = listOf(62), kind = ScaleSoundKind.Note, step = 4))),
            ScaleSet(sounds = listOf(ScaleSound(notes = listOf(64), kind = ScaleSoundKind.Note, step = 8))),
        )

        val updated = ScaleEditorOps.moveSetBoundary(
            sets = sets,
            targetSetIndex = 1,
            column = 6,
            stepBeats = SetGridOps.FineStepBeats,
        )

        assertEquals(6, updated[1].sounds.single().step)
        assertEquals(10, updated[2].sounds.single().step)
    }

    @Test
    fun toGrid_usesFirstSoundAsLaterSetBoundary() {
        val grid = SetGridOps.toGrid(
            sets = listOf(
                ScaleSet(sounds = listOf(ScaleSound(notes = listOf(60), kind = ScaleSoundKind.Note, step = 4))),
                ScaleSet(
                    sounds = listOf(
                        ScaleSound(notes = listOf(62), kind = ScaleSoundKind.Note, step = 8),
                        ScaleSound(notes = listOf(64), kind = ScaleSoundKind.Note, step = 10),
                    ),
                ),
            ),
        )

        assertEquals(listOf(0, 8), grid.setStartColumns)
        assertTrue(grid.notes.filter { it.setIndex == 1 }.all { it.column >= grid.setStartColumns[1] })
    }

    @Test
    fun buildSavableScale_filtersEmptySoundsAndTrimsName() {
        val scale = ScaleEditorOps.buildSavableScale(
            scaleId = "",
            name = "  Test Scale  ",
            sets = listOf(
                ScaleSet(
                    sounds = listOf(
                        ScaleSound(notes = emptyList(), kind = ScaleSoundKind.Note, step = 0),
                        ScaleSound(notes = listOf(60), kind = ScaleSoundKind.Note, step = 4),
                    ),
                ),
            ),
            bpm = 92,
        )

        assertNotNull(scale)
        assertEquals("Test Scale", scale?.name)
        assertEquals(listOf(60), scale?.sets?.single()?.sounds?.flatMap { it.notes })
        assertEquals(4, scale?.sets?.single()?.sounds?.single()?.step)
    }

    @Test
    fun buildSavableScale_returnsNullForBlankName() {
        val scale = ScaleEditorOps.buildSavableScale(
            scaleId = null,
            name = "   ",
            sets = listOf(ScaleSet(sounds = listOf(ScaleSound(notes = listOf(60), kind = ScaleSoundKind.Note, step = 0)))),
            bpm = 92,
        )

        assertNull(scale)
    }
}
