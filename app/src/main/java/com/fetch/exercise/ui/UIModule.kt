package com.fetch.exercise.ui

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
object UIModule {

    @Provides
    fun createCircuit(
        presenterFactories: Set<@JvmSuppressWildcards Presenter.Factory>,
        uiFactories: Set<@JvmSuppressWildcards Ui.Factory>,
    ): Circuit = Circuit.Builder()
        .addUiFactories(uiFactories)
        .addPresenterFactories(presenterFactories)
        .build()
}
