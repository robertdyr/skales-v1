package com.example.skales.storage.local

import androidx.room.TypeConverter
import com.example.skales.model.ScaleSet
import com.example.skales.model.ScaleSound
import com.example.skales.model.ScaleSoundKind
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromScaleSets(sets: List<ScaleSet>): String {
        return JSONArray().apply {
            sets.forEach { set ->
                put(
                    JSONObject().apply {
                        put("breakAfterBeats", set.breakAfterBeats?.toDouble() ?: JSONObject.NULL)
                        put(
                            "sounds",
                            JSONArray().apply {
                                set.sounds.forEach { sound ->
                                    put(
                                        JSONObject().apply {
                                            put("kind", sound.kind.name)
                                            put("breakAfterBeats", sound.breakAfterBeats?.toDouble() ?: JSONObject.NULL)
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
                        notes = List(notesJson.length(), notesJson::getInt),
                        kind = ScaleSoundKind.valueOf(soundJson.getString("kind")),
                        breakAfterBeats = soundJson.nullableFloat("breakAfterBeats"),
                    )
                },
                breakAfterBeats = setJson.nullableFloat("breakAfterBeats"),
            )
        }
    }
}

private fun JSONObject.nullableFloat(key: String): Float? {
    if (isNull(key)) return null
    return getDouble(key).toFloat()
}
