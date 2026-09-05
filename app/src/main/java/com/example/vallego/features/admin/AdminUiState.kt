package com.example.vallego.features.admin

import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.CampusMetrics
import com.example.vallego.domain.model.SellerApplication

enum class AdminTab {
    MEETING_POINTS,
    SELLER_APPLICATIONS,
    CAMPUS_METRICS
}

data class AdminUiState(
    val selectedTab: AdminTab = AdminTab.MEETING_POINTS,
    val meetingPoints: List<CampusMeetingPoint> = emptyList(),
    val sellerApplications: List<SellerApplication> = emptyList(),
    val metrics: CampusMetrics = CampusMetrics(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showCreateMeetingPointDialog: Boolean = false,
    val selectedApplicationForRejection: SellerApplication? = null
)