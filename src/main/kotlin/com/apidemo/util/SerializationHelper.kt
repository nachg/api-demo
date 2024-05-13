package com.apidemo.util

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.restassured.response.Response
import org.openapitools.jackson.nullable.JsonNullableModule
import kotlin.reflect.full.declaredMemberProperties

object SerializationHelper {
    val mapper: ObjectMapper = jacksonObjectMapper()
    init {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.registerModule(JavaTimeModule())
        mapper.registerModule(JsonNullableModule())
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        mapper.serializationConfig.defaultVisibilityChecker
            .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withCreatorVisibility(JsonAutoDetect.Visibility.NONE)

    }

    fun<T> serialize(response: Response, cls: Class<T>): T? {
        val body = response.body.asString()
        return if(body.isEmpty()) {
            null
        } else if (cls.simpleName == "String"){
            body as T
        } else {
            try {
                mapper.readValue(body, cls)
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }

    fun Any?.toJson() = if(this == null) "" else if (this is String) this else mapper.writeValueAsString(this)
    fun Any?.toPrettyJson() = if(this == null) "" else mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)

    inline fun <reified T> String.serialize() : T {
        return mapper.readValue(this, T::class.java)
    }

    fun String.toKeyValueObject(): Any? {
        try {
            if(this.trim().isEmpty()) return null
            val trimmed = this.trim()
            if(trimmed.startsWith("[")) {
                return mapper.readValue(
                    trimmed,
                    object : TypeReference<Array<Map<String, Any?>?>>() {}
                )
            }
            return mapper.readValue(
                trimmed,
                object : TypeReference<Map<String, Any?>?>() {}
            )
        } catch (e: Exception) {

        }
        return null
    }

    fun Any.toUrlParams(): String {
        var res ="?"
        this.javaClass.kotlin.declaredMemberProperties.forEach {
            val v = it.call(this)
            if(v != null) {
                if(v is Collection<*>) {
                    v.forEach { v ->
                        res += it.name + "=" + v.toString() + "&"
                    }
                } else {
                    res += it.name + "=" + v.toString() + "&"
                }
            }
        }
        return res.removeSuffix("&")
    }
}

