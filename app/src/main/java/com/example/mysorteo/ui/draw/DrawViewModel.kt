package com.example.mysorteo.ui.draw

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mysorteo.data.DrawRepository
import com.example.mysorteo.model.Draw
import com.example.mysorteo.model.Entry
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DrawUiState(
    val currentDraw: Draw? = null,
    val allDraws: List<Draw> = emptyList(),
    val userEntry: Entry? = null,
    val selectedNumbers: Set<Int> = emptySet(),
    val manualInputText: String = "",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class DrawViewModel(
    private val drawRepository: DrawRepository = DrawRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrawUiState())
    val uiState: StateFlow<DrawUiState> = _uiState.asStateFlow()

    private var drawJob: Job? = null
    private var entryJob: Job? = null
    private var allDrawsJob: Job? = null

    init {
        viewModelScope.launch {
            drawRepository.ensureDefaultDrawExists()
            observeAllDraws()
        }
    }

    fun observeAllDraws() {
        allDrawsJob?.cancel()
        allDrawsJob = viewModelScope.launch {
            drawRepository.observeAllDraws().collect { draws ->
                _uiState.update { it.copy(allDraws = draws) }
            }
        }
    }

    fun selectDrawAndUser(drawId: String, userId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        drawJob?.cancel()
        drawJob = viewModelScope.launch {
            drawRepository.observeDraw(drawId).collect { draw ->
                _uiState.update { it.copy(currentDraw = draw, isLoading = false) }
            }
        }

        entryJob?.cancel()
        entryJob = viewModelScope.launch {
            if (userId.isNotEmpty()) {
                drawRepository.observeUserEntry(drawId, userId).collect { entry ->
                    _uiState.update { state ->
                        // If user already registered an entry, preselect those numbers if selectedNumbers is empty
                        val newSelected = if (entry != null && state.selectedNumbers.isEmpty()) {
                            entry.numbers.toSet()
                        } else {
                            state.selectedNumbers
                        }
                        state.copy(userEntry = entry, selectedNumbers = newSelected)
                    }
                }
            }
        }
    }

    fun toggleNumberSelection(number: Int) {
        _uiState.update { state ->
            val current = state.selectedNumbers.toMutableSet()
            if (current.contains(number)) {
                current.remove(number)
            } else {
                if (current.size < 6) {
                    current.add(number)
                }
            }
            state.copy(
                selectedNumbers = current,
                manualInputText = current.sorted().joinToString(", "),
                errorMessage = null
            )
        }
    }

    fun setManualInputText(text: String) {
        _uiState.update { state ->
            // Try to parse up to 6 numbers separated by commas/spaces
            val parsed = text.split(",", " ", ";")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..45 }
                .distinct()
                .take(6)
                .toSet()

            state.copy(
                manualInputText = text,
                selectedNumbers = parsed,
                errorMessage = null
            )
        }
    }

    fun quickPresetNumbers(numbers: List<Int>) {
        val valid = numbers.filter { it in 1..45 }.distinct().take(6).toSet()
        _uiState.update { state ->
            state.copy(
                selectedNumbers = valid,
                manualInputText = valid.sorted().joinToString(", "),
                errorMessage = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun registerEntry(
        drawId: String,
        userId: String,
        userEmail: String,
        onSuccess: () -> Unit
    ) {
        val selected = _uiState.value.selectedNumbers.toList().sorted()

        if (selected.size != 6) {
            _uiState.update { it.copy(errorMessage = "Debe seleccionar exactamente 6 números.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = drawRepository.registerEntry(drawId, userId, userEmail, selected)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        successMessage = "¡Jugada registrada con éxito!"
                    )
                }
                onSuccess()
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Error al registrar la jugada."
                _uiState.update { it.copy(isSubmitting = false, errorMessage = errorMsg) }
            }
        }
    }

    fun publishDrawnNumber(drawId: String, number: Int) {
        viewModelScope.launch {
            val result = drawRepository.publishDrawnNumber(drawId, number)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.localizedMessage) }
            }
        }
    }

    fun resetDraw(drawId: String) {
        viewModelScope.launch {
            val result = drawRepository.resetDraw(drawId)
            if (result.isFailure) {
                _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.localizedMessage) }
            } else {
                _uiState.update { it.copy(successMessage = "Sorteo reiniciado.") }
            }
        }
    }
}
