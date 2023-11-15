package ai.asleep.asleep_sdk_android_sampleapp.di

import ai.asleep.asleep_sdk_android_sampleapp.ui.MainViewModel
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
    fun provideMainViewModel(
    ): MainViewModel {
        return MainViewModel()
    }


}