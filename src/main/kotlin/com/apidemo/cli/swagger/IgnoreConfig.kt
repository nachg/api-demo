package com.apidemo.cli.swagger

data class IgnoreConfig(
    val ignorePatters: Collection<String> = emptyList(),
    val ignoreEndpoints: Collection<UrlLog> = emptyList()
)
