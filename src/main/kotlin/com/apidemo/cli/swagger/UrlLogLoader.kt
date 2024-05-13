package com.apidemo.cli.swagger

import java.io.File

class UrlLogLoader(
    private val path: String = "build${File.separator}urllog${File.separator}"
) {
    fun parse(): Collection<UrlLog> {
        val lines = File(path).walkTopDown().filter {
            it.isFile
        }.flatMap {
            it.readLines()
        }

        return lines.map {
            it.split(" ").run {
                UrlLog(
                    method = first().uppercase(),
                    path = last().replace("{uuid}", "{}").replace("{num}", "{}").removeSuffix("/")
                )
            }
        }.distinct().toList()
    }
}