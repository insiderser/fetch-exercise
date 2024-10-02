package com.fetch.exercise.data

import kotlinx.coroutines.delay
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class FakeFetchAPI : FetchAPI {

    private var itemsResult: NetworkResult<List<FetchItemResponse>> = NetworkResult.NoInternet

    override suspend fun getItems(): List<FetchItemResponse> {
        return itemsResult.execute()
    }

    fun setItemsResult(result: NetworkResult<List<FetchItemResponse>>) {
        itemsResult = result
    }
}

sealed interface NetworkResult<out T> {
    suspend fun execute(): T

    data class Success<T>(
        val data: T,
        val delay: Duration = 2.seconds,
    ) : NetworkResult<T> {
        override suspend fun execute(): T {
            delay(delay)
            return data
        }
    }

    data object NoInternet : NetworkResult<Nothing> {
        override suspend fun execute(): Nothing {
            throw UnknownHostException("No internet")
        }
    }

    data object Timeout : NetworkResult<Nothing> {
        override suspend fun execute(): Nothing {
            delay(30.seconds)
            throw SocketTimeoutException("Timeout")
        }
    }

    data object ServerDown : NetworkResult<Nothing> {
        override suspend fun execute(): Nothing {
            throw HttpException(Response.error<Any>(500, "".toResponseBody(null)))
        }
    }

    data class Error(
        val error: Throwable,
    ) : NetworkResult<Nothing> {
        override suspend fun execute(): Nothing {
            throw error
        }
    }
}
