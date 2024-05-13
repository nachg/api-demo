package com.apidemo.util

import org.xpathqs.log.Log
import kotlin.reflect.KProperty

open class JsonFields(
    private var source: String,
) {
    fun getResult() = source

    inner class Prop(private val json: String) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
            val json = if(thisRef is Block) thisRef.json + json else json
            return source.selectJson(json)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Any?) {
            val json = if(thisRef is Block) thisRef.json + json else json
            if(runCatching {
                source = when(value) {
                    is Double -> source.updateJson(json, value)
                    is Int -> source.updateJson(json, value)
                    else -> source.updateJson(json, value.toString())
                }
            }.isFailure) {
                Log.error("Json set error for the: $json")
            }
        }

        //TODO: implement remove value
        fun delete() {}

        fun readAll() : Collection<String> {
            return source.extractValues(json)
        }
    }

    open inner class Block(
        val parentJson: String,
        var parent: Block? = null
    ) {
        fun delete() {
            source = source.deleteJson(json)
        }

        var modifiedJson = ""

        val json: String
            get() {
                val l = (parent?.let {
                    parent!!.json
                } ?: "")

                var r = parentJson
                if(modifiedJson.isNotEmpty()) {
                    r = modifiedJson
                    modifiedJson = ""
                }

                return l + r
            }
    }


    companion object {
        inline fun<reified T: JsonFields> String.updateJson(f: T.() -> Unit) : T {
            return T::class.constructors.first {
                it.parameters.size == 1
            }.call(this).apply(f)
        }
    }
}

operator fun <T : JsonFields.Block> T.get(pos: Int) = get(pos.toString())
operator fun <T : JsonFields.Block> T.get(pos: String) : T {
    modifiedJson = "$parentJson[$pos]"
    return this
}