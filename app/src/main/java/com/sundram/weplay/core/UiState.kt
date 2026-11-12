package com.sundram.weplay.core

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Empty(val message: String? = null) : UiState<Nothing>
    data class Error(val message: String, val retryable: Boolean = true) : UiState<Nothing>
}
