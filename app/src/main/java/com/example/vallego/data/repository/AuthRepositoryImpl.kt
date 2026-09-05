package com.example.vallego.data.repository

import com.example.vallego.domain.model.UserProfile
import com.example.vallego.domain.model.UserRole
import com.example.vallego.domain.repository.AuthRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class AuthRepositoryImpl(
    private val auth: Auth,
    private val postgrest: Postgrest
) : AuthRepository {

    private val _currentProfile = MutableStateFlow<UserProfile?>(null)
    override val currentProfile: StateFlow<UserProfile?> = _currentProfile.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        _isAuthenticated.value = true
                        val user = auth.currentUserOrNull()
                        if (user != null) {
                            try {
                                _currentProfile.value = fetchProfile(user.id)
                            } catch (_: Exception) {
                                val meta = user.userMetadata
                                val metaName = meta?.get("full_name")?.jsonPrimitive?.contentOrNull ?: "Estudiante UCV"
                                val metaRoleStr = meta?.get("role")?.jsonPrimitive?.contentOrNull ?: "comprador"
                                val role = when (metaRoleStr.lowercase()) {
                                    "admin" -> UserRole.ADMIN
                                    "emprendedor" -> UserRole.EMPRENDEDOR
                                    else -> UserRole.COMPRADOR
                                }
                                _currentProfile.value = UserProfile(
                                    id = user.id,
                                    fullName = metaName,
                                    role = role
                                )
                            }
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _currentProfile.value = null
                        _isAuthenticated.value = false
                    }
                    else -> Unit
                }
            }
        }
    }

    override fun isValidInstitutionalEmail(email: String): Boolean {
        val trimmed = email.trim().lowercase()
        return trimmed.endsWith("@ucv.edu.pe") || trimmed.endsWith("@ucvvirtual.edu.pe")
    }

    override suspend fun signIn(email: String, password: String): Result<UserProfile> {
        val trimmedEmail = email.trim().lowercase()
        if (!isValidInstitutionalEmail(trimmedEmail)) {
            return Result.failure(
                IllegalArgumentException("Debes usar tu correo institucional UCV (@ucvvirtual.edu.pe o @ucv.edu.pe)")
            )
        }

        return try {
            auth.signInWith(Email) {
                this.email = trimmedEmail
                this.password = password
            }
            val user = auth.currentUserOrNull()
                ?: throw IllegalStateException("Sesión no iniciada correctamente")
            val profile = try {
                fetchProfile(user.id)
            } catch (_: Exception) {
                val meta = user.userMetadata
                val metaName = meta?.get("full_name")?.jsonPrimitive?.contentOrNull ?: "Estudiante UCV"
                val metaRoleStr = meta?.get("role")?.jsonPrimitive?.contentOrNull ?: "comprador"
                val role = when (metaRoleStr.lowercase()) {
                    "admin" -> UserRole.ADMIN
                    "emprendedor" -> UserRole.EMPRENDEDOR
                    else -> UserRole.COMPRADOR
                }
                UserProfile(
                    id = user.id,
                    fullName = metaName,
                    role = role
                )
            }
            _currentProfile.value = profile
            _isAuthenticated.value = true
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        role: UserRole
    ): Result<UserProfile> {
        val trimmedEmail = email.trim().lowercase()
        if (!isValidInstitutionalEmail(trimmedEmail)) {
            return Result.failure(
                IllegalArgumentException("Debes usar tu correo institucional UCV (@ucvvirtual.edu.pe o @ucv.edu.pe)")
            )
        }

        return try {
            auth.signUpWith(Email) {
                this.email = trimmedEmail
                this.password = password
                this.data = buildJsonObject {
                    put("full_name", fullName.trim())
                    put("phone", phone.trim())
                    put("role", role.name.lowercase())
                    put("campus", "Los Olivos")
                }
            }
            val userId = auth.currentUserOrNull()?.id
                ?: throw IllegalStateException("No se pudo obtener el ID del usuario tras registrarse")

            val profile = try {
                fetchProfile(userId)
            } catch (_: Exception) {
                UserProfile(
                    id = userId,
                    fullName = fullName.trim(),
                    phone = phone.trim(),
                    role = role
                )
            }

            _currentProfile.value = profile
            _isAuthenticated.value = true
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            _currentProfile.value = null
            _isAuthenticated.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshProfile(): Result<UserProfile?> {
        val user = auth.currentUserOrNull()
            ?: return Result.success(null)

        return try {
            val profile = fetchProfile(user.id)
            _currentProfile.value = profile
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchProfile(userId: String): UserProfile {
        return postgrest.from("profiles")
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingle<UserProfile>()
    }
}