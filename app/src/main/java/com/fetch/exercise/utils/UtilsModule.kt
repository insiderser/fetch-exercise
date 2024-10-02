package com.fetch.exercise.utils

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface UtilsModule {

    companion object {

        @Provides
        fun provideConnectivityManager(
            @ApplicationContext context: Context,
        ): ConnectivityManager = context.getSystemService()!!
    }
}
