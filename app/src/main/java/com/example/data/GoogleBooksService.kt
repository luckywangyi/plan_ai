package com.example.data

import com.example.model.GoogleBooksResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for Google Books API v1.
 * Base URL: https://www.googleapis.com/books/v1/
 */
interface GoogleBooksService {

    /**
     * Performs a book search using the volumes query endpoint.
     *
     * @param query Search keywords or filters (e.g. "intitle:algorithms", "isbn:9780262033848", "computer science").
     * @param startIndex Zero-based index of the first book to return (for pagination).
     * @param maxResults Maximum number of results to return (up to 40, defaults to 10 or 20).
     * @param printType Filter by print type (e.g., "all", "books", "magazines").
     * @param orderBy Order results by "relevance" or "newest".
     * @param langRestrict Restrict results to two-letter ISO-639-1 language code (e.g., "en", "zh").
     * @param apiKey Optional API key for quota extension / authentication.
     */
    @GET("volumes")
    suspend fun searchVolumes(
        @Query("q") query: String,
        @Query("startIndex") startIndex: Int? = null,
        @Query("maxResults") maxResults: Int? = null,
        @Query("printType") printType: String? = null,
        @Query("orderBy") orderBy: String? = null,
        @Query("langRestrict") langRestrict: String? = null,
        @Query("key") apiKey: String? = null
    ): GoogleBooksResponse
}
