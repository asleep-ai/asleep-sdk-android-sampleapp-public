package ai.asleep.asleep_sdk_android_sampleapp.di

import ai.asleep.asleep_sdk_android_sampleapp.service.AsleepViewModel
import ai.asleep.asleep_sdk_android_sampleapp.ui.report.ReportViewModel
import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ViewModelModule {

    /**
     * Service-side ViewModel, injected into AsleepService in the :RecordingService process.
     */
    @Provides
    fun provideAsleepViewModel(
        applicationContext: Application
    ): AsleepViewModel {
        return AsleepViewModel(applicationContext)
    }

    /**
     * UI-side ViewModel, injected into MainActivity in the main process.
     */
    @Provides
    fun provideMainAsleepViewModel(
        applicationContext: Application
    ): ai.asleep.asleep_sdk_android_sampleapp.ui.main.AsleepViewModel {
        return ai.asleep.asleep_sdk_android_sampleapp.ui.main.AsleepViewModel(applicationContext)
    }

    @Provides
    fun provideReportViewModel(
        applicationContext: Application
    ): ReportViewModel = ReportViewModel(applicationContext)
}
