package com.apidemo.cli.swagger

data class SwaggerConfig(
    val stand: String = "preprod",
    val name: String,
    val url: String,
    val include: Collection<String> = emptyList(),
    val ignoreConfig: IgnoreConfig = IgnoreConfig(),
    val replaceConfigs: Collection<ReplaceConfig>
) {
    fun replace(source: String): String {
        var res = source
        replaceConfigs.forEach {
            res = res.replace(it.replaceUrl, it.replaceBy)
        }
        return res
    }

    fun filterUrl(source: String) =
        replaceConfigs.firstOrNull {
            source.startsWith(it.replaceUrl)
        } != null
}