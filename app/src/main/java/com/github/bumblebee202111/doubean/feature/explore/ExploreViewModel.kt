package com.github.bumblebee202111.doubean.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bumblebee202111.doubean.data.subject.SubjectRepository
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

/** 找电影 / 找电视的筛选项。 */
enum class ExploreSort(val label: String, val apiValue: String) {
    RECOMMEND("综合排序", "U"),
    RANK("评分排序", "S"),
    TIME("时间排序", "T"),
}

data class ExploreFilters(
    val category: String = "电影",
    val region: String = "全部",
    val year: String = "全部",
    val sort: ExploreSort = ExploreSort.RECOMMEND,
)

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

    private var started = false
    private var start = 0
    private var job: Job? = null

    fun start(type: SubjectType) {
        if (started && _type.value == type) return
        started = true
        _type.value = type
        _filters.value = ExploreFilters(
            category = if (type == SubjectType.TV) "电视剧" else "电影"
        )
        reload()
    }

    fun setCategory(category: String) {
        _filters.value = _filters.value.copy(category = category)
        reload()
    }

    fun setRegion(region: String) {
        _filters.value = _filters.value.copy(region = region)
        reload()
    }

    fun setYear(year: String) {
        _filters.value = _filters.value.copy(year = year)
        reload()
    }

    fun setSort(sort: ExploreSort) {
        _filters.value = _filters.value.copy(sort = sort)
        reload()
    }

    fun reload() {
        job?.cancel()
        start = 0
        job = viewModelScope.launch {
            _items.value = UiState.Loading
            _items.value = repository.explore(
                tags = buildTags(_filters.value),
                sort = _filters.value.sort.apiValue,
                start = 0,
                type = _type.value,
            ).toUiState()
            start = PAGE_SIZE
        }
    }

    fun loadMore() {
        val current = _items.value
        if (current !is UiState.Success) return
        if (_loadingMore.value) return
        job = viewModelScope.launch {
            _loadingMore.value = true
            val result = repository.explore(
                tags = buildTags(_filters.value),
                sort = _filters.value.sort.apiValue,
                start = start,
                type = _type.value,
            )
            when (result) {
                is com.github.bumblebee202111.doubean.model.AppResult.Success -> {
                    start += PAGE_SIZE
                    _items.value = UiState.Success(current.data + result.data)
                }

                is com.github.bumblebee202111.doubean.model.AppResult.Error -> Unit
            }
            _loadingMore.value = false
        }
    }

    private fun buildTags(filters: ExploreFilters): List<String> = buildList {
        add(filters.category)
        if (filters.region != "全部") add(filters.region)
        if (filters.year != "全部") add(filters.year)
    }

    companion object {
        const val PAGE_SIZE = 20

        val CATEGORIES = listOf("电影", "电视剧", "综艺", "纪录片")
        val REGIONS = listOf("全部", "华语", "欧美", "韩国", "日本")
        val YEARS = listOf(
            "全部", "2025", "2024", "2023", "2022", "2021", "2020",
            "2019", "2018", "2017", "2016", "2015",
            "2010年代", "2000年代", "90年代", "80年代",
        )
    }
}
