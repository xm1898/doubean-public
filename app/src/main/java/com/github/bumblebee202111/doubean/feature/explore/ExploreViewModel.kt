package com.github.bumblebee202111.doubean.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bumblebee202111.doubean.data.subject.SubjectRepository
import com.github.bumblebee202111.doubean.model.AppResult
import com.github.bumblebee202111.doubean.model.subject.Subject
import com.github.bumblebee202111.doubean.model.subject.SubjectType
import com.github.bumblebee202111.doubean.ui.UiState
import com.github.bumblebee202111.doubean.ui.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 找电影 / 找电视的排序方式，取值就是官方「选电影 / 选剧集」页信息流自己列的 sorts：
 * `[{"综合排序":"T"},{"近期热度":"U"},{"首映时间":"R"},{"高分优先":"S"}]`。
 *
 * 注意「综合」是 T 不是 U —— U 是「近期热度」。
 */
enum class ExploreSort(val label: String, val apiValue: String) {
    RECOMMEND("综合", "T"),
    RANK("评分", "S"),
    TIME("时间", "R"),
}

data class ExploreFilters(
    val category: String = "电影",
    val genre: String = "全部",
    val region: String = "全部",
    val year: String = "全部",
    val rating: String = "全部",
    val sort: ExploreSort = ExploreSort.RECOMMEND,
) {
    /** 体裁/地区/年份/评分里选中的个数，用于「筛选 N」角标。 */
    val activeCount: Int
        get() = listOf(genre, region, year, rating).count { it != "全部" }

    val isDefault: Boolean
        get() = activeCount == 0 && sort == ExploreSort.RECOMMEND
}

