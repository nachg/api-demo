package com.apidemo.cli.swagger

import com.apidemo.cli.swagger.Endpoint
import io.swagger.v3.parser.OpenAPIV3Parser
import java.math.MathContext
import kotlin.reflect.jvm.jvmName

class SwaggerCheck(
    private val config: SwaggerConfig,
) {
    init {
        System.setProperty(io.swagger.v3.parser.util.RemoteUrl::class.jvmName + ".trustAll", "true")
    }

    private lateinit var urlLog: Collection<UrlLog>

    fun urlLogToEndpoints(): Collection<Endpoint> {
        val urlLogMap = HashMap<String, Endpoint>()

        urlLog
            .filter { config.filterUrl(it.path) }
            .forEach {
                urlLogMap.getOrPut(it.path) {
                    Endpoint(
                        path = config.replace(it.path)
                    )
                }.apply {
                    when (it.method) {
                        "GET" -> get = true
                        "POST" -> post = true
                        "PUT" -> put = true
                        "PATCH" -> patch = true
                        "DELETE" -> delete = true
                        "OPTIONS" -> options = true
                    }
                }
            }

        return urlLogMap.values
    }

    fun check(urlLog: Collection<UrlLog>) {
        this.urlLog = urlLog
        val regEx = "\\{.*?\\}".toRegex()
        val swagger = OpenAPIV3Parser().readLocation(config.url, null, null).openAPI
        var endpoints = swagger.paths.map {
            Endpoint(
                path = it.key.replace(regEx, "{}"),
                get = it.value.get != null,
                put = it.value.put != null,
                post = it.value.post != null,
                patch = it.value.patch != null,
                delete = it.value.delete != null,
                options = it.value.options != null,
            )
        }

        if (config.include.isNotEmpty()) {
            endpoints = endpoints.filter { ep ->
                config.include.any {
                    ep.path.startsWith(it)
                }
            }
        }

        if (config.ignoreConfig.ignorePatters.isNotEmpty()) {
            endpoints = endpoints.filter { ep ->
                config.ignoreConfig.ignorePatters.none {
                    ep.path.startsWith(it)
                }
            }
        }

        if (config.ignoreConfig.ignoreEndpoints.isNotEmpty()) {
            endpoints = endpoints.filter { ep ->
                config.ignoreConfig.ignoreEndpoints.none {
                    //TODO: add method check
                    ep.path == (it.path)
                }
            }
        }

        val logEndpoints = urlLogToEndpoints()

        val notImplemented = ArrayList<UrlLog>()
        val implemented = ArrayList<UrlLog>()

        endpoints.forEach { swagger ->
            val exist = logEndpoints.firstOrNull {
                it.path == swagger.path
            }
            if (exist == null) {
                notImplemented.addAll(swagger.toUrlLog())
            } else {
                val s = swagger.toUrlLog()
                val l = exist.toUrlLog()
                notImplemented.addAll(
                    s subtract l
                )
                implemented.addAll(
                    l intersect s
                )
            }
        }
        printResults(implemented, notImplemented)
    }

    fun printResults(implemented: Collection<UrlLog>, notImplemented: Collection<UrlLog>) {
        val i = implemented.size
        val n = notImplemented.size
        val t = i + n
        println("Swagger check results of '${config.name}' service on '${config.stand}' stand")
        println("Total endpoints: $t")
        println("Covered: $i")
        println("Not covered: $n")

        val cov = ((i.toDouble() / t) * 100).toBigDecimal(MathContext(4))
        println("Coverage: ${cov.toPlainString()} %")

        println()
        println("Not implemented: ")
        notImplemented.sortedBy { it.path }.forEach {
            println(it.path + " " + it.method)
        }

        println()
        println("Implemented: ")
        implemented.sortedBy { it.path }.forEach {
            println(it.path + " " + it.method)
        }
    }
}











