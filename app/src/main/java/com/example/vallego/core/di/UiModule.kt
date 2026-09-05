package com.example.vallego.core.di

import com.example.vallego.features.auth.AuthViewModel
import com.example.vallego.features.cart.CartViewModel
import com.example.vallego.features.seller.SellerDashboardViewModel
import com.example.vallego.features.tracking.OrderTrackingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val uiModule = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::CartViewModel)
    viewModelOf(::SellerDashboardViewModel)
    viewModelOf(::OrderTrackingViewModel)
}
