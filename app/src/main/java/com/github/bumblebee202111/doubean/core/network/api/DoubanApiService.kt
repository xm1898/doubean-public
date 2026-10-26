package com.github.bumblebee202111.doubean.core.network.api

import com.github.bumblebee202111.doubean.core.network.DoubanThrottledException
import com.github.bumblebee202111.doubean.core.network.model.DtoChartItem
import com.github.bumblebee202111.doubean.core.network.model.DtoRecommendSubject
import com.github.bumblebee202111.doubean.core.network.model.DtoSearchSubject
import com.github.bumblebee202111.doubean.core.network.model.DtoSuggestItem
import com.github.bumblebee202111.doubean.core.network.model.NetworkCollectionItems
import com.github.bumblebee202111.doubean.core.network.model.NetworkCredits
import com.github.bumblebee202111.doubean.core.network.model.NetworkPhotos
import com.github.bumblebee202111.doubean.core.network.model.NetworkSubjectDetail
import com.github.bumblebee202111.doubean.core.network.model.NetworkTagSubjectsResponse
import com.github.bumblebee202111.doubean.core.network.model.NetworkTagsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.ContentConvertException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 豆瓣 Web / rexxar 接口封装。
 *
 * rexxar 接口（m.douban.com）必须携带对应频道的 Referer，否则返回 403/空。
 */
@Singleton
class DoubanApiService @Inject constructor(
    private val client: HttpClient,
) {

    private companion object {
        const val REXXAR_BASE = "https://m.douban.com/rexxar/api/v2"
        const val MOVIE_WEB = "https://movie.douban.com"
    }

    private fun rexxarReferer(type: String): String =
        if (type == "tv") "https://m.douban.com/tv/" else "https://m.douban.com/movie/"

    /* ------------------------- 详情 ------------------------- */

    suspend fun getSubjectDetail(type: String, subjectId: String): NetworkSubjectDetail =
        client.get("$REXXAR_BASE/$type/$subjectId") {
            header("Referer", rexxarReferer(type))
        }.body()

    suspend fun getCredits(type: String, subjectId: String): NetworkCredits =
        client.get("$REXXAR_BASE/$type/$subjectId/credits") {
            header("Referer", rexxarReferer(type))
        }.body()

    suspend fun getPhotos(
        type: String,
        subjectId: String,
        start: Int = 0,
        count: Int = 20,
    ): NetworkPhotos =
        client.get("$REXXAR_BASE/$type/$subjectId/photos") {
            header("Referer", rexxarReferer(type))
            parameter("start", start)
            parameter("count", count)
        }.body()

    suspend fun getRecommendations(
        type: String,
        subjectId: String,
    ): List<DtoRecommendSubject> =
        client.get("$REXXAR_BASE/$type/$subjectId/recommendations") {
            header("Referer", rexxarReferer(type))
        }.body()

    /* --------------------- 合集 / 首页流 --------------------- */

    /** 例：movie_showing / movie_hot_gaia / tv_hot / tv_airing */
    suspend fun getCollectionItems(
        collectionId: String,
        start: Int = 0,
        count: Int = 20,
    ): NetworkCollectionItems =
        client.get("$REXXAR_BASE/subject_collection/$collectionId/items") {
            header("Referer", "https://m.douban.com/movie/")
            parameter("start", start)
            parameter("count", count)
        }.body()

    /* ------------------------- 榜单 ------------------------- */

    /**
     * @param chartType 11=剧情片 24=喜剧 … 电影榜；电视剧用 电视 分类 id
     */
    suspend fun getChart(
        chartType: Int,
        intervalId: String = "100:90",
        start: Int = 0,
        limit: Int = 20,
    ): List<DtoChartItem> =
        client.get("$MOVIE_WEB/j/chart/top_list") {
            header("Referer", "$MOVIE_WEB/")
            parameter("type", chartType)
            parameter("interval_id", intervalId)
            parameter("action", "")
            parameter("start", start)
            parameter("limit", limit)
        }.body()

    /* ------------------------- 搜索 ------------------------- */

    suspend fun getTags(type: String): List<String> =
        client.get("$MOVIE_WEB/j/search_tags") {
            header("Referer", "$MOVIE_WEB/")
            parameter("type", type)
        }.body<NetworkTagsResponse>().tags

    suspend fun getTagSubjects(
        type: String,
        tag: String,
        pageLimit: Int = 20,
        pageStart: Int = 0,
    ): NetworkTagSubjectsResponse =
        client.get("$MOVIE_WEB/j/search_subjects") {
            header("Referer", "$MOVIE_WEB/")
            parameter("type", type)
            parameter("tag", tag)
            parameter("page_limit", pageLimit)
            parameter("page_start", pageStart)
        }.body()

    /** 「找电影 / 找电视」筛选：tags 形如 "电影,美国,2024" */
    suspend fun newSearchSubjects(
        tags: String,
        sort: String = "U",
        start: Int = 0,
        range: String = "0,10",
    ): List<DtoSearchSubject> {
        val response = client.get("$MOVIE_WEB/j/new_search_subjects") {
            header("Referer", "$MOVIE_WEB/")
            parameter("sort", sort)
            parameter("range", range)
            parameter("tags", tags)
            parameter("start", start)
        }.body<com.github.bumblebee202111.doubean.core.network.model.NetworkSearchSubjectsResponse>()
        // 限流时返回的是 {"msg":"…","r":1}，别当成「没数据」静默吞掉。
        response.msg?.let { throw DoubanThrottledException(it) }
        return response.data
    }

    /**
     * 官方「选电影 / 选剧集」页的信息流，`sort` 取 T(综合)/U(近期热度)/R(首映时间)/S(高分优先)。
     *
     * 只有这个接口的 R 才真的是「按首映时间」；`/j/new_search_subjects` 的 sort=T 是假的。
     * `tags` 是「体裁,地区,年份」逗号串，`scoreRange` 是「评分下限,上限」（如 `8,10`），
     * 两个都由服务端过滤。`selected_categories` 实测不生效（传「类型=国产剧」照样返回日剧韩剧），
     * 所以固定传空对象。
     */
    suspend fun recommendFeed(
        type: String,
        sort: String,
        start: Int,
        count: Int,
        tags: String? = null,
        scoreRange: String = "0,10",
    ): com.github.bumblebee202111.doubean.core.network.model.NetworkRecommendFeed =
        client.get("$REXXAR_BASE/$type/recommend") {
            header("Referer", "https://movie.douban.com/explore")
            parameter("refresh", 0)
            parameter("start", start)
            parameter("count", count)
            parameter("sort", sort)
            parameter("selected_categories", "{}")
            parameter("uncollect", "false")
            parameter("score_range", scoreRange)
            if (!tags.isNullOrBlank()) parameter("tags", tags)
        }.body()

    suspend fun suggest(q: String): List<DtoSuggestItem> =
        try {
            client.get("$MOVIE_WEB/j/subject_suggest") {
                header("Referer", "$MOVIE_WEB/")
                parameter("q", q)
            }.body()
        } catch (e: ContentConvertException) {
            // 正常返回是数组；限流时变成 {"msg":…} 对象，解析成数组会失败。
            throw DoubanThrottledException(e.message ?: "suggest 被限流")
        }
}
