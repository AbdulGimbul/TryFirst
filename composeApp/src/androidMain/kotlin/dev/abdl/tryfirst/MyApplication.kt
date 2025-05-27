package dev.abdl.tryfirst

import android.app.Application
import dev.abdl.tryfirst.di.initKoin
import org.koin.android.ext.koin.androidContext

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        multiplatform.network.cmptoast.AppContext.apply { set(applicationContext) }
        initKoin {
            androidContext(this@MyApplication)
        }
    }
}