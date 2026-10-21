package org.videopocket.probe.core.common.result

sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Error(val error: org.videopocket.probe.core.common.error.AppError) : AppResult<Nothing>
    data class Progress(val progressPercent: Int, val stageDescription: String? = null) : AppResult<Nothing>

    val isSuccess: Boolean
        get() = this is Success

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
}
