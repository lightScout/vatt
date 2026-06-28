package org.lightscout.vatt.core.result

import org.lightscout.vatt.domain.error.AppError

/**
 * The single return type for every repository/use-case call: either [Success] with a value or [Failure]
 * with a typed [AppError]. Modelling failure as data (rather than thrown exceptions) forces every caller
 * to handle the error path, which is exactly the discipline this integration needs given how often the
 * mock fails.
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: AppError) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
}

inline fun <T> ApiResult<T>.onSuccess(action: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) action(data)
    return this
}

inline fun <T> ApiResult<T>.onFailure(action: (AppError) -> Unit): ApiResult<T> {
    if (this is ApiResult.Failure) action(error)
    return this
}

fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.data
