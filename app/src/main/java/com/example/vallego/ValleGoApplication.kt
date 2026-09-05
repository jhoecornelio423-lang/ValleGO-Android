package com.example.vallego

import android.app.Application
import com.example.vallego.core.di.networkModule
import com.example.vallego.core.di.repositoryModule
import com.example.vallego.core.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class ValleGoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@ValleGoApplication)
            modules(networkModule, repositoryModule, uiModule)
        }
    }
}