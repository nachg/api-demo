package com.apidemo.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode


class JsonReplace(
    val rootNode: JsonNode
) {
    constructor(json: String)
            : this(SerializationHelper.mapper.readTree(json))

    fun update(key: String, v: Any): JsonReplace {
        val parent = key.substringBeforeLast("/")
        val attr = key.substringAfterLast("/")
        val parentNode = rootNode.at(parent) as ObjectNode

        when (v) {
            is String -> parentNode.put(attr, v)
            is Int -> parentNode.put(attr, v)
            is Long -> parentNode.put(attr, v)
            is Float -> parentNode.put(attr, v)
            is Double -> parentNode.put(attr, v)
        }

        return this
    }

    override fun toString(): String =
        SerializationHelper.mapper.writeValueAsString(rootNode)
}