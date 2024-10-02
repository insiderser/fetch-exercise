package com.fetch.exercise.ui

import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize

interface AppScreen : Screen

object AppScreens {

    @Parcelize
    data object List : AppScreen
}
