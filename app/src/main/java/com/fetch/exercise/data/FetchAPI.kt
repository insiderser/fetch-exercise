package com.fetch.exercise.data

import kotlinx.serialization.Serializable
import retrofit2.http.GET

interface FetchAPI {

    @GET("hiring.json")
    suspend fun getItems(): List<FetchItemResponse>
}

@Serializable
data class FetchItemResponse(
    val id: Int,
    val listId: Int,
    val name: String? = null,
)
