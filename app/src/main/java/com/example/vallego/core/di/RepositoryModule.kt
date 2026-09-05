package com.example.vallego.core.di

import com.example.vallego.data.repository.AuthRepositoryImpl
import com.example.vallego.data.repository.CartRepositoryImpl
import com.example.vallego.data.repository.OrderRepositoryImpl
import com.example.vallego.domain.repository.AuthRepository
import com.example.vallego.domain.repository.CartRepository
import com.example.vallego.domain.repository.OrderRepository
import com.example.vallego.domain.usecase.CalculateCartUseCase
import com.example.vallego.domain.usecase.CreateOrderWithSubordersUseCase
import com.example.vallego.domain.usecase.RecalculateOrderUseCase
import org.koin.dsl.module

val repositoryModule = module {
    single<AuthRepository> {
        AuthRepositoryImpl(
            auth = get(),
            postgrest = get()
        )
    }

    single { CalculateCartUseCase() }
    single { CreateOrderWithSubordersUseCase() }
    single { RecalculateOrderUseCase() }

    single<CartRepository> {
        CartRepositoryImpl(
            calculateCartUseCase = get()
        )
    }

    single<OrderRepository> {
        OrderRepositoryImpl(
            postgrest = get()
        )
    }
}