/**
 * 找片页的数据源统一走 `rexxar/api/v2/{movie,tv}/recommend`：
 * 它同时支持 `tags`（体裁/地区/年份）、`score_range`（评分区间）和 `sort`（含首映时间），
 * 三项都由服务端过滤，翻页不会再出现「本地筛空导致白屏」。
 */
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: SubjectRepository,
) : ViewModel() {

    private val _type = MutableStateFlow(SubjectType.MOVIE)
    val type: StateFlow<SubjectType> = _type.asStateFlow()

    private val _filters = MutableStateFlow(ExploreFilters())
    val filters: StateFlow<ExploreFilters> = _filters.asStateFlow()

    private val _items = MutableStateFlow<UiState<List<Subject>>>(UiState.Loading)
    val items: StateFlow<UiState<List<Subject>>> = _items.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    private val _exhausted = MutableStateFlow(false)
    val exhausted: StateFlow<Boolean> = _exhausted.asStateFlow()

    private var started = false
    private var job: Job? = null

    /** 信息流的翻页游标。 */
    private var offset = 0

    /** 已经展示过的 id，避免同一部片子在两页里重复出现把列表撑出重复 key。 */
    private val seen = mutableSetOf<String>()

    fun start(type: SubjectType) {
        if (started && _type.value == type) return
        started = true
        _type.value = type
        _filters.value = ExploreFilters(
            category = if (type == SubjectType.TV) "电视剧" else "电影",
        )
        reload()
    }

    /** 切大类（电影/电视剧/综艺/纪录片）时体裁词表会变，所以要重置体裁。 */
    fun setCategory(category: String) {
        if (_filters.value.category == category) return
        _filters.value = _filters.value.copy(category = category, genre = "全部")
        _type.value = if (category == "电影") SubjectType.MOVIE else SubjectType.TV
        reload()
    }

    fun setGenre(genre: String) = update { it.copy(genre = genre) }

    fun setRegion(region: String) = update { it.copy(region = region) }

    fun setYear(year: String) = update { it.copy(year = year) }

    fun setRating(rating: String) = update { it.copy(rating = rating) }

    fun setSort(sort: ExploreSort) = update { it.copy(sort = sort) }

    fun resetFilters() {
        _filters.value = _filters.value.copy(
            genre = "全部",
            region = "全部",
            year = "全部",
            rating = "全部",
            sort = ExploreSort.RECOMMEND,
        )
        reload()
    }

    private inline fun update(block: (ExploreFilters) -> ExploreFilters) {
        val next = block(_filters.value)
        if (next == _filters.value) return
        _filters.value = next
        reload()
    }

    fun reload() {
        job?.cancel()
        offset = 0
        seen.clear()
        _exhausted.value = false
        _loadingMore.value = false
        val filters = _filters.value
        job = viewModelScope.launch {
            _items.value = UiState.Loading
            when (val result = fetch(filters, start = 0)) {
                is AppResult.Error -> _items.value = result.toUiState()
                is AppResult.Success -> {
                    seen += result.data.map { it.id }
                    _items.value = UiState.Success(result.data)
                    if (result.data.size < PAGE_SIZE) _exhausted.value = true
                    offset = PAGE_SIZE
                }
            }
        }
    }

    fun loadMore() {
        val current = _items.value as? UiState.Success ?: return
        if (_loadingMore.value || _exhausted.value) return
        val filters = _filters.value
        // 先占住闸门再起协程：两次滚动事件挨得很近时不会重复请求、重复追加。
        _loadingMore.value = true
        val cursor = offset
        viewModelScope.launch {
            when (val result = fetch(filters, cursor)) {
                is AppResult.Error -> _exhausted.value = true
                is AppResult.Success -> {
                    val fresh = result.data.filter { seen.add(it.id) }
                    if (fresh.isNotEmpty()) {
                        _items.value = UiState.Success(current.data + fresh)
                    }
                    // 接口没有 total：原始条数不满一页就说明到底了。
                    if (result.data.size < PAGE_SIZE) _exhausted.value = true
                    offset = cursor + PAGE_SIZE
                }
            }
            _loadingMore.value = false
        }
    }

    private suspend fun fetch(filters: ExploreFilters, start: Int): AppResult<List<Subject>> =
        repository.premiereFeed(
            type = _type.value,
            sort = filters.sort.apiValue,
            start = start,
            count = PAGE_SIZE,
            tags = buildTags(filters).ifEmpty { null }?.joinToString(","),
            scoreRange = scoreRange(filters.rating),
        )

    /**
     * 信息流的 tags 是「体裁,地区,年份」逗号串，空项不传。
     *
     * 电影 / 电视剧已经由 path 区分（`/movie/recommend`、`/tv/recommend`），
     * 综艺和纪录片都挂在 tv 下面，所以要把大类本身当 tag 传。
     */
    private fun buildTags(filters: ExploreFilters): List<String> = buildList {
        if (filters.category == "综艺" || filters.category == "纪录片") add(filters.category)
        if (filters.genre != "全部") add(filters.genre)
        if (filters.region != "全部") add(filters.region)
        if (filters.year != "全部") add(filters.year)
    }

    companion object {
        const val PAGE_SIZE = 20

        val CATEGORIES = listOf("电影", "电视剧", "综艺", "纪录片")

        private val MOVIE_GENRES = listOf(
            "全部", "喜剧", "爱情", "动作", "科幻", "动画", "悬疑", "惊悚", "恐怖",
            "剧情", "犯罪", "纪录片", "短片", "家庭", "传记", "历史", "战争", "奇幻",
            "冒险", "灾难", "武侠", "古装", "音乐", "运动",
        )

        private val TV_GENRES = listOf(
            "全部", "古装", "战争", "青春", "偶像", "喜剧", "家庭", "犯罪", "动作",
            "奇幻", "剧情", "历史", "年代", "悬疑", "武侠", "爱情", "恐怖", "科幻",
            "动画", "短片", "纪录片",
        )

        private val SHOW_GENRES = listOf(
            "全部", "真人秀", "脱口秀", "访谈", "选秀", "搞笑", "音乐", "竞技", "生活",
        )

        private val DOC_GENRES = listOf(
            "全部", "人文", "历史", "自然", "科技", "美食", "社会", "旅行", "动物",
        )

        fun genresFor(category: String): List<String> = when (category) {
            "电视剧" -> TV_GENRES
            "综艺" -> SHOW_GENRES
            "纪录片" -> DOC_GENRES
            else -> MOVIE_GENRES
        }

        val REGIONS = listOf("全部", "华语", "欧美", "韩国", "日本")

        val YEARS = listOf(
            "全部", "2026", "2025", "2024", "2023", "2022", "2021", "2020",
            "2019", "2018", "2017", "2016", "2015",
            "2010年代", "2000年代", "90年代", "80年代",
        )

        val RATINGS = listOf("全部", "9分以上", "8分以上", "7分以上", "6分以上", "5分以下")

        /** 信息流的 `score_range` 参数是「评分下限,上限」，服务端过滤。 */
        fun scoreRange(label: String): String = when (label) {
            "9分以上" -> "9,10"
            "8分以上" -> "8,10"
            "7分以上" -> "7,10"
            "6分以上" -> "6,10"
            "5分以下" -> "0,5"
            else -> "0,10"
        }
    }
}
