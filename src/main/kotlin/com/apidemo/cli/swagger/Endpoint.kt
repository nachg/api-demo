package com.apidemo.cli.swagger

data class Endpoint(
    val path: String,
    var get: Boolean = false,
    var put: Boolean = false,
    var post: Boolean = false,
    var delete: Boolean = false,
    var options: Boolean = false,
    var patch: Boolean = false
) {
    fun toUrlLog() = buildList {
        if (get) add(UrlLog(path, "GET"))
        if (put) add(UrlLog(path, "PUT"))
        if (post) add(UrlLog(path, "POST"))
        if (delete) add(UrlLog(path, "DELETE"))
        if (options) add(UrlLog(path, "OPTIONS"))
        if (patch) add(UrlLog(path, "PATCH"))
    }
}