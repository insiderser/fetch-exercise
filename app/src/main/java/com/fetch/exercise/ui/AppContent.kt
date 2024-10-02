package com.fetch.exercise.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.fetch.exercise.ui.theme.FetchExerciseTheme
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@AssistedFactory
fun interface AppContentFactory {
    fun create(
        // You can have activity or other parameters here.
//        activity: Activity,
    ): AppContent
}

@Stable
class AppContent @AssistedInject constructor(
//    @Assisted private val activity: Activity,
    private val circuit: Circuit,
) {

    @Composable
    fun Content() {
        val backStack = rememberSaveableBackStack(AppScreens.List)
        val navigator = rememberCircuitNavigator(backStack)

        CircuitCompositionLocals(circuit) {
            FetchExerciseTheme {
                NavigableCircuitContent(
                    navigator = navigator,
                    backStack = backStack,
                    circuit = circuit,
                )
            }
        }
    }
}
