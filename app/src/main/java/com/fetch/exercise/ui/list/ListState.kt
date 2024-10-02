package com.fetch.exercise.ui.list

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class ListState(
    val loadState: LoadState,

    // You can use this eventSink to send events from UI to the presenter.
    val eventSink: (ListUIEvent) -> Unit,
) : CircuitUiState {

    @Immutable
    sealed interface LoadState {
        data object Loading : LoadState
        data class Success(val groups: ImmutableList<Group>) : LoadState
        data object Error : LoadState
    }

    @Immutable
    data class Group(
        val id: Int,
        val name: String,
        val items: ImmutableList<Item>,
    )

    @Immutable
    data class Item(
        val id: Int,
        val name: String,
    )
}

@Immutable
sealed interface ListUIEvent {
    // Add events here.

    data object TryAgain : ListUIEvent
}
