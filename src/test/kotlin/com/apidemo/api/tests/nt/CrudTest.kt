package com.apidemo.api.tests.nt

import com.apidemo.api.realworld.steps.RealWorldContext.Companion.context
import com.apidemo.api.tests.nt.util.DataNt
import com.apidemo.util.nt.BaseNtTest
import com.apidemo.util.nt.Config
import com.apidemo.util.nt.TestMethod
import java.time.Duration

fun main() {
    BaseNtTest(
        parallelDataProvider = DataNt.build(),
        enableLog = false
    ).run(
        method = TestMethod(
            title = "crud",
            //Тестируемый метод
            lambda = {
                it.next().run {
                    context(api = api) {}
                }
            },
        ),
        config = Config(
            singleThreadRetryCount = 10, //количество запусков метода в однопоточном режиме для вычисления среднего времени выполнения
            threadCount = 4, //Количество потоков одновременно выполняющих тестируемый метод
            internalRunFor = Duration.ofMinutes(1)
        )
    )
}