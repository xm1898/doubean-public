package com.github.bumblebee202111.doubean.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bumblebee202111.doubean.data.subject.SubjectRepository
import com.github.bumblebee202111.doubean.model.AppResult
import com.github.bumblebee202111.doubean.model.PhotoList
import com.github.bumblebee202111.doubean.model.subject.CreditList
import com.github.bumblebee202111.doubean.model.subject.Subject
import com.github.bumblebee202111.doubean.model.subject.SubjectDetail
import com.github.bumblebee202111.doubean.model.subject.SubjectType
import com.github.bumblebee202111.doubean.ui.UiState
import com.github.bumblebee202111.doubean.ui.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: SubjectRepository,
) : ViewModel() {

    private val _detail = MutableStateFlow<UiState<SubjectDetail>>(UiState.Loading)
    val detail: StateFlow<UiState<SubjectDetail>> = _detail.asStateFlow()

    private val _credits = MutableStateFlow<CreditList?>(null)
    val credits: StateFlow<CreditList?> = _credits.asStateFlow()

    private val _photos = MutableStateFlow<PhotoList?>(null)
    val photos: StateFlow<PhotoList?> = _photos.asStateFlow()

    private val _recommendations = MutableStateFlow<List<Subject>>(emptyList())
    val recommendations: StateFlow<List<Subject>> = _recommendations.asStateFlow()

    private var currentId: String? = null

    fun load(id: String, type: SubjectType) {
        if (currentId == id) return
        currentId = id
        viewModelScope.launch {
            _detail.value = UiState.Loading
            _detail.value = repository.loadDetail(id, type).toUiState()

            val creditsDeferred = async { repository.loadCredits(id, type) }
            val photosDeferred = async { repository.loadPhotos(id, type, start = 0, count = 12) }
            val recommendationsDeferred = async { repository.loadRecommendations(id, type) }

            (creditsDeferred.await() as? AppResult.Success)?.let { _credits.value = it.data }
            (photosDeferred.await() as? AppResult.Success)?.let { _photos.value = it.data }
            (recommendationsDeferred.await() as? AppResult.Success)?.let {
                _recommendations.value = it.data
            }
        }
    }

    fun retry() {
        val id = currentId ?: return
        currentId = null
        load(id, (detail.value as? UiState.Success)?.data?.type ?: SubjectType.MOVIE)
    }
}
