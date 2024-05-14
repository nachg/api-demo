package com.apidemo.util.nt

import com.apidemo.util.ApiHelper
import com.apidemo.util.ApiHelperDelegate
import com.apidemo.util.ResponseWrapper
import com.apidemo.util.db.DBCls
import com.apidemo.util.db.config.Connections
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.insert
import org.xpathqs.log.Log
import org.xpathqs.log.style.StyleFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.time.*

@OptIn(ExperimentalTime::class)
class BaseNtTest<T>(
    val parallelDataProvider: DataProvider<T>,
    val singleDataProvider: DataProvider<T> = DataProvider(parallelDataProvider.data.subList(0,1)),
    private val enableLog: Boolean = false
) {
    fun run(method: TestMethod<T>, config: Config) {
        val singleDurations = mutableListOf<Duration>()
        val multipleDurations = mutableListOf<Duration>()

        Log.info("singleDataProvider size: ${singleDataProvider.data.size}")
        Log.info("parallelDataProvider size: ${parallelDataProvider.data.size}")

        if(singleDataProvider.data.isEmpty()) {
            throw Exception("singleDataProvider is empty")
        }
        if(parallelDataProvider.data.isEmpty()) {
            throw Exception("parallelDataProvider is empty")
        }

        var startSingle = 0L
        Log.enabled = enableLog
        var singleErrorCount = 0
        val allSingle = measureTime {
            Log.action(StyleFactory.testTitle("Выполнение однопоточного теста")) {
                startSingle = System.currentTimeMillis()
                repeat(config.singleThreadRetryCount) {
                    singleDurations.add(
                        measureTime {
                            Log.info("Executing: $it")
                            if(runCatching {
                                (method.lambda(singleDataProvider) as? ResponseWrapper<*>)?.response?.let {
                                    if(!it.statusCode.toString().startsWith("2")) {
                                        singleErrorCount++
                                    }
                                }
                            }.isFailure) {
                                singleErrorCount++
                            }
                        }
                    )
                }
            }
        }

        forceLog {
            Log.info(
                StyleFactory.result("Single avg: ${singleDurations.avg}")
            )
            Log.info(
                StyleFactory.result("all Single: ${allSingle.inWholeMilliseconds}")
            )
            Log.info(StyleFactory.testTitle("Выполнение многопоточного теста"))
        }

        val service = Executors.newFixedThreadPool(config.threadCount)

        var startParallel: Long
        var parallelErrorCount = 0
        try {
            val taskList = ArrayList<Callable<Duration>>()
            startParallel = System.currentTimeMillis()
            repeat(config.threadCount) {
                taskList.add(
                    Callable<Duration> {
                        measureTime {
                            if(runCatching {
                                (method.lambda(parallelDataProvider) as? ResponseWrapper<*>)?.response?.let {
                                    if(!it.statusCode.toString().startsWith("2")) {
                                        parallelErrorCount++
                                    }
                                }
                            }.isFailure) {
                                parallelErrorCount++
                            }
                        }
                    }
                )
            }

            val allParallel = measureTime {
                service.invokeAll(taskList).forEach {
                    multipleDurations.add(
                        it.get()
                    )
                }
            }

            forceLog {
                Log.info(
                    StyleFactory.result("Multiple avg: ${multipleDurations.avg}")
                )
                Log.info(
                    StyleFactory.result("All multiple: ${allParallel.inWholeMilliseconds}")
                )
            }

            DBCls(Connections.nt).transaction {
                addLogger(StdOutSqlLogger)

                MetricsTable.insert {
                    it[startTime] = Instant.ofEpochMilli(startSingle)
                    it[MetricsTable.method] = method.title
                    it[mode] = "single"
                    it[avg_ts] = singleDurations.avg
                    it[min_ts] = singleDurations.min
                    it[max_ts] = singleDurations.max
                    it[version] = "latest"
                    it[comment] = ""
                    it[count] = config.singleThreadRetryCount
                    it[total_execution_ts] = allSingle.inWholeMilliseconds
                    it[errors] = singleErrorCount
                }

                MetricsTable.insert {
                    it[startTime] = Instant.ofEpochMilli(startParallel)
                    it[MetricsTable.method] = method.title
                    it[mode] = "parallel"
                    it[avg_ts] = multipleDurations.avg
                    it[min_ts] = multipleDurations.min
                    it[max_ts] = multipleDurations.max
                    it[version] = "latest"
                    it[comment] = ""
                    it[count] = config.threadCount
                    it[total_execution_ts] = allParallel.inWholeMilliseconds
                    it[errors] = parallelErrorCount
                }

                val item = parallelDataProvider.data.first()
                val apiProp = (item as Any).javaClass.kotlin.members.first { it.name == "api" }

                parallelDataProvider.data.forEach {item ->
                    val helper = (apiProp.call(item) as ApiHelperDelegate).apiHelper
                    val messages = helper.messages
                    messages.forEach { msg ->
                        LogMessages.insert {
                            it[ts] = Instant.ofEpochMilli(msg.ts)
                            it[type] = msg.type
                            it[LogMessages.method] = msg.method
                            it[url] = msg.url
                            it[code] = msg.code
                            it[message] = msg.message
                        }
                    }
                }
            }
        } finally {
            service.shutdown()
        }

        if(config.internalRunFor > java.time.Duration.ZERO) {
            var parallelErrorCount = 0
            var requestWithoutResponse = 0
            var successRequest = 0

            val service = Executors.newFixedThreadPool(config.threadCount)
            try {

                val taskList = ArrayList<Callable<Duration>>()
                val durationsOfInternalRun = Collections.synchronizedList(ArrayList<Duration>())
                val durationsErrorsOfInternalRun = Collections.synchronizedList(ArrayList<Duration>())
                val unrespondedList = Collections.synchronizedList(ArrayList<Pair<Long,String>>())

                startParallel = System.currentTimeMillis()
                repeat(config.threadCount) {
                    taskList.add(
                        Callable<Duration> {
                            measureTime {
                                while((System.currentTimeMillis() - startParallel) < config.internalRunFor.toMillis()) {
                                    val t1 = System.currentTimeMillis()
                                    runCatching {
                                        (method.lambda(parallelDataProvider) as? ResponseWrapper<*>)?.response?.let {
                                            if(!it.statusCode.toString().startsWith("2")) {
                                                val ts = (System.currentTimeMillis() - t1).toDuration(DurationUnit.MILLISECONDS)
                                                forceLog {
                                                    Log.error("Got error from NT test, status: ${it.statusCode}, body: ${it.body.asString()}, time: $ts")
                                                }
                                                durationsErrorsOfInternalRun.add(ts)
                                                parallelErrorCount++
                                            } else {
                                                durationsOfInternalRun.add(
                                                    (System.currentTimeMillis() - t1).toDuration(DurationUnit.MILLISECONDS)
                                                )
                                                successRequest++
                                            }
                                        }
                                    }.run {
                                        if(isFailure) {
                                            unrespondedList.add(System.currentTimeMillis() to ApiHelper.lastUrlCall())

                                            val ts = (System.currentTimeMillis() - t1).toDuration(DurationUnit.MILLISECONDS)
                                            durationsErrorsOfInternalRun.add(ts)
                                            forceLog {
                                                Log.error("Got request failed from NT test. Message: ${this.exceptionOrNull()?.message}, time: $ts")
                                                println(
                                                    this.exceptionOrNull()?.stackTraceToString()
                                                )
                                            }
                                            requestWithoutResponse++
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                forceLog {
                    Log.trace(StyleFactory.testTitle("Выполнение тестов параллельно в течении ${config.internalRunFor}"))
                }

                val startContinual = System.currentTimeMillis()
                val allParallel = measureTime {
                    service.invokeAll(taskList).forEach {
                        multipleDurations.add(
                            it.get()
                        )
                    }
                }

                forceLog {
                    Log.info(StyleFactory.result("All internal: $allParallel"))
                    Log.info(StyleFactory.result("Errors: $parallelErrorCount"))
                    Log.info(StyleFactory.result("Success Request: $successRequest"))
                    Log.info(StyleFactory.result("Request Without Response: $requestWithoutResponse"))
                }

                val item = parallelDataProvider.data.first()
                val apiProp = (item as Any).javaClass.kotlin.members.first { it.name == "api" }

                DBCls(Connections.nt).transaction {
                    addLogger(StdOutSqlLogger)

                    MetricsTable.insert {
                        it[startTime] = Instant.ofEpochMilli(startContinual)
                        it[MetricsTable.method] = method.title
                        it[mode] = "continual"
                        it[avg_ts] = durationsOfInternalRun.avg
                        it[min_ts] = durationsOfInternalRun.min
                        it[max_ts] = durationsOfInternalRun.max
                        it[unresponded] = requestWithoutResponse
                        it[version] = "latest"
                        it[requestsCount] = successRequest + parallelErrorCount + requestWithoutResponse
                        it[comment] = "Success Request: $successRequest; Duration: ${config.internalRunFor}"
                        it[count] = config.threadCount
                        it[total_execution_ts] = allParallel.inWholeMilliseconds
                        it[errors] = parallelErrorCount
                    }

                    parallelDataProvider.data.forEach { item ->
                        val helper = (apiProp.call(item) as ApiHelperDelegate).apiHelper
                        val messages = helper.messages
                        messages.forEach { msg ->
                            LogMessages.insert {
                                it[ts] = Instant.ofEpochMilli(msg.ts)
                                it[type] = msg.type
                                it[LogMessages.method] = msg.method
                                it[url] = msg.url
                                it[code] = msg.code
                                it[message] = msg.message
                            }
                        }
                    }

                    unrespondedList.forEach { ts ->
                        UnrespondedRequests.insert {
                            it[url] = ts.second
                            it[LogMessages.ts] = Instant.ofEpochMilli(ts.first)
                        }
                    }
                }
            } finally {
                service.shutdown()
            }

        }
    }

    private fun forceLog(lambda: ()->Unit) {
        Log.enabled = true
        lambda()
        Log.enabled = enableLog
    }

    val List<Duration>.avg: Long
        get() {
            if(this.isEmpty()) return 0
            return this.sumOf { it.inWholeMilliseconds } / this.size
        }

    val List<Duration>.max: Long
        get() {
            if(this.isEmpty()) return 0
            return this.maxOf { it.inWholeMilliseconds }
        }

    val List<Duration>.min: Long
        get() {
            if(this.isEmpty()) return 0
            return this.minOf { it.inWholeMilliseconds }
        }
}