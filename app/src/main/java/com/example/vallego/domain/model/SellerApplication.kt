package com.example.vallego.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
enum class ApplicationStatus {
    PENDIENTE,
    APROBADA,
    RECHAZADA
}

@Serializable
data class SellerApplication(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("applicant_name") val applicantName: String,
    @SerialName("student_email") val studentEmail: String,
    @SerialName("store_name") val storeName: String,
    @SerialName("category") val category: String,
    @SerialName("description") val description: String,
    @SerialName("proposed_location") val proposedLocation: String,
    @SerialName("status") val status: ApplicationStatus = ApplicationStatus.PENDIENTE,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)