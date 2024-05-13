package com.apidemo.util

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.apidemo.util.ApiHelper.Companion.EXPECTED_JSON
import com.apidemo.util.SerializationHelper.toPrettyJson
import com.apidemo.util.annotations.EndpointInfo
import com.apidemo.util.ddt.TestCase
import io.qameta.allure.Allure
import io.qameta.allure.Epic
import org.testng.ITest
import org.testng.SkipException
import org.testng.annotations.AfterTest
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Listeners
import org.xpathqs.log.Log
import org.xpathqs.gwt.GIVEN
import org.xpathqs.gwt.When
import org.xpathqs.log.style.StyleFactory
import org.xpathqs.log.style.StyleFactory.arg
import org.xpathqs.log.style.StyleFactory.result
import org.xpathqs.log.style.StyleFactory.text
import java.lang.reflect.Method
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties

@Target(AnnotationTarget.CLASS)
annotation class Smoke

@Epic("API-тесты")
@Listeners(SmokeTestListener::class)
open class ApiTestCase : ITest {

    open fun setUp() {}
    open fun cleanUp() {}

    @BeforeClass
    fun init() {
        initLog()
        Loader.loadProperties()
        if(SmokeTestListener.smokeTestFailure) {
            throw SkipException("Smoke tests fails")
        }
    }

    private fun initLog() {
        Log.log = ApiLogger
        GIVEN.log = ApiLogger
    }

    companion object {
        val testCase = ThreadLocal<TestCase>()
        val testCaseName = ThreadLocal<String>()
    }

    override fun getTestName(): String {
        return testCaseName.get() ?: ""
    }

    @Synchronized
    @BeforeMethod(alwaysRun = true)
    open fun setTestName(method: Method, row: Array<Any?>?) {
        try {
            val tc = row!![0] as TestCase
            testCase.set(tc)
            testCaseName.set(tc.info.name)
        } catch (e: Exception) {
            testCaseName.set(method.name)
        }
    }

    @Synchronized
    @BeforeMethod(dependsOnMethods = ["setTestName"])
    fun logSeparator(method: Method) {
        Log.info(
            StyleFactory.testTitle("                    ${testCaseName.get() +""}                   ")
        )
    }

    fun checkEndpoint(
        endpoint: KCallable<*>,
        testcase: TestCase,
        code: Int? = null)
    {
        val lifecycle = Allure.getLifecycle()
        lifecycle.updateTestCase { testResult ->
            testResult.name = testCaseName.get()
        }

        if(testcase.wrapper.hasBeforeRequest()) {
            Log.action("Предусловие") {
                testcase.wrapper.given = testcase.wrapper.beforeRequest()
            }
        }

        val info = endpoint.annotations.find { it is EndpointInfo } as? EndpointInfo
        val title = info?.title ?: "API вызов к ${endpoint.name}"

        var code = code ?: testcase.wrapper.responseCode ?: info?.responseCode

        val res =  Log.action("Отправить $title") {
            val request: Any? = try {
                testcase.request()
            } catch (s: SkipException) {
                throw s
            } catch (e: Exception) {
                null
            }

            if(request != null) {
                testcase.wrapper.requestResult = request
                endpoint.call(request) as ResponseWrapper<*>
            } else {
                endpoint.call()
            }
        } as ResponseWrapper<*>

        if(testcase.wrapper.hasAfterRequest()) {
            Log.action("Постусловие") {
                testcase.wrapper.afterRequest()
            }
        }

        Log.action("Проверка результата") {
            if(code != null) {
                Log.action("Код ответа должен быть $code") {
                    assertThat(res.response.statusCode)
                        .isEqualTo(code)
                }
                Log.action("Проверка тела ответа") {
                    if(testcase.wrapper.lambdaResponse != null) {
                        val actual = res()

                        if(testcase.wrapper.dumpResponse) {
                            Log.tag(actual.toPrettyJson(), EXPECTED_JSON)
                        }

                        assertThat(
                            testcase.wrapper.lambdaResponse!!(actual)
                        ).isEqualTo(true)
                    } else {
                        ResponseValidator(
                            expected = testcase.wrapper.getResponse(),
                            actual = res()
                        ).validate(testcase.wrapper)
                    }
                }
            }
        }

        Log.trace("--------------------------------")
    }

    fun getErrorCls(info: EndpointInfo?): KClass<*> {
        return info!!.errorCls
    }

    fun checkEndpointError(endpoint: KCallable<*>, testcase: TestCase, code: Int? = null) {
        val lifecycle = Allure.getLifecycle()
        lifecycle.updateTestCase { testResult ->
            testResult.name = testCaseName.get()
        }

        if(testcase.wrapper.hasBeforeRequest()) {
            Log.action("Предусловие") {
                testcase.wrapper.beforeRequest()
            }
        }

        val info = endpoint.annotations.find { it is EndpointInfo } as? EndpointInfo
        val title = info?.title ?: "API вызов к ${endpoint.name}"

        var code = code ?: testcase.wrapper.responseCode ?: info?.errorCode

        val res = Log.action("Отправить $title") {
            val request: Any = testcase.request()
            testcase.wrapper.requestResult = request
            endpoint.call(request) as ResponseWrapper<*>
        }
        Log.action("Проверка результата") {
            val errorCls = getErrorCls(info)
            if(code != null) {
                Log.action("Код ответа должен быть $code") {
                    assertThat(res.response.statusCode)
                        .isEqualTo(code)
                }
                Log.action("Проверка тела ответа") {
                    if(testcase.wrapper.lambdaResponse != null) {
                        assertThat(
                            testcase.wrapper.lambdaResponse!!(res.getError(errorCls))
                        ).isEqualTo(true)
                    } else {
                        if(testcase.wrapper.isResponseSet) {

                            ResponseValidator(
                                expected = testcase.wrapper.getResponse(),
                                actual = res.getError(errorCls)
                            ).validate(testcase.wrapper)
                        }
                    }
                }
            }
        }

        Log.trace("--------------------------------")
    }

    @AfterTest
    fun afterAll() {
        ApiHelper.saveLogs()
    }
}


fun<G: Any, W> When<G, W>.shouldBeEqual(vararg pairs: Pair<Any?, Any?>) : Boolean {
    return mapResponse(this.given, *pairs)
}

fun mapResponse(source: Any, vararg pairs: Pair<Any?, Any?>) : Boolean {
    assertAll {
        pairs.forEach {
            val name = getMemberName(source = source, member = it.first)
            Log.action(
                text("Проверка поля ") + arg(name)
                        + text("\nОжидаемое значение : ") + result(it.first.toString())
                        + text("\nАктуальное значение: ") + result(it.second.toString())
            ) {
                assertThat(it.first, name)
                    .isEqualTo(it.second)
            }
        }
    }
    return true
}

fun getMemberName(source: Any, member: Any?, prefix: String = "") : String {
    val res = source::class.declaredMemberProperties.firstOrNull {
        it.getter.call(source) === member
    }?.name
    if(res != null) {
        return prefix + res
    }
    source::class.declaredMemberProperties.filter {
        !it.getter.call(source)!!.javaClass.kotlin.qualifiedName!!.startsWith("kotlin.")
    }.forEach {
        val res = getMemberName(it.getter.call(source)!!, member, prefix + it.name + ".")
        if(res.isNotEmpty()) {
            return prefix + res
        }
    }
    return ""
}