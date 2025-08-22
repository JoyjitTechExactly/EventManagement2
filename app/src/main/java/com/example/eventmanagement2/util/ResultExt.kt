package com.example.eventmanagement2.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import timber.log.Timber

/**
 * Converts a Flow of Result<T> to handle loading, success, and error states
 */
fun <T> Flow<Result<T>>.asResult(): Flow<Result<T>> = this
    .map { result ->
        when (result) {
            is Result.Success -> result
            is Result.Error -> {
                Timber.e(result.throwable, result.message)
                result
            }
            Result.Loading -> result
        }
    }
    .onStart { emit(Result.Loading) }
    .catch { e ->
        Timber.e(e, "Error in flow")
        emit(Result.Error(e))
    }

/**
 * Executes the given [block] and wraps the result in a [Result].
 */
suspend fun <T> runCatching(block: suspend () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        Timber.e(e, "Error in runCatching")
        Result.Error(e)
    }
}

/**
 * Maps the success value of this [Result] using the given [transform] function.
 */
inline fun <T, R> Result<T>.map(transform: (value: T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> this
        Result.Loading -> Result.Loading
    }
}

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated value
 * if this instance represents [Result.Success] or the original [Result.Error] or [Result.Loading].
 */
inline fun <T, R> Result<T>.flatMap(transform: (value: T) -> Result<R>): Result<R> {
    return when (this) {
        is Result.Success -> transform(data)
        is Result.Error -> this
        Result.Loading -> Result.Loading
    }
}
