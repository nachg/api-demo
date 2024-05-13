package com.apidemo.util

import com.apidemo.api.realworld.constants.Global
import org.xpathqs.log.Log
import org.xpathqs.log.noLog
import org.xpathqs.prop.PropScanner
import org.xpathqs.prop.impl.NoValueProcessor
import org.xpathqs.prop.impl.ProcessDate
import org.xpathqs.prop.impl.ProcessVars

object Loader {
    fun loadProperties() {
        noLog {
            scan(
                "com.apidemo.api.realworld.constants" to "realworld/constants"
            )
        }
    }

    private fun scan(vararg args: Pair<String, String>) {
        args.forEach {
            try {
                scan(it.first, it.second)
            } catch (e: Exception) {
                Log.error("No resource for ${it.first} and ${it.second}")
                throw e
            }
        }
    }

    private fun scan(pack: String, path: String) {
        val profile = Global.profile
        PropScanner(
            rootPackage = pack,
            resourceRoot = "properties/$profile/$path",
            valueProcessor =
            ProcessEnv(
                ProcessVars(
                    ProcessDate(
                        NoValueProcessor()
                    )
                )
            )
        ).scan()
    }
}