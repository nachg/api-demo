package com.apidemo.util.annotations

import kotlin.reflect.KClass

const val UNDEF = -1

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EndpointInfo(
    val title: String = "",
    val responseCode: Int = UNDEF,
    val errorCode: Int = UNDEF,
    val errorCls: KClass<*> = NullErrorCls::class,
    val logRequestBody: Boolean = true,
    val logResponseBody: Boolean = true,
    val authRequired: Boolean = false,
    val linkedWithSession: Boolean = false
)

class NullErrorCls