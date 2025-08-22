package com.example.eventmanagement2.util

/**
 * A sealed class that encapsulates successful outcome with a value of type [T]
 * or a failure with message and throwable.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>() {
        override fun toString(): String = "[Success: data=$data]"
    }

    data class Error(val message: String? = null, val throwable: Throwable? = null) : Result<Nothing>() {
        constructor(throwable: Throwable) : this(throwable.message, throwable)

        override fun toString(): String = "[Error: message=$message, throwable=${throwable?.message}]"
    }

    object Loading : Result<Nothing>() {
        override fun toString(): String = "[Loading]"
    }

    /**
     * Returns `true` if this instance represents a successful outcome.
     */
    val isSuccess: Boolean get() = this is Success

    /**
     * Returns `true` if this instance represents a failed outcome.
     */
    val isError: Boolean get() = this is Error

    /**
     * Returns `true` if this instance represents a loading state.
     */
    val isLoading: Boolean get() = this is Loading

    /**
     * Returns the encapsulated value if this instance represents [Success] or `null` if it is [Error] or [Loading].
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Returns the encapsulated [Throwable] if this instance represents [Error] or `null` if it is [Success] or [Loading].
     */
    fun exceptionOrNull(): Throwable? = when (this) {
        is Error -> throwable
        else -> null
    }

    /**
     * Returns the encapsulated result of the given [transform] function applied to the encapsulated value
     * if this instance represents [Success] or the original [Error] or [Loading] value.
     */
    inline fun <R> map(transform: (value: T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> this
    }

    /**
     * Returns the encapsulated result of the given [transform] function applied to the encapsulated [Throwable]
     * if this instance represents [Error] or the original [Success] or [Loading] value.
     */
    inline fun <R> mapError(transform: (message: String?, throwable: Throwable?) -> R): Result<T> = when (this) {
        is Error -> {
            val result = transform(message, throwable)
            if (result is Result<*>) {
                result as Result<T>
            } else {
                Success(result as T)
            }
        }
        else -> this
    }

    /**
     * Performs the given [action] on the encapsulated value if this instance represents [Success].
     * Returns the original `Result` unchanged.
     */
    inline fun onSuccess(action: (value: T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * Performs the given [action] on the encapsulated [Throwable] if this instance represents [Error].
     * Returns the original `Result` unchanged.
     */
    inline fun onError(action: (message: String?, throwable: Throwable?) -> Unit): Result<T> {
        if (this is Error) action(message, throwable)
        return this
    }

    /**
     * Performs the given [action] if this instance represents [Loading].
     * Returns the original `Result` unchanged.
     */
    inline fun onLoading(action: () -> Unit): Result<T> {
        if (this is Loading) action()
        return this
    }

    companion object {
        /**
         * Returns a [Result] with the specified [value] as [Success].
         */
        fun <T> success(value: T): Result<T> = Success(value)

        /**
         * Returns a [Result] with the specified [message] and [throwable] as [Error].
         */
        fun <T> error(message: String? = null, throwable: Throwable? = null): Result<T> =
            Error(message, throwable)

        /**
         * Returns a [Result] with the specified [throwable] as [Error].
         */
        fun <T> error(throwable: Throwable): Result<T> = Error(throwable)

        /**
         * Returns a [Loading] result.
         */
        fun <T> loading(): Result<T> = Loading
    }
}
