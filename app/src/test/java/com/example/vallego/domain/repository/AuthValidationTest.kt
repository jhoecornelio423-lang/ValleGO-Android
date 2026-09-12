package com.example.vallego.domain.repository

import com.example.vallego.domain.model.UserRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    private fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isNotEmpty() && emailRegex.matches(trimmed)
    }

    @Test
    fun testValidEmailsBothStandardAndInstitutional() {
        // Correos institucionales UCV siguen siendo válidos
        assertTrue(isValidEmail("estudiante@ucvvirtual.edu.pe"))
        assertTrue(isValidEmail("profesor@ucv.edu.pe"))
        assertTrue(isValidEmail("  JUAN.PEREZ@UCVVIRTUAL.EDU.PE  "))
        assertTrue(isValidEmail("admin@ucv.edu.pe"))

        // Correos comerciales y generales ahora son válidos (para negocios de alrededores o usuarios generales)
        assertTrue(isValidEmail("usuario@gmail.com"))
        assertTrue(isValidEmail("negocio.alrededor@outlook.com"))
        assertTrue(isValidEmail("tiendita@yahoo.es"))
        assertTrue(isValidEmail("vendedor_externo123@empresa.com.pe"))
    }

    @Test
    fun testInvalidEmailsMalformed() {
        assertFalse(isValidEmail(""))
        assertFalse(isValidEmail("   "))
        assertFalse(isValidEmail("correo_sin_arroba"))
        assertFalse(isValidEmail("usuario@"))
        assertFalse(isValidEmail("@dominio.com"))
        assertFalse(isValidEmail("usuario@dominio"))
    }

    @Test
    fun testAuthUiStateStandardEmailValidation() {
        val validLoginState = com.example.vallego.features.auth.AuthUiState(
            email = "negocio@gmail.com",
            password = "password123",
            isLoginMode = true
        )
        assertTrue(validLoginState.isEmailValid)
        assertTrue(validLoginState.canSubmit)

        val validRegisterState = com.example.vallego.features.auth.AuthUiState(
            email = "vendedor.cercano@hotmail.com",
            password = "password123",
            fullName = "Don Pepe Delivery",
            phone = "987654321",
            isLoginMode = false
        )
        assertTrue(validRegisterState.isEmailValid)
        assertTrue(validRegisterState.canSubmit)

        val invalidState = com.example.vallego.features.auth.AuthUiState(
            email = "invalido",
            password = "password123"
        )
        assertFalse(invalidState.isEmailValid)
        assertFalse(invalidState.canSubmit)
    }

    @Test
    fun testUserRolePermissions() {
        // Buyer
        assertTrue(UserRole.COMPRADOR.canBuy)
        assertFalse(UserRole.COMPRADOR.canSell)
        assertFalse(UserRole.COMPRADOR.isAdmin)
        assertFalse(UserRole.COMPRADOR.isSuspended)

        // Entrepreneur
        assertTrue(UserRole.EMPRENDEDOR.canBuy)
        assertTrue(UserRole.EMPRENDEDOR.canSell)
        assertFalse(UserRole.EMPRENDEDOR.isAdmin)
        assertFalse(UserRole.EMPRENDEDOR.isSuspended)

        // Admin
        assertTrue(UserRole.ADMIN.canBuy)
        assertFalse(UserRole.ADMIN.canSell)
        assertTrue(UserRole.ADMIN.isAdmin)
        assertFalse(UserRole.ADMIN.isSuspended)

        // Suspended
        assertTrue(UserRole.SUSPENDED.isSuspended)
        assertTrue(UserRole.SUSPENDED_BUYER.isSuspended)
    }
}
