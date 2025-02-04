package ai.asleep.asleep_sdk_android_sampleapp.di

import ai.asleep.asleep_sdk_android_sampleapp.ui.main.AsleepViewModel
import ai.asleep.asleep_sdk_android_sampleapp.ui.report.ReportViewModel
import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ViewModelModule {

    @Provides
    fun provideAsleepViewModel(
        applicationContext: Application
    ): AsleepViewModel {
        return AsleepViewModel(applicationContext)
    }

    @Provides
    fun provideReportViewModel(
        applicationContext: Application
    ): ReportViewModel = ReportViewModel(applicationContext)
}