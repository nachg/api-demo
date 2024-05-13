package com.apidemo.util.ddt

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.apidemo.util.ApiTestCase
import com.apidemo.util.JsonReplace
import com.apidemo.util.ResponseWrapper
import io.qameta.allure.Allure
import org.xpathqs.log.Log
import org.xpathqs.gwt.GIVEN
import org.xpathqs.gwt.When
import org.xpathqs.log.style.StyledString

fun testCase(name: String, tmsId: String = "", builder: TestCaseWrapper.() -> Unit): TestCase {
    val res = TestCaseWrapper()
    res.builder()

    return TestCase(
        info = TestCaseInfo(name, tmsId),
        wrapper = res
    )
}

class TestCase(
    val info: TestCaseInfo,
    val wrapper: TestCaseWrapper
) {
    inline fun<reified T: Any> request(): T {
        return wrapper.request() as T
    }

    override fun toString() = "_${System.currentTimeMillis()}"
}

class TestCaseInfo(
    val name: String,
    val tmsId: String = ""
)

class TestCaseWrapper() {
    lateinit var request: ()->Any
    lateinit var beforeRequest: ()->Any?
    lateinit var afterRequest: ()->Any?
    lateinit var response: ()->Any

    var precondition: (()->Any)? = null

    var jsonResponse: (()->Any)? = null
    var lambdaResponse: ((Any)->Boolean)? = null

    var requestResult: Any? = null

    var given: Any? = null
    var responseCode: Int? = null
    var checkAll = false
    var useContains = false
    var dumpResponse = false

    var ignoreEmptyString = true
    var caseSensetive = false

    fun hasBeforeRequest() = ::beforeRequest.isInitialized
    fun hasAfterRequest() = ::afterRequest.isInitialized

    inline fun<reified T: Any> getPrecondition(): T {
        return precondition!!() as T
    }

    inline fun<reified T: Any> getRequest(): T {
        return request() as T
    }

    inline fun<reified T: Any> getResponse(): T {
        return response() as T
    }

    val isResponseSet: Boolean
       get() = ::response.isInitialized
}

fun TestCaseWrapper.request(l: () -> String) {
    this.request = {
        l()
    }
}

fun TestCaseWrapper.request(json: String, l: () -> Collection<Pair<String, Any>>) {
    this.request = {
        val jr = JsonReplace(json)
        val kv = l()
        kv.forEach {
            jr.update(it.first, it.second)
        }
        jr.toString()
    }
}

fun TestCaseWrapper.given(l: () -> Any?) {
    this.beforeRequest = l
}

fun TestCaseWrapper.before(l: () -> Unit) {
    this.beforeRequest = l
}

fun TestCaseWrapper.after(l: () -> Any?) {
    this.afterRequest = l
}

fun TestCaseWrapper.response(dumpResponse:Boolean = false, l: (Any) -> Boolean) {
    this.dumpResponse = dumpResponse
    this.lambdaResponse = l
}


fun <G:Any,W> When<G, W>.statusShouldBe(code: Int) {
    Log.action("Код ответа должен быть: $code") {
        assertThat((actual as ResponseWrapper<*>).response.statusCode())
            .isEqualTo(code)
    }
}

fun <G:Any,W> When<G, W>.THEN(f: When<G, W>.()->W) = THEN("Проверка результата", f)
fun <G:Any,W> When<G, W>.THEN(msg: String, f: When<G, W>.()->W) = THEN(StyledString(msg), f)
fun <G:Any,W> When<G, W>.THEN(msg: StyledString, f: When<G, W>.()->W): When<G, W> {
    val lifecycle = Allure.getLifecycle()
    lifecycle.updateTestCase { testResult ->
        testResult.name = ApiTestCase.testCaseName.get()
        testResult.fullName = ApiTestCase.testCaseName.get()
    }

    GIVEN.log.action(msg, GIVEN.THEN) {
        GIVEN.gwtAssert.equals(actual, f())
    }
    return this
}