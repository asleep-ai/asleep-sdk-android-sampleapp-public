package ai.asleep.asleep_sdk_android_sampleapp.di

import ai.asleep.asleep_sdk_android_sampleapp.ui.AsleepViewModel
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
    @Singleton
    fun provideAsleepViewModel(
        applicationContext: Application
    ): AsleepViewModel {
        return AsleepViewModel(applicationContext)
    }
}