package com.example.vallego.domain.repository

import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.CampusMetrics
import com.example.vallego.domain.model.SellerApplication
import kotlinx.coroutines.flow.Flow

interface AdminRepository {
    fun observeMeetingPoints(): Flow<List<CampusMeetingPoint>>
    suspend fun createMeetingPoint(meetingPoint: CampusMeetingPoint): Result<CampusMeetingPoint>
    suspend fun toggleMeetingPoint(id: String, active: Boolean): Result<CampusMeetingPoint>

    fun observeSellerApplications(): Flow<List<SellerApplication>>
    suspend fun approveSellerApplication(applicationId: String): Result<Unit>
    suspend fun rejectSellerApplication(applicationId: String, reason: String): Result<Unit>

    fun observeCampusMetrics(): Flow<CampusMetrics>
}