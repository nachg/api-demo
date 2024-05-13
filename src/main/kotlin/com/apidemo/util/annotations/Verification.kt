package com.apidemo.util.annotations

class Verification {
    @Target(AnnotationTarget.PROPERTY)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Ignore
}