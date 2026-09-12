package com.example.vallego.domain.repository

import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.model.UserRole
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentProfile: StateFlow<UserProfile?>
    val isAuthenticated: StateFlow<Boolean>

    suspend fun signIn(email: String, password: String): Result<UserProfile>
    suspend fun signUp(email: String, password: String, fullName: String, phone: String, role: UserRole = UserRole.COMPRADOR): Result<UserProfile>
    suspend fun signOut(): Result<Unit>
    suspend fun refreshProfile(): Result<UserProfile?>
    fun isValidEmail(email: String): Boolean
    fun isValidInstitutionalEmail(email: String): Boolean = isValidEmail(email)
}