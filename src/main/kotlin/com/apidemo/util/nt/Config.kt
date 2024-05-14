package com.apidemo.util.nt

import java.time.Duration

data class Config(
    val staticDiffMs: Long = 10L,
    val staticDiffPercent: Long = 1,

    val parallelDiffMs: Long = 10L,
    val parallelDiffPercent: Long = 1,

    val singleThreadRetryCount: Int = 10,
    val threadCount: Int = 10,

    val internalRunFor: Duration = Duration.ZERO
)