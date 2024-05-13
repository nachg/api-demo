package com.apidemo.util


import com.apidemo.util.common.NoBodyResponse
import io.restassured.response.Response
import kotlin.reflect.KClass

open class ResponseWrapper<out T: Any>(
    private val cls: Class<*>,
    var response: Response
 ) {

    operator fun invoke(): T {
        if(cls.name == NoBodyResponse::class.java.name) {
            return NoBodyResponse() as T
        }
        return (SerializationHelper.serialize(response, cls) as T?)!!
    }

    fun responseAsString(): String {
        return response.body.asString()
    }

    fun getError(cls: KClass<*>): Any {
        return SerializationHelper.serialize(response, cls.java)!!
    }
}

