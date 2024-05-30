package ai.asleep.asleep_sdk_android_sampleapp.di

import ai.asleep.asleep_sdk_android_sampleapp.service.AsleepViewModel
import ai.asleep.asleep_sdk_android_sampleapp.ui.MainViewModel
import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ViewModelModule {

    @Provides
    fun provideMainViewModel(
        applicationContext: Application
    ): MainViewModel {
        return MainViewModel(applicationContext)
    }

    @Provides
    fun provideAsleepViewModel(
        applicationContext: Application
    ): AsleepViewModel {
        return AsleepViewModel(applicationContext)
    }

}