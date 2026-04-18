package com.example.skales.storage.local

import androidx.room.TypeConverter
import com.example.skales.model.ScaleSet
import com.example.skales.model.ScaleSound
import com.example.skales.model.ScaleSoundKind
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromScaleSets(sets: List<ScaleSet>): String {
        return JSONArray().apply {
            sets.forEach { set ->
                put(
                    JSONObject().apply {
                        put(
                            "sounds",
                            JSONArray().apply {
                                set.sounds.forEach { sound ->
                                    put(
                                        JSONObject().apply {
                                            put("id", sound.id)
                                            put("kind", sound.kind.name)
                                            put("step", sound.step)
                                            put(
                                                "notes",
                                                JSONArray().apply {
                                                    sound.notes.forEach(::put)
                                                },
                                            )
                                        },
                                    )
                                }
                            },
                        )
                    },
                )
            }
        }.toString()
    }

    @TypeConverter
    fun toScaleSets(value: String): List<ScaleSet> {
        if (value.isBlank()) return emptyList()

        val setsJson = JSONArray(value)
        return List(setsJson.length()) { setIndex ->
            val setJson = setsJson.getJSONObject(setIndex)
            val soundsJson = setJson.getJSONArray("sounds")

            ScaleSet(
                sounds = List(soundsJson.length()) { soundIndex ->
                    val soundJson = soundsJson.getJSONObject(soundIndex)
                    val notesJson = soundJson.getJSONArray("notes")

                    ScaleSound(
                        id = if (soundJson.has("id") && !soundJson.isNull("id")) {
                            soundJson.getString("id")
                        } else {
                            UUID.randomUUID().toString()
                        },
                        notes = List(notesJson.length(), notesJson::getInt),
                        kind = ScaleSoundKind.valueOf(soundJson.getString("kind")),
                        step = soundJson.optInt("step", 0),
                    )
                },
            )
        }
    }
}
