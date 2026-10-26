package com.github.bumblebee202111.doubean.ui

import com.github.bumblebee202111.doubean.model.ApiError
import com.github.bumblebee202111.doubean.model.AppError
import com.github.bumblebee202111.doubean.model.ConnectionError
import com.github.bumblebee202111.doubean.model.GenericError
import com.github.bumblebee202111.doubean.model.AppResult

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
}

fun AppError.displayMessage(): String = when (this) {
    is ApiError -> message ?: "请求失败（${status}）"
    is ConnectionError -> "网络连接失败，请检查网络"
    is GenericError -> originalError?.message ?: "出错了"
    else -> "出错了"
}

fun <T> AppResult<T>.toUiState(): UiState<T> = when (this) {
    is AppResult.Success -> UiState.Success(data)
    is AppResult.Error -> UiState.Error(error.displayMessage())
}
