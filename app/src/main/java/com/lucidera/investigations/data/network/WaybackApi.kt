package com.lucidera.investigations.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface WaybackApi {

    @GET("wayback/available")
    suspend fun lookupAvailability(
        @Query("url") url: String
    ): WaybackAvailabilityResponse
}

data class WaybackAvailabilityResponse(
    @SerializedName("archived_snapshots")
    val archivedSnapshots: ArchivedSnapshots
)

data class ArchivedSnapshots(
    val closest: WaybackSnapshot?
)

data class WaybackSnapshot(
    val available: Boolean,
    val status: String,
    val timestamp: String,
    val url: String
)

data class WaybackLookupResult(
    val originalUrl: String,
    val archiveUrl: String,
    val timestamp: String,
    val available: Boolean,
    val status: String
)
