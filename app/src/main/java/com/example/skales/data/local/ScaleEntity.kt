package com.example.skales.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.skales.domain.model.PlaybackTiming
import com.example.skales.domain.model.Scale
import com.example.skales.domain.model.ScaleSet

@Entity(tableName = "scales")
data class ScaleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sets: List<ScaleSet>,
    val defaultBpm: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

fun ScaleEntity.toDomain(): Scale = Scale(
    id = id,
    name = name,
    sets = sets,
    timing = PlaybackTiming(
        defaultBpm = defaultBpm,
    ),
)

fun Scale.toEntity(createdAt: Long, updatedAt: Long): ScaleEntity = ScaleEntity(
    id = id,
    name = name,
    sets = sets,
    defaultBpm = timing.defaultBpm,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
