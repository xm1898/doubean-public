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

/** 找电影 / 找电视的排序方式。 */
enum class ExploreSort(val label: String, val apiValue: String) {
    RECOMMEND("综合", "U"),
    RANK("评分", "S"),
    TIME("时间", "T"),
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
    private var start = 0
    private var job: Job? = null

    // 「时间排序 + 年份=全部」时按年份倒着翻：先 2026，再 2025…，保证新片永远在最前面。
    private var yearCursor = 0
    private var yearStart = 0

    /** 豆瓣的 sort=T 在「年份=全部」下并不按年份倒序，只能自己按年份往回走。 */
    private val ExploreFilters.isTimeCrawl: Boolean
        get() = sort == ExploreSort.TIME && year == "全部"

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
        start = 0
        _exhausted.value = false
        val filters = _filters.value
        job = viewModelScope.launch {
            _items.value = UiState.Loading
            if (filters.isTimeCrawl) {
                yearCursor = MAX_YEAR
                yearStart = 0
                val result = fetchTimeChunk(filters, PAGE_SIZE)
                _items.value = result.toUiState()
                if (result is AppResult.Success && result.data.size < PAGE_SIZE) _exhausted.value = true
                return@launch
            }
            val result = repository.explore(
                tags = buildTags(filters),
                sort = filters.sort.apiValue,
                start = 0,
                range = ratingRange(filters.rating),
                type = _type.value,
            )
            _items.value = result.toUiState()
            start = PAGE_SIZE
            if (result is AppResult.Success && result.data.size < PAGE_SIZE) _exhausted.value = true
        }
    }

    fun loadMore() {
        val current = _items.value
        if (current !is UiState.Success) return
        if (_loadingMore.value || _exhausted.value) return
        val filters = _filters.value
        job = viewModelScope.launch {
            _loadingMore.value = true
            if (filters.isTimeCrawl) {
                val result = fetchTimeChunk(filters, PAGE_SIZE)
                when (result) {
                    is AppResult.Success -> {
                        if (result.data.isEmpty() || result.data.size < PAGE_SIZE) _exhausted.value = true
                        if (result.data.isNotEmpty()) {
                            _items.value = UiState.Success(current.data + result.data)
                        }
                    }

                    is AppResult.Error -> _exhausted.value = true
                }
                _loadingMore.value = false
                return@launch
            }
            val result = repository.explore(
                tags = buildTags(filters),
                sort = filters.sort.apiValue,
                start = start,
                range = ratingRange(filters.rating),
                type = _type.value,
            )
            when (result) {
                is AppResult.Success -> {
                    // 该接口不返回 total，只能靠「翻到空页」判断到底。
                    if (result.data.isEmpty() || result.data.size < PAGE_SIZE) {
                        _exhausted.value = true
                    }
                    start += PAGE_SIZE
                    _items.value = UiState.Success(current.data + result.data)
                }

                is AppResult.Error -> _exhausted.value = true
            }
            _loadingMore.value = false
        }
    }

    /**
     * 从 [yearCursor] 开始按年份倒序取，凑够 [target] 条或翻到 [MIN_YEAR] 为止。
     *
     * 每翻完一年游标就减一，所以「时间」排序天然从最新往回走；接口没有 total，
     * 靠「这一年返回不满 20 条」判断该年翻完。
     */
    private suspend fun fetchTimeChunk(
        filters: ExploreFilters,
        target: Int,
    ): AppResult<List<Subject>> {
        val acc = mutableListOf<Subject>()
        var hops = 0
        while (acc.size < target && yearCursor >= MIN_YEAR && hops++ < MAX_YEAR_HOPS) {
            val year = yearCursor.toString()
            val result = repository.explore(
                tags = buildTags(filters, year),
                sort = ExploreSort.TIME.apiValue,
                start = yearStart,
                range = ratingRange(filters.rating),
                type = _type.value,
                yearHint = year,
            )
            when (result) {
                is AppResult.Error -> return if (acc.isEmpty()) result else AppResult.Success(acc)
                is AppResult.Success -> {
                    acc += result.data
                    if (result.data.size < PAGE_SIZE) {
                        yearCursor--
                        yearStart = 0
                    } else {
                        yearStart += PAGE_SIZE
                    }
                }
            }
        }
        return AppResult.Success(acc)
    }

    private fun buildTags(filters: ExploreFilters, yearOverride: String? = null): List<String> = buildList {
        add(filters.category)
        if (filters.genre != "全部") add(filters.genre)
        if (filters.region != "全部") add(filters.region)
        val year = yearOverride ?: filters.year.takeIf { it != "全部" }
        if (year != null) add(year)
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

        /** 「时间」排序从这一年开始往前翻（取年份列表里最新的那一年）。 */
        val MAX_YEAR: Int = YEARS.firstNotNullOfOrNull { it.toIntOrNull() } ?: 2026

        const val MIN_YEAR = 1980

        /** 单次加载最多换几个年份，避免极端情况下一直空转到卡死。 */
        const val MAX_YEAR_HOPS = 12

        /** 豆瓣 `range` 参数是「评分下限,上限」。 */
        fun ratingRange(label: String): String = when (label) {
            "9分以上" -> "9,10"
            "8分以上" -> "8,10"
            "7分以上" -> "7,10"
            "6分以上" -> "6,10"
            "5分以下" -> "0,5"
            else -> "0,10"
        }
    }
}
