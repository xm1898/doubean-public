package com.github.bumblebee202111.doubean

import com.github.bumblebee202111.doubean.core.network.api.DoubanApiService
import com.github.bumblebee202111.doubean.data.subject.toSubject
import com.github.bumblebee202111.doubean.data.subject.toSubjectDetail
import com.github.bumblebee202111.doubean.data.subject.toSubjectWithRank
import com.github.bumblebee202111.doubean.data.subject.toCreditList
import com.github.bumblebee202111.doubean.data.subject.toPhotoList
import com.github.bumblebee202111.doubean.data.subject.toRecommendSubject
import com.github.bumblebee202111.doubean.data.subject.toSearchResultItem
import com.github.bumblebee202111.doubean.model.subject.Rating
import com.github.bumblebee202111.doubean.model.subject.SubjectType
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 真实网络冒烟测试：验证豆瓣公开接口 + DTO 字段映射。
 * 需要联网；离线环境下会失败，属预期。
 */
class DoubanApiSmokeTest {

    private val client = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                    explicitNulls = false
                }
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 15_000
        }
        engine {
            config {
                followRedirects(true)
            }
        }
    }

    private val api = DoubanApiService(client)

    @Test
    fun feedCollections() = runBlocking {
        val ids = listOf(
            "movie_showing", "movie_soon", "movie_top250",
            "tv_hot", "tv_domestic", "tv_american",
            "tv_korean", "tv_japanese", "tv_animation",
        )
        ids.forEach { id ->
            val resp = api.getCollectionItems(id, start = 0, count = 6)
            assertTrue("collection $id empty", resp.items.isNotEmpty())
            val subjects = resp.items.map { it.toSubject(SubjectType.MOVIE) }
            assertTrue("collection $id mapped empty", subjects.isNotEmpty())
            assertTrue("collection $id blank title", subjects.all { it.title.isNotBlank() })
            println("[ok] $id total=${resp.total} first=${subjects.first().title} / ${subjects.first().rating}")
        }
    }

    @Test
    fun detailCreditsPhotos() = runBlocking {
        val detail = api.getSubjectDetail("movie", "1292052").toSubjectDetail(SubjectType.MOVIE)
        assertTrue(detail.title.isNotBlank())
        println("[ok] detail ${detail.title} ${detail.displayMetaInfo} rating=${detail.rating}")

        val creditsResponse = api.getCredits("movie", "1292052")
        val credits = creditsResponse.items.toCreditList(creditsResponse.total)
        assertTrue(credits.items.isNotEmpty())
        println("[ok] credits=${credits.items.size} first=${credits.items.first().name}")

        val photos = api.getPhotos("movie", "1292052", 0, 5).toPhotoList()
        assertTrue(photos.photos.isNotEmpty())
        println("[ok] photos=${photos.total} first=${photos.photos.first().image.normal.url}")

        val tv = api.getSubjectDetail("tv", "36675942").toSubjectDetail(SubjectType.TV)
        assertTrue(tv.title.isNotBlank())
        println("[ok] tv detail ${tv.title} meta=${tv.displayMetaInfo}")
    }

    @Test
    fun chartAndRecommendations() = runBlocking {
        val chart = api.getChart(chartType = 11, start = 0, limit = 5)
            .map { it.toSubjectWithRank(SubjectType.MOVIE) }
        assertTrue(chart.isNotEmpty())
        assertTrue(chart.all { it.rankValue > 0 })
        println("[ok] chart=${chart.size} first=${chart.first().subject.title} rank=${chart.first().rankValue}")

        val recs = api.getRecommendations("movie", "1292052")
            .map { it.toRecommendSubject(SubjectType.MOVIE) }
        assertTrue(recs.isNotEmpty())
        println("[ok] recs=${recs.size} first=${recs.first().title}")
    }

    @Test
    fun exploreAndSuggest() = runBlocking {
        val page = api.newSearchSubjects(tags = "电影,美国,2024", sort = "U", start = 0)
            .map { it.toSubject(SubjectType.MOVIE) }
        assertTrue(page.isNotEmpty())
        println("[ok] explore=${page.size} first=${page.first().title} / ${page.first().rating}")

        val tvPage = api.newSearchSubjects(tags = "电视剧,韩剧", sort = "S", start = 0)
            .map { it.toSubject(SubjectType.TV) }
        assertTrue(tvPage.isNotEmpty())
        println("[ok] explore tv=${tvPage.size} first=${tvPage.first().title}")

        // 体裁 + 评分区间 + 排序 三者叠加
        val comedy = api.newSearchSubjects(
            tags = "电影,喜剧",
            sort = "T",
            start = 0,
            range = "8,10",
        ).map { it.toSubject(SubjectType.MOVIE) }
        assertTrue(comedy.isNotEmpty())
        assertTrue(
            "range=8,10 却返回了低分片：${comedy.map { it.title to it.rating }}",
            comedy.all { it.rating is Rating.NonNull && (it.rating as Rating.NonNull).value >= 8f },
        )
        println("[ok] 电影+喜剧+8分以上+时间排序=${comedy.size} ${comedy.take(3).map { it.title }}")

        val costume = api.newSearchSubjects(
            tags = "电视剧,古装,华语",
            sort = "S",
            start = 0,
            range = "7,10",
        ).map { it.toSubject(SubjectType.TV) }
        assertTrue(costume.isNotEmpty())
        println("[ok] 电视剧+古装+华语+7分以上=${costume.size} ${costume.take(3).map { it.title }}")

        val tags = api.getTags("movie")
        assertTrue(tags.isNotEmpty())
        println("[ok] tags=$tags")

        val suggest = api.suggest("生日").map { it.toSearchResultItem(SubjectType.MOVIE) }
        assertTrue(suggest.isNotEmpty())
        println("[ok] suggest=${suggest.size} first=${suggest.first().title}")
    }
}
