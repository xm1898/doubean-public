package com.github.bumblebee202111.doubean.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bumblebee202111.doubean.data.subject.SubjectRepository
import com.github.bumblebee202111.doubean.model.subject.SearchResultSubjectItem
import com.github.bumblebee202111.doubean.model.subject.SubjectType
import com.github.bumblebee202111.doubean.ui.UiState
import com.github.bumblebee202111.doubean.ui.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SubjectRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _type = MutableStateFlow(SubjectType.MOVIE)
    val type: StateFlow<SubjectType> = _type.asStateFlow()

    private val _results = MutableStateFlow<UiState<List<SearchResultSubjectItem>>>(UiState.Idle)
    val results: StateFlow<UiState<List<SearchResultSubjectItem>>> = _results.asStateFlow()

    init {
        combine(_query, _type) { keyword, type -> keyword to type }
            .debounce(350)
            .distinctUntilChanged()
            .onEach { (keyword, type) ->
                if (keyword.isBlank()) {
                    _results.value = UiState.Idle
                } else {
                    _results.value = UiState.Loading
                    _results.value = repository.suggest(keyword, type).toUiState()
                }
            }
            .launchIn(viewModelScope)
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setType(type: SubjectType) {
        _type.value = type
    }
}
