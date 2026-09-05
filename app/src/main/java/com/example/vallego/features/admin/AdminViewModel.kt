package com.example.vallego.features.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vallego.domain.model.CampusMeetingPoint
import com.example.vallego.domain.model.SellerApplication
import com.example.vallego.domain.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class AdminViewModel(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            adminRepository.observeMeetingPoints().collect { points ->
                _uiState.update { it.copy(meetingPoints = points) }
            }
        }
        viewModelScope.launch {
            adminRepository.observeSellerApplications().collect { apps ->
                _uiState.update { it.copy(sellerApplications = apps) }
            }
        }
        viewModelScope.launch {
            adminRepository.observeCampusMetrics().collect { metrics ->
                _uiState.update { it.copy(metrics = metrics) }
            }
        }
    }

    fun setTab(tab: AdminTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun toggleMeetingPoint(pointId: String, currentActive: Boolean) {
        viewModelScope.launch {
            adminRepository.toggleMeetingPoint(pointId, !currentActive)
        }
    }

    fun openCreateMeetingPointDialog() {
        _uiState.update { it.copy(showCreateMeetingPointDialog = true) }
    }

    fun dismissCreateMeetingPointDialog() {
        _uiState.update { it.copy(showCreateMeetingPointDialog = false) }
    }

    fun createMeetingPoint(name: String, pavilion: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val point = CampusMeetingPoint(
                id = "mp-" + UUID.randomUUID().toString().take(6),
                name = name.trim(),
                pavilion = pavilion.trim().ifBlank { null },
                description = description.trim().ifBlank { null },
                isActive = true
            )
            adminRepository.createMeetingPoint(point)
            dismissCreateMeetingPointDialog()
        }
    }

    fun approveApplication(applicationId: String) {
        viewModelScope.launch {
            adminRepository.approveSellerApplication(applicationId)
        }
    }

    fun openRejectionDialog(application: SellerApplication) {
        _uiState.update { it.copy(selectedApplicationForRejection = application) }
    }

    fun dismissRejectionDialog() {
        _uiState.update { it.copy(selectedApplicationForRejection = null) }
    }

    fun confirmRejection(applicationId: String, reason: String) {
        viewModelScope.launch {
            adminRepository.rejectSellerApplication(applicationId, reason)
            dismissRejectionDialog()
        }
    }
}