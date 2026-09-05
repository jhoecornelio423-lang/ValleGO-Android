package com.example.vallego.core.di

import com.example.vallego.data.repository.AuthRepositoryImpl
import com.example.vallego.domain.repository.AuthRepository
import org.koin.dsl.module

val repositoryModule = module {
    single<AuthRepository> {
        AuthRepositoryImpl(
            auth = get(),
            postgrest = get()
        )
    }
}
