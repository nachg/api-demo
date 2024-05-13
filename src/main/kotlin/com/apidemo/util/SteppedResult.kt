package com.apidemo.util

import org.testng.SkipException
import org.xpathqs.log.Log
import java.time.Duration

fun <T> step(log: String, lambda: ()->T) : StepResult<T> {
    return Log.step(log) {
        StepResult(
            result = lambda()
        )
    }
}

class StepResult<T>(
    val result: T
) {
    fun<A> step(log: String, lambda: (T)->A) : StepResult<A> {
        return Log.step(log) {
            StepResult(
                result = lambda(result)
            )
        }
    }
}

inline fun<T> runWithRetryOnFailure(
    retryCount: Int = 3,
    delay: Duration? = null,
    skipOnError: Boolean = false,
    failLambda: (()->Unit) = {},
    f: ()->T
): T? {
    repeat(retryCount) {
        runCatching {
            f()
        }.getOrNull()?.let {
            return it
        }
        delay?.let {
            Log.info("Sleep for ${delay.toMillis()}ms before next attempt")
            Thread.sleep(delay.toMillis())
        }
        failLambda()
    }
    if(skipOnError) {
        throw SkipException("Block was not able to complete without errors")
    }
    return null
}