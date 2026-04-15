package com.example.skales.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.skales.storage.ScaleRepository
import com.example.skales.model.Scale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScaleListUiState(
    val scales: List<Scale> = emptyList(),
)

class ScaleListViewModel(
    private val scaleRepository: ScaleRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScaleListUiState())
    val uiState: StateFlow<ScaleListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            scaleRepository.observeScales().collect { scales ->
                _uiState.update { it.copy(scales = scales) }
            }
        }
    }

    fun deleteScale(scaleId: String) {
        viewModelScope.launch {
            scaleRepository.delete(scaleId)
        }
    }

    companion object {
        fun factory(scaleRepository: ScaleRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return ScaleListViewModel(scaleRepository) as T
                }
            }
        }
    }
}
