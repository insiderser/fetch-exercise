package com.fetch.exercise.utils

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

suspend inline fun <R> runWithRetry(
    maxRetries: Int = 3,
    retryDelay: Duration = 1.seconds,
    block: () -> Result<R>,
): Result<R> {
    var lastResult = block()
    repeat(maxRetries - 1) {
        if (lastResult.isSuccess) return lastResult
        delay(retryDelay)
        lastResult = block()
    }
    return lastResult
}
