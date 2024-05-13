package com.apidemo.util

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider

object JsonUtils {
    private val configuration = Configuration.builder()
        .jsonProvider(JacksonJsonNodeJsonProvider())
        .mappingProvider(JacksonMappingProvider())
        .build()

    private val jsonWorker = JsonPath.using(configuration)

    fun select(source: String, jsonPath: String) : String {
         return jsonWorker.parse(source).read<Any>(jsonPath).toString()
    }

    fun updateJson(source: String, jsonPath: String, value: String) : String {
        return jsonWorker.parse(source).set(jsonPath, value).jsonString()
    }

    fun updateJson(source: String, jsonPath: String, value: Int) : String {
        return jsonWorker.parse(source).set(jsonPath, value).jsonString()
    }

    fun updateJson(source: String, jsonPath: String, value: Any) : String {
        return jsonWorker.parse(source).set(jsonPath, value).jsonString()
    }

    fun deleteJson(source: String, jsonPath: String) : String {
        return jsonWorker.parse(source).delete(jsonPath).jsonString()
    }

    fun removePrefixAndSuffix(source: DocumentContext, jsonPath: String ) =
        (source.read(jsonPath) as String)
            .removePrefix("[\"")
            .removeSuffix("\"]")
}

fun String.selectJson(jsonPath: String) = JsonUtils.select(this, jsonPath)
fun String.extractValues(jsonPath: String): Collection<String> {
    val res = JsonUtils.select(this, jsonPath)
    if(res.startsWith("[")) {
        return res.removePrefix("[")
            .removeSuffix("]")
            .split(",")
            .map {
                it.trimIndent()
                    .removePrefix("\"")
                    .removeSuffix("\"")
            }
    }
    return emptyList()
}


fun String.updateJson(jsonPath: String, value: String) = JsonUtils.updateJson(this, jsonPath, value)

fun String.updateJson(jsonPath: String, value: Int) = JsonUtils.updateJson(this, jsonPath, value)
fun String.updateJson(jsonPath: String, value: Double) = JsonUtils.updateJson(this, jsonPath, value)

fun String.deleteJson(jsonPath: String) = JsonUtils.deleteJson(this, jsonPath)