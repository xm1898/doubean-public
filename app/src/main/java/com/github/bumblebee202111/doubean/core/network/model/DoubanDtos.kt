package com.github.bumblebee202111.doubean.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* ============================ 通用 ============================ */

@Serializable
data class NetworkApiError(
    val code: Int = 0,
    val message: String = "",
    val localizedMessage: String? = null,
    val request: String? = null,
    val extra: NetworkApiErrorExtra? = null,
)

@Serializable
data class NetworkApiErrorExtra(
    val solutionUri: String? = null,
)

@Serializable
data class DtoRating(
    val count: Int? = null,
    val max: Int = 10,
    val value: Double? = null,
    @SerialName("star_count") val starCount: Double? = null,
    @SerialName("null_rating_reason") val nullRatingReason: String? = null,
)

@Serializable
data class DtoImage(
    val large: String? = null,
    val normal: String? = null,
    val small: String? = null,
)

@Serializable
data class DtoCover(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val shape: String? = null,
)

@Serializable
data class DtoActor(
    val id: String? = null,
    val name: String? = null,
    val character: String? = null,
    @SerialName("latin_name") val latinName: String? = null,
    val avatar: DtoImage? = null,
    val roles: List<String> = emptyList(),
)

@Serializable
data class DtoTrailer(
    val id: String? = null,
    val title: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    val runtime: String? = null,
)

@Serializable
data class DtoVendor(
    val id: String? = null,
    val title: String? = null,
    val url: String? = null,
    val uri: String? = null,
    val icon: String? = null,
    @SerialName("gray_icon") val grayIcon: String? = null,
    @SerialName("payment_desc") val paymentDesc: String? = null,
)

/* ============================ 详情 ============================ */

@Serializable
data class NetworkSubjectDetail(
    val id: String = "",
    val title: String = "",
    @SerialName("original_title") val originalTitle: String? = null,
    val year: String? = null,
    val type: String? = null,
    val subtype: String? = null,
    val intro: String? = null,
    @SerialName("card_subtitle") val cardSubtitle: String? = null,
    val rating: DtoRating? = null,
    val pic: DtoImage? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("is_released") val isReleased: Boolean = false,
    @SerialName("vendor_count") val vendorCount: Int = 0,
    val vendors: List<DtoVendor> = emptyList(),
    val pubdate: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val durations: List<String> = emptyList(),
    val actors: List<DtoActor> = emptyList(),
    val directors: List<DtoActor> = emptyList(),
    val trailers: List<DtoTrailer> = emptyList(),
    @SerialName("episodes_count") val episodesCount: Int = 0,
    val uri: String? = null,
    val url: String? = null,
)

@Serializable
data class NetworkCredits(
    val items: List<DtoActor> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class NetworkPhotos(
    val start: Int = 0,
    val count: Int = 0,
    val total: Int = 0,
    val photos: List<DtoPhotoItem> = emptyList(),
)

@Serializable
data class DtoPhotoItem(
    val id: String? = null,
    val image: DtoSizedImage? = null,
    val description: String? = null,
)

/** 剧照里的 image.large/normal 是对象，不是字符串 */
@Serializable
data class DtoSizedImage(
    val large: DtoImageItem? = null,
    val normal: DtoImageItem? = null,
    val small: DtoImageItem? = null,
    @SerialName("primary_color") val primaryColor: String? = null,
)

@Serializable
data class DtoImageItem(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val size: Long? = null,
)

/* ============================ 列表 / 合集 ============================ */

@Serializable
data class NetworkCollectionItems(
    val start: Int = 0,
    val count: Int = 0,
    val total: Int = 0,
    @SerialName("subject_collection_items") val items: List<DtoCollectionSubject> = emptyList(),
)

@Serializable
data class DtoCollectionSubject(
    val id: String = "",
    val title: String = "",
    val type: String? = null,
    val subtype: String? = null,
    val info: String? = null,
    @SerialName("card_subtitle") val cardSubtitle: String? = null,
    val cover: DtoCover? = null,
    val pic: DtoImage? = null,
    val rating: DtoRating? = null,
    val url: String? = null,
    val uri: String? = null,
)

/** /rexxar/api/v2/{movie,tv}/{id}/recommendations 返回裸数组 */
@Serializable
data class DtoRecommendSubject(
    val id: String = "",
    val title: String = "",
    val type: String? = null,
    @SerialName("card_subtitle") val cardSubtitle: String? = null,
    val pic: DtoImage? = null,
    val rating: DtoRating? = null,
    val uri: String? = null,
    val url: String? = null,
    /** 信息流里会混进榜单卡（`chart`）之类的非条目卡，只有 `subject` 才是能打开的条目。 */
    val card: String? = null,
)

/** /rexxar/api/v2/{movie|tv}/recommend 的排序项：T=综合 U=近期热度 R=首映时间 S=高分优先 */
@Serializable
data class DtoSortOption(
    val text: String = "",
    val name: String = "",
    val checked: Boolean = false,
)

/** /rexxar/api/v2/{movie|tv}/recommend 信息流（官方「选电影/选剧集」页用的就是它）。 */
@Serializable
data class NetworkRecommendFeed(
    val sorts: List<DtoSortOption> = emptyList(),
    val items: List<DtoRecommendSubject> = emptyList(),
)

/** /j/chart/top_list 榜单条目 */
@Serializable
data class DtoChartItem(
    val id: String = "",
    val title: String = "",
    val rank: Int = 0,
    val rating: List<String> = emptyList(),
    @SerialName("cover_url") val coverUrl: String? = null,
    val url: String? = null,
    val types: List<String> = emptyList(),
    val regions: List<String> = emptyList(),
    @SerialName("is_playable") val isPlayable: Boolean? = null,
)

/** /j/new_search_subjects 筛选结果 */
@Serializable
data class NetworkSearchSubjectsResponse(
    val data: List<DtoSearchSubject> = emptyList(),
    /** 被限流时豆瓣返回 `{"msg":"…","r":1}`，没有 data 字段。 */
    val msg: String? = null,
    val r: Int? = null,
)

@Serializable
data class DtoSearchSubject(
    val id: String = "",
    val title: String = "",
    val url: String? = null,
    val cover: String? = null,
    val rate: String? = null,
    val star: String? = null,
    val directors: List<String> = emptyList(),
    val casts: List<String> = emptyList(),
)

/** /j/search_subjects 标签结果 */
@Serializable
data class NetworkTagSubjectsResponse(
    val subjects: List<DtoTagSubject> = emptyList(),
)

@Serializable
data class DtoTagSubject(
    val id: String = "",
    val title: String = "",
    val rate: String? = null,
    val cover: String? = null,
    val url: String? = null,
    val playable: Boolean? = null,
    @SerialName("is_new") val isNew: Boolean? = null,
    @SerialName("episodes_info") val episodesInfo: String? = null,
)

@Serializable
data class NetworkTagsResponse(
    val tags: List<String> = emptyList(),
)

/** /j/subject_suggest 搜索建议 */
@Serializable
data class DtoSuggestItem(
    val id: String = "",
    val title: String = "",
    val url: String? = null,
    val img: String? = null,
    val type: String? = null,
    val year: String? = null,
    val episode: String? = null,
)
