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
    val isInstitutionalEmailValid: Boolean get() {
        val trimmed = email.trim().lowercase()
        return trimmed.endsWith("@ucv.edu.pe") || trimmed.endsWith("@ucvvirtual.edu.pe")
    }

    val canSubmit: Boolean get() {
        if (!isInstitutionalEmailValid || password.length < 6 || isLoading) return false
        return if (isLoginMode) true else fullName.isNotBlank() && phone.isNotBlank()
    }
}