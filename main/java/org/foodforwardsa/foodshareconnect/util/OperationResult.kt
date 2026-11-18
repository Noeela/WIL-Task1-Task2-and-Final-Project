package org.foodforwardsa.foodshareconnect.util

/**
 * Every Repository function returns this instead of throwing, so every
 * screen can show a plain-language message instead of crashing - the
 * client-side half of the app's "extensive error handling" requirement.
 */
sealed class OperationResult<out T> {
    data class Success<T>(val data: T) : OperationResult<T>()
    data class Failure(val message: String) : OperationResult<Nothing>()
}

/** Runs [block], catching any exception and turning it into a friendly [OperationResult.Failure]. */
inline fun <T> safeCall(errorPrefix: String = "Something went wrong", block: () -> T): OperationResult<T> {
    return try {
        OperationResult.Success(block())
    } catch (e: Exception) {
        OperationResult.Failure("$errorPrefix: ${e.localizedMessage ?: "please try again."}")
    }
}
