package com.github.bumblebee202111.doubean.data.subject

import com.github.bumblebee202111.doubean.core.network.api.DoubanApiService
import com.github.bumblebee202111.doubean.model.ApiError
import com.github.bumblebee202111.doubean.model.AppError
import com.github.bumblebee202111.doubean.model.AppResult
import com.github.bumblebee202111.doubean.model.ConnectionError
import com.github.bumblebee202111.doubean.model.GenericError
import com.github.bumblebee202111.doubean.model.PhotoList
import com.github.bumblebee202111.doubean.model.subject.CreditList
import com.github.bumblebee202111.doubean.model.subject.SearchResultSubjectItem
import com.github.bumblebee202111.doubean.model.subject.Subject
import com.github.bumblebee202111.doubean.model.subject.SubjectDetail
import com.github.bumblebee202111.doubean.model.subject.SubjectFeedSection
import com.github.bumblebee202111.doubean.model.subject.SubjectType
import com.github.bumblebee202111.doubean.model.subject.SubjectWithRank
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 书影音数据仓库：只读，全部直连豆瓣公开接口。
 */
@Singleton
class SubjectRepository @Inject constructor(
    private val api: DoubanApiService,
) {

    private data class CollectionSpec(val id: String, val title: String)

    private fun collectionsOf(type: SubjectType): List<CollectionSpec> = when (type) {
        SubjectType.TV -> listOf(
            CollectionSpec("tv_hot", "近期热门"),
            CollectionSpec("tv_domestic", "国产剧"),
            CollectionSpec("tv_american", "美剧"),
            CollectionSpec("tv_korean", "韩剧"),
            CollectionSpec("tv_japanese", "日剧"),
            CollectionSpec("tv_animation", "动画"),
        )

        else -> listOf(
            CollectionSpec("movie_showing", "正在上映"),
            CollectionSpec("movie_soon", "即将上映"),
            CollectionSpec("movie_top250", "豆瓣 Top 250"),
        )
    }

    /* ----------------------------- 首页 ----------------------------- */

    suspend fun loadFeed(type: SubjectType): AppResult<List<SubjectFeedSection>> = call {
        val sections = coroutineScope {
            collectionsOf(type)
                .map { spec ->
                    async {
                        SubjectFeedSection(
                            id = spec.id,
                            title = spec.title,
                            subjects = api.getCollectionItems(spec.id, start = 0, count = PER_PAGE)
                                .items
                                .map { it.toSubject(type) },
                        )
                    }
                }
                .awaitAll()
        }
        sections.filter { it.subjects.isNotEmpty() }
    }

    /* ----------------------------- 榜单 ----------------------------- */

    suspend fun loadChart(type: SubjectType): AppResult<List<SubjectWithRank<Subject>>> = call {
        if (type == SubjectType.TV) {
            api.getCollectionItems("tv_hot", start = 0, count = PER_PAGE)
                .items
                .mapIndexed { index, dto ->
                    SubjectWithRank(dto.toSubject(type), index + 1)
                }
        } else {
            api.getChart(chartType = 11, start = 0, limit = PER_PAGE)
                .map { it.toSubjectWithRank(type) }
        }
    }

    /* ----------------------------- 详情 ----------------------------- */

    suspend fun loadDetail(id: String, type: SubjectType): AppResult<SubjectDetail> = call {
        api.getSubjectDetail(type.apiValue(), id).toSubjectDetail(type)
    }

    suspend fun loadCredits(id: String, type: SubjectType): AppResult<CreditList> = call {
        val credits = api.getCredits(type.apiValue(), id)
        credits.items.toCreditList(credits.total)
    }

    suspend fun loadPhotos(
        id: String,
        type: SubjectType,
        start: Int = 0,
        count: Int = 20,
    ): AppResult<PhotoList> = call {
        api.getPhotos(type.apiValue(), id, start, count).toPhotoList()
    }

    suspend fun loadRecommendations(
        id: String,
        type: SubjectType,
    ): AppResult<List<Subject>> = call {
        api.getRecommendations(type.apiValue(), id)
            .map { it.toRecommendSubject(type) }
            .map { dto ->
                when (dto.type) {
                    SubjectType.TV -> com.github.bumblebee202111.doubean.model.subject.Tv(
                        dto.id, dto.rating, dto.cardSubtitle, dto.title, dto.imageUrl, dto.uri
                    )

                    else -> com.github.bumblebee202111.doubean.model.subject.Movie(
                        dto.id, dto.rating, dto.cardSubtitle, dto.title, dto.imageUrl, dto.uri
                    )
                }
            }
    }

    /* --------------------------- 找片 / 搜索 --------------------------- */

    suspend fun loadTags(type: SubjectType): AppResult<List<String>> = call {
        api.getTags(type.apiValue())
    }

    /**
     * 找电影 / 找电视。
     *
     * @param tags 形如 "电影"、"电影,美国"、"电影,美国,2024"
     */
    suspend fun explore(
        tags: List<String>,
        sort: String,
        start: Int = 0,
        range: String = "0,10",
        type: SubjectType = SubjectType.MOVIE,
    ): AppResult<List<Subject>> = call {
        api.newSearchSubjects(
            tags = tags.filter { it.isNotBlank() }.joinToString(","),
            sort = sort,
            start = start,
            range = range,
        ).map { it.toSubject(type) }
    }

    suspend fun suggest(query: String, type: SubjectType): AppResult<List<SearchResultSubjectItem>> =
        call {
            if (query.isBlank()) return@call emptyList()
            api.suggest(query).map { it.toSearchResultItem(type) }
        }

    /* ----------------------------- 工具 ----------------------------- */

    private suspend fun <T> call(block: suspend () -> T): AppResult<T> = withContext(Dispatchers.IO) {
        try {
            AppResult.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: ResponseException) {
            AppResult.Error(
                ApiError(
                    status = e.response.status.value,
                    code = e.response.status.value,
                    message = e.message,
                    request = e.response.call.request.url.toString(),
                    solutionUri = null,
                    originalError = e,
                )
            )
        } catch (e: IOException) {
            AppResult.Error(ConnectionError(e))
        } catch (e: Exception) {
            AppResult.Error(GenericError(e))
        }
    }

    private companion object {
        const val PER_PAGE = 20
    }
}

internal fun SubjectType.apiValue(): String = when (this) {
    SubjectType.TV -> "tv"
    else -> "movie"
}
