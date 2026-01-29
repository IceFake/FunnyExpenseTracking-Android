package com.example.funnyexpensetracking.util

/**
 * 通用资源封装类，用于处理网络请求结果
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}

/**
 * 扩展函数：将Resource映射为另一种类型
 */
inline fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> {
    return when (this) {
        is Resource.Success -> Resource.Success(transform(data!!))
        is Resource.Error -> Resource.Error(message ?: "Unknown error")
        is Resource.Loading -> Resource.Loading()
    }
}

/**
 * 扩展函数：处理Resource结果
 */
inline fun <T> Resource<T>.onSuccess(action: (T) -> Unit): Resource<T> {
    if (this is Resource.Success) {
        data?.let(action)
    }
    return this
}

inline fun <T> Resource<T>.onError(action: (String) -> Unit): Resource<T> {
    if (this is Resource.Error) {
        message?.let(action)
    }
    return this
}

inline fun <T> Resource<T>.onLoading(action: () -> Unit): Resource<T> {
    if (this is Resource.Loading) {
        action()
    }
    return this
}

