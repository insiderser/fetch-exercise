package com.fetch.exercise.ui.list

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.fetch.exercise.data.FetchAPI
import com.fetch.exercise.ui.AppScreens
import com.fetch.exercise.utils.ObserveIsNetworkAvailable
import com.fetch.exercise.utils.runWithRetry
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.internal.rememberStableCoroutineScope
import com.slack.circuit.runtime.presenter.Presenter
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.components.ActivityComponent
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ListPresenter @AssistedInject constructor(
    // You can inject navigator, screen, or other parameters here.
//    @Assisted private val navigator: Navigator,
//    @Assisted private val screen: AppScreens.List,
    private val fetchAPI: FetchAPI,
    private val observeIsNetworkAvailable: ObserveIsNetworkAvailable,
) : Presenter<ListState> {

    @Composable
    override fun present(): ListState {
        val coroutineScope = rememberStableCoroutineScope()
        val loadMutex = remember { Mutex() }

        var loadState: ListState.LoadState by rememberRetained { mutableStateOf(ListState.LoadState.Loading) }

        suspend fun refresh() {
            loadState = ListState.LoadState.Loading
            runWithRetry { runCatching { fetchAPI.getItems() } }
                .onSuccess { items ->
                    val groups = items
                        .groupBy { it.listId }
                        .map { (groupId, items) ->
                            ListState.Group(
                                id = groupId,
                                name = groupId.toString(), // The API response doesn't have group names, so I'm using the group ID as the name.
                                items = items.mapNotNull { item ->
                                    ListState.Item(
                                        id = item.id,
                                        name = item.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null,
                                    )
                                }.sortedBy { it.name }.toPersistentList(),
                            )
                        }
                        .filter { it.items.isNotEmpty() }
                        .sortedBy { it.id }
                        .toPersistentList()
                    loadState = ListState.LoadState.Success(groups)
                }
                .onFailure { e ->
                    // Should disable logging in production builds.
                    Log.e("ListPresenter", "Failed to fetch items", e)
                    loadState = ListState.LoadState.Error
                }
        }

        LaunchedEffect(Unit) {
            loadMutex.withLock {
                if (loadState is ListState.LoadState.Loading) {
                    refresh()
                }
            }

            observeIsNetworkAvailable().collectLatest { isAvailable ->
                loadMutex.withLock {
                    if (isAvailable && loadState is ListState.LoadState.Error) {
                        refresh()
                    }
                }
            }
        }

        fun eventSink(event: ListUIEvent) {
            when (event) {
                is ListUIEvent.TryAgain -> {
                    coroutineScope.launch {
                        loadMutex.withLock {
                            if (loadState is ListState.LoadState.Error) {
                                refresh()
                            }
                        }
                    }
                }
            }
        }

        return ListState(
            loadState = loadState,
            eventSink = ::eventSink,
        )
    }

    @CircuitInject(AppScreens.List::class, ActivityComponent::class)
    @AssistedFactory
    fun interface Factory {
        fun create(
//            navigator: Navigator,
        ): ListPresenter
    }
}
