package com.example.vallego.domain.repository

import com.example.vallego.domain.model.UserRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {

    private fun isValidInstitutionalEmail(email: String): Boolean {
        val trimmed = email.trim().lowercase()
        return trimmed.endsWith("@ucv.edu.pe") || trimmed.endsWith("@ucvvirtual.edu.pe")
    }

    @Test
    fun testValidInstitutionalEmails() {
        assertTrue(isValidInstitutionalEmail("estudiante@ucvvirtual.edu.pe"))
        assertTrue(isValidInstitutionalEmail("profesor@ucv.edu.pe"))
        assertTrue(isValidInstitutionalEmail("  JUAN.PEREZ@UCVVIRTUAL.EDU.PE  "))
        assertTrue(isValidInstitutionalEmail("admin@ucv.edu.pe"))
    }

    @Test
    fun testInvalidNonInstitutionalEmails() {
        assertFalse(isValidInstitutionalEmail("usuario@gmail.com"))
        assertFalse(isValidInstitutionalEmail("usuario@outlook.com"))
        assertFalse(isValidInstitutionalEmail("hacker@fakeucv.edu.pe.attacker.com"))
        assertFalse(isValidInstitutionalEmail("estudiante@ucv.pe"))
        assertFalse(isValidInstitutionalEmail(""))
        assertFalse(isValidInstitutionalEmail("   "))
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
