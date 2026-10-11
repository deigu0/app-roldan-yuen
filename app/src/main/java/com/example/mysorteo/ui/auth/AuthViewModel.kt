package com.example.mysorteo.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mysorteo.data.AppUser
import com.example.mysorteo.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentUser: AppUser? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authStateFlow.collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onConfirmPasswordChanged(password: String) {
        _uiState.update { it.copy(confirmPassword = password, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun login(onSuccess: () -> Unit) {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password.trim()

        if (email.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Por favor ingrese correo y contraseña.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.login(email, password)
            if (result.isSuccess) {
                val user = result.getOrNull()
                _uiState.update { it.copy(isLoading = false, password = "", currentUser = user) }
                onSuccess()
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Error al iniciar sesión."
                _uiState.update { it.copy(isLoading = false, errorMessage = translateAuthError(errorMsg)) }
            }
        }
    }

    fun register(onSuccess: () -> Unit) {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password.trim()
        val confirm = _uiState.value.confirmPassword.trim()

        if (email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Por favor complete todos los campos.") }
            return
        }

        if (password != confirm) {
            _uiState.update { it.copy(errorMessage = "Las contraseñas no coinciden.") }
            return
        }

        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "La contraseña debe tener al menos 6 caracteres.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.register(email, password)
            if (result.isSuccess) {
                val user = result.getOrNull()
                _uiState.update { it.copy(isLoading = false, password = "", confirmPassword = "", currentUser = user) }
                onSuccess()
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Error al registrar cuenta."
                _uiState.update { it.copy(isLoading = false, errorMessage = translateAuthError(errorMsg)) }
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.update { AuthUiState() }
    }

    private fun translateAuthError(message: String): String {
        return when {
            message.contains("The email address is badly formatted", ignoreCase = true) ->
                "El formato del correo electrónico no es válido."
            message.contains("The user may have been deleted", ignoreCase = true) ||
                    message.contains("no user record", ignoreCase = true) ->
                "El correo electrónico no está registrado."
            message.contains("invalid-credential", ignoreCase = true) ||
                    message.contains("wrong-password", ignoreCase = true) ->
                "Correo o contraseña incorrectos."
            message.contains("email address is already in use", ignoreCase = true) ->
                "El correo electrónico ya está registrado por otra cuenta."
            message.contains("Password should be at least", ignoreCase = true) ->
                "La contraseña debe tener al menos 6 caracteres."
            else -> message
        }
    }
}
