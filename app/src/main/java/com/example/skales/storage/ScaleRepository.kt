package com.example.skales.storage

import com.example.skales.storage.local.ScaleDao
import com.example.skales.storage.local.toDomain
import com.example.skales.storage.local.toEntity
import com.example.skales.model.Scale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScaleRepository(
    private val scaleDao: ScaleDao,
) {
    fun observeScales(): Flow<List<Scale>> = scaleDao.observeScales().map { scales ->
        scales.map { it.toDomain() }
    }

    suspend fun getScale(scaleId: String): Scale? = scaleDao.getScaleById(scaleId)?.toDomain()

    suspend fun getScaleCreatedAt(scaleId: String): Long? = scaleDao.getScaleById(scaleId)?.createdAt

    suspend fun upsert(scale: Scale, createdAt: Long? = null) {
        val now = System.currentTimeMillis()
        val persistedCreatedAt = createdAt ?: scaleDao.getScaleById(scale.id)?.createdAt ?: now
        scaleDao.upsert(scale.toEntity(createdAt = persistedCreatedAt, updatedAt = now))
    }

    suspend fun delete(scaleId: String) {
        scaleDao.deleteById(scaleId)
    }
}
