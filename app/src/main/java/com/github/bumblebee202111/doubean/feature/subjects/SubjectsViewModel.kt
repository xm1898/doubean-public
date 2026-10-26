package com.github.bumblebee202111.doubean.feature.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bumblebee202111.doubean.data.subject.SubjectRepository
import com.github.bumblebee202111.doubean.model.subject.SubjectFeedSection
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

@HiltViewModel
class SubjectsViewModel @Inject constructor(
    private val repository: SubjectRepository,
) : ViewModel() {

    private val _selectedType = MutableStateFlow(SubjectType.MOVIE)
    val selectedType: StateFlow<SubjectType> = _selectedType.asStateFlow()

    private val _feedState = MutableStateFlow<UiState<List<SubjectFeedSection>>>(UiState.Loading)
    val feedState: StateFlow<UiState<List<SubjectFeedSection>>> = _feedState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load(_selectedType.value)
    }

    fun selectType(type: SubjectType) {
        if (type == _selectedType.value) return
        _selectedType.value = type
        load(type)
    }

    fun retry() = load(_selectedType.value)

    private fun load(type: SubjectType) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _feedState.value = UiState.Loading
            _feedState.value = repository.loadFeed(type).toUiState()
        }
    }
}
