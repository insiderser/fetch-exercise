@file:OptIn(ExperimentalFoundationApi::class)

package com.fetch.exercise.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.fetch.exercise.R
import com.fetch.exercise.ui.AppScreens
import com.fetch.exercise.ui.theme.FetchExerciseTheme
import com.fetch.exercise.ui.theme.dimensions
import com.slack.circuit.codegen.annotations.CircuitInject
import dagger.hilt.android.components.ActivityComponent
import kotlinx.collections.immutable.persistentListOf

@CircuitInject(AppScreens.List::class, ActivityComponent::class)
@Composable
fun ListUI(state: ListState, modifier: Modifier = Modifier) {
    val eventSink = state.eventSink

    Scaffold(modifier) { innerPadding ->
        when (state.loadState) {
            is ListState.LoadState.Loading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }

            is ListState.LoadState.Success -> {
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    state.loadState.groups.forEach { group ->
                        stickyHeader(key = group.id) {
                            GroupHeader(group)
                        }

                        items(group.items) { item ->
                            ItemContent(item)
                        }
                    }
                }
            }

            is ListState.LoadState.Error -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        stringResource(R.string.list_error_loading),
                        Modifier.padding(horizontal = MaterialTheme.dimensions.padding_x1),
                        textAlign = TextAlign.Center,
                    )

                    Button(
                        onClick = { eventSink(ListUIEvent.TryAgain) },
                        modifier = Modifier.padding(top = MaterialTheme.dimensions.padding_x1),
                    ) {
                        Text(stringResource(R.string.try_again))
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(group: ListState.Group, modifier: Modifier = Modifier) {
    Text(
        group.name,
        modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .padding(
                horizontal = MaterialTheme.dimensions.padding_x1,
                vertical = MaterialTheme.dimensions.padding_x0_5,
            ),
    )
}

@Composable
private fun ItemContent(item: ListState.Item, modifier: Modifier = Modifier) {
    Text(
        item.name,
        modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.dimensions.padding_x0_5)
            .padding(
                start = MaterialTheme.dimensions.padding_x1_5,
                end = MaterialTheme.dimensions.padding_x1,
            ),
    )
}

@Composable
@Preview
private fun ListUIPreviewSuccess() {
    FetchExerciseTheme {
        ListUI(
            ListState(
                loadState = ListState.LoadState.Success(
                    groups = persistentListOf(
                        ListState.Group(
                            id = 1,
                            name = "Group 1",
                            items = persistentListOf(
                                ListState.Item(
                                    id = 1,
                                    name = "Item 1",
                                ),
                                ListState.Item(
                                    id = 2,
                                    name = "Item 2",
                                ),
                            )
                        ),
                        ListState.Group(
                            id = 2,
                            name = "Group 2",
                            items = persistentListOf(
                                ListState.Item(
                                    id = 3,
                                    name = "Item 3",
                                ),
                                ListState.Item(
                                    id = 4,
                                    name = "Item 4",
                                ),
                            )
                        ),
                    ),
                ),
                eventSink = {},
            ),
        )
    }
}

@Composable
@Preview
private fun ListUIPreviewLoading() {
    FetchExerciseTheme {
        ListUI(
            ListState(
                loadState = ListState.LoadState.Loading,
                eventSink = {},
            ),
        )
    }
}

@Composable
@Preview
private fun ListUIPreviewError() {
    FetchExerciseTheme {
        ListUI(
            ListState(
                loadState = ListState.LoadState.Error,
                eventSink = {},
            ),
        )
    }
}
