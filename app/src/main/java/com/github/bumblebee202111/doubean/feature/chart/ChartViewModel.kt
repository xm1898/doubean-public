package com.github.bumblebee202111.doubean.feature.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bumblebee202111.doubean.data.subject.SubjectRepository
import com.github.bumblebee202111.doubean.model.subject.Subject
import com.github.bumblebee202111.doubean.model.subject.SubjectType
import com.github.bumblebee202111.doubean.model.subject.SubjectWithRank
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
class ChartViewModel @Inject constructor(
    private val repository: SubjectRepository,
) : ViewModel() {

    private val _state =
        MutableStateFlow<UiState<List<SubjectWithRank<Subject>>>>(UiState.Loading)
    val state: StateFlow<UiState<List<SubjectWithRank<Subject>>>> = _state.asStateFlow()

    private var currentType: SubjectType? = null
    private var job: Job? = null

    fun load(type: SubjectType) {
        if (currentType == type) return
        currentType = type
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = repository.loadChart(type).toUiState()
        }
    }

    fun retry() {
        currentType?.let {
            currentType = null
            load(it)
        }
    }
}
