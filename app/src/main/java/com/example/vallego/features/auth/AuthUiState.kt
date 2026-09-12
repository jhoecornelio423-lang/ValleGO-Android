package com.example.vallego.features.auth

import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.model.UserRole

data class AuthUiState(
    val isLoginMode: Boolean = true,
    val email: String = "",
    val password: String = "",
    val fullName: String = "",
    val phone: String = "",
    val selectedRole: UserRole = UserRole.COMPRADOR,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val profile: UserProfile? = null
) {
    val isEmailValid: Boolean get() {
        val trimmed = email.trim()
        return trimmed.isNotEmpty() && EMAIL_REGEX.matches(trimmed)
    }

    val isInstitutionalEmailValid: Boolean get() = isEmailValid

    val canSubmit: Boolean get() {
        if (!isEmailValid || password.length < 6 || isLoading) return false
        return if (isLoginMode) true else fullName.isNotBlank() && phone.isNotBlank()
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}