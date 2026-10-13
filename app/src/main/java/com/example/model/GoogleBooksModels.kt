package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Top-level response returned by Google Books API volumes search endpoint:
 * https://www.googleapis.com/books/v1/volumes?q={query}
 */
@JsonClass(generateAdapter = true)
data class GoogleBooksResponse(
    @Json(name = "kind") val kind: String? = null,
    @Json(name = "totalItems") val totalItems: Int = 0,
    @Json(name = "items") val items: List<GoogleBookItem>? = null
)

/**
 * Individual volume/book item returned by Google Books API.
 */
@JsonClass(generateAdapter = true)
data class GoogleBookItem(
    @Json(name = "kind") val kind: String? = null,
    @Json(name = "id") val id: String,
    @Json(name = "etag") val etag: String? = null,
    @Json(name = "selfLink") val selfLink: String? = null,
    @Json(name = "volumeInfo") val volumeInfo: VolumeInfo? = null,
    @Json(name = "saleInfo") val saleInfo: SaleInfo? = null,
    @Json(name = "accessInfo") val accessInfo: AccessInfo? = null,
    @Json(name = "searchInfo") val searchInfo: SearchInfo? = null
)

/**
 * Detailed volume metadata including title, authors, publisher, page count, categories, etc.
 */
@JsonClass(generateAdapter = true)
data class VolumeInfo(
    @Json(name = "title") val title: String = "",
    @Json(name = "subtitle") val subtitle: String? = null,
    @Json(name = "authors") val authors: List<String>? = null,
    @Json(name = "publisher") val publisher: String? = null,
    @Json(name = "publishedDate") val publishedDate: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "industryIdentifiers") val industryIdentifiers: List<IndustryIdentifier>? = null,
    @Json(name = "readingModes") val readingModes: ReadingModes? = null,
    @Json(name = "pageCount") val pageCount: Int? = null,
    @Json(name = "printedPageCount") val printedPageCount: Int? = null,
    @Json(name = "printType") val printType: String? = null,
    @Json(name = "categories") val categories: List<String>? = null,
    @Json(name = "averageRating") val averageRating: Double? = null,
    @Json(name = "ratingsCount") val ratingsCount: Int? = null,
    @Json(name = "maturityRating") val maturityRating: String? = null,
    @Json(name = "allowAnonLogging") val allowAnonLogging: Boolean? = null,
    @Json(name = "contentVersion") val contentVersion: String? = null,
    @Json(name = "panelizationSummary") val panelizationSummary: PanelizationSummary? = null,
    @Json(name = "imageLinks") val imageLinks: ImageLinks? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "previewLink") val previewLink: String? = null,
    @Json(name = "infoLink") val infoLink: String? = null,
    @Json(name = "canonicalVolumeLink") val canonicalVolumeLink: String? = null
)

@JsonClass(generateAdapter = true)
data class IndustryIdentifier(
    @Json(name = "type") val type: String? = null, // e.g. "ISBN_10", "ISBN_13", "OTHER"
    @Json(name = "identifier") val identifier: String? = null
)

@JsonClass(generateAdapter = true)
data class ReadingModes(
    @Json(name = "text") val text: Boolean = false,
    @Json(name = "image") val image: Boolean = false
)

@JsonClass(generateAdapter = true)
data class PanelizationSummary(
    @Json(name = "containsEpubBubbles") val containsEpubBubbles: Boolean? = null,
    @Json(name = "containsImageBubbles") val containsImageBubbles: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class ImageLinks(
    @Json(name = "smallThumbnail") val smallThumbnail: String? = null,
    @Json(name = "thumbnail") val thumbnail: String? = null,
    @Json(name = "small") val small: String? = null,
    @Json(name = "medium") val medium: String? = null,
    @Json(name = "large") val large: String? = null,
    @Json(name = "extraLarge") val extraLarge: String? = null
)

@JsonClass(generateAdapter = true)
data class SaleInfo(
    @Json(name = "country") val country: String? = null,
    @Json(name = "saleability") val saleability: String? = null, // e.g. "FOR_SALE", "NOT_FOR_SALE", "FREE"
    @Json(name = "isEbook") val isEbook: Boolean? = null,
    @Json(name = "listPrice") val listPrice: PriceInfo? = null,
    @Json(name = "retailPrice") val retailPrice: PriceInfo? = null,
    @Json(name = "buyLink") val buyLink: String? = null
)

@JsonClass(generateAdapter = true)
data class PriceInfo(
    @Json(name = "amount") val amount: Double? = null,
    @Json(name = "currencyCode") val currencyCode: String? = null
)

@JsonClass(generateAdapter = true)
data class AccessInfo(
    @Json(name = "country") val country: String? = null,
    @Json(name = "viewability") val viewability: String? = null,
    @Json(name = "embeddable") val embeddable: Boolean? = null,
    @Json(name = "publicDomain") val publicDomain: Boolean? = null,
    @Json(name = "textToSpeechPermission") val textToSpeechPermission: String? = null,
    @Json(name = "epub") val epub: FormatAvailability? = null,
    @Json(name = "pdf") val pdf: FormatAvailability? = null,
    @Json(name = "webReaderLink") val webReaderLink: String? = null,
    @Json(name = "accessViewStatus") val accessViewStatus: String? = null,
    @Json(name = "quoteSharingAllowed") val quoteSharingAllowed: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class FormatAvailability(
    @Json(name = "isAvailable") val isAvailable: Boolean = false,
    @Json(name = "acsTokenLink") val acsTokenLink: String? = null,
    @Json(name = "downloadLink") val downloadLink: String? = null
)

@JsonClass(generateAdapter = true)
data class SearchInfo(
    @Json(name = "textSnippet") val textSnippet: String? = null
)
