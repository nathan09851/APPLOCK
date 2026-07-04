package com.nathan.applock.data

enum class RelockPolicy {
    IMMEDIATELY,
    AFTER_10_SEC,
    AFTER_1_MIN;

    val delayMillis: Long
        get() = when (this) {
            IMMEDIATELY -> 0L
            AFTER_10_SEC -> 10_000L
            AFTER_1_MIN -> 60_000L
        }
}
