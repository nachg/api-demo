package com.apidemo.util

import assertk.fail
import com.apidemo.util.SerializationHelper.toJson
import com.apidemo.util.SerializationHelper.toKeyValueObject
import com.apidemo.util.SerializationHelper.toPrettyJson
import com.apidemo.util.SerializationHelper.toUrlParams
import com.apidemo.util.annotations.EndpointInfo
import com.apidemo.util.annotations.UNDEF

import io.qameta.allure.restassured.AllureRestAssured
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.http.Header
import io.restassured.http.Headers
import io.restassured.response.Response
import io.restassured.http.Cookies
import org.apache.http.HttpStatus
import org.xpathqs.framework.util.DateTimeUtil.NOW
import org.xpathqs.framework.util.DateTimeUtil.toStringValue
import org.xpathqs.log.Log
import org.xpathqs.log.style.StyleFactory
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.reflect.full.declaredMembers

data class ApiLog(
    val method: String,
    val url: String
)

open class ApiHelper {
    var headers: Headers = Headers()
    var cookies: Cookies = Cookies()

    fun clean() {
        headers = Headers()
        cookies = Cookies()
    }

    var messages = ArrayList<LogMessage>()
    var addErrorMessages = true

    var reauthorize = true

    private var authLambda: (() -> Any?)? = null
    private var beforeReauthLambda: (() -> Unit)? = null
    var validateResponse = true
    var checkResponseCodes = true
    var logUrl = false

    private var reauthRequestCount = 0
    private val REAUTH_MAX_ATTEMPT_COUNT = 2

    fun <T> auth(f: () -> T): T {
        authLambda = f
        return f()
    }

    fun onInvalidToken(f: () -> Unit) {
        beforeReauthLambda = f
    }

    fun getHeaders(token: String = ""): Headers {
        if (token.isEmpty()) {
            return headers
        }
        return Headers(Header("Authorization", token))
    }

    inline fun <reified T : Any> post(
        url: String,
        request: Any? = null,
        headers: Headers? = null,
        contentType: ContentType? = null,
        formParams: Boolean = false,
        noContentType: Boolean = false,
        additionalHeaders: Headers? = null,
        accept: String? = null,
        cookies: Cookies? = null
    ): ResponseWrapper<T> {
        if (request.containsFile()) {
            return ResponseWrapper(
                T::class.java,
                postImpFile(
                    url = url,
                    file = request!!.getFile(),
                    formParams = request.toKeyValue(),
                    headers = headers,
                    cookies = cookies as Cookies,
                    cls = T::class.java
                )
            )
        }
        //В строчке request.toJson есть бага в маппере, дублируются поля guaranteeCategory для запроса CreateGuaranteeRequest из IB
        //возможно связанно с тем что это поле в интерфейсе
        val r = if (request is String || request is ByteArray) request else /* request?.toPrettyJson()*/ request?.toKeyValue().toPrettyJson()
        return ResponseWrapper(
            T::class.java,
            postImp(
                url = url,
                jsonBody = r ?: "",
                headers = headers,
                contentType = if (noContentType) null else contentType ?: ContentType.JSON,
                formParam = formParams,
                additionalHeaders = additionalHeaders,
                accept = accept,
                cookies = cookies,
                cls = T::class.java
            ),
        )
    }

    inline fun <reified T : Any> postParams(
        url: String,
        params: Any,
        headers: Headers? = null,
        contentType: ContentType? = null,
        noContentType: Boolean = true,
    ): ResponseWrapper<T> {
        if (params.containsFile()) {
            return ResponseWrapper(
                T::class.java,
                postParamsImpl(
                    url = url,
                    file = params!!.getFile(),
                    params = params.toKeyValue(),
                    headers = headers,
                    cls = T::class.java
                )
            )
        }
        return ResponseWrapper(
            T::class.java,
            postParamsImpl(
                url = url,
                params = params.toKeyValue(),
                headers = headers,
                contentType = if (noContentType) null else contentType ?: ContentType.JSON,
                cls = T::class.java
            ),
        )
    }

    inline fun <reified T : Any> putParams(
        url: String,
        params: Any,
        headers: Headers? = null,
        contentType: ContentType? = null,
        noContentType: Boolean = true,
    ): ResponseWrapper<T> {

        return ResponseWrapper(
            T::class.java, putParamsImpl(
                url = url,
                params = params.toKeyValue(),
                headers = headers,
                contentType = if (noContentType) null else contentType ?: ContentType.JSON,
                cls = T::class.java
            )
        )
    }

    inline fun <reified T : Any> patch(
        url: String,
        request: Any? = null,
        headers: Headers? = null,
    ): ResponseWrapper<T> {

        return ResponseWrapper(
            cls = T::class.java,
            response = patchImp(
                url = url,
                jsonBody = if (request is String) request else request?.toPrettyJson() ?: "",
                headers = headers,
                cls = T::class.java
            )
        )
    }

    fun download(url: String): ByteArray {
        val resp = RestAssured.given()
            .relaxedHTTPSValidation()
            .headers(getHeaders())
            .`when`()
            .get(url)
            .andReturn()

        return resp.body.asByteArray()
    }

    inline fun <reified T : Any> get(
        url: String,
        headers: Headers? = null,
        additionalHeaders: Headers? = null,
        accept: String? = null,
        contentType: ContentType? = ContentType.JSON,
        params: Any? = null,
        cookies: Cookies? = null
    ) = ResponseWrapper<T>(
            T::class.java,
            getImp(
                url = url,
                headers = headers,
                additionalHeaders = additionalHeaders,
                accept = accept,
                contentType = contentType,
                params = params,
                cookies = cookies,
                cls = T::class.java
            )
        )

    inline fun <reified T : Any> put(
        url: String,
        request: Any? = null,
        headers: Headers? = null
    ) = ResponseWrapper<T>(
        cls = T::class.java,
        response = putImpl(
            url = url,
            jsonBody = if (request is String) request else request?.toPrettyJson() ?: "",
            headers = headers,
            cls = T::class.java
        )
    )

    inline fun <reified T : Any> delete(url: String) = ResponseWrapper<T>(
        cls = T::class.java,
        response = delete(
            url = url,
            headers = getHeaders(),
            cls = T::class.java
        )
    )

    fun postParamsImpl(
        url: String,
        params: Map<String, Any>,
        headers: Headers? = getHeaders(),
        contentType: ContentType? = ContentType.JSON,
        file: Pair<String, File>? = null,
        cookies: Cookies? = null,
        cls: Class<*>? = null
    ): Response {
        return executeResponse(
            url = url,
            method = "post",
            cls = cls
        ) {
            val headers = headers ?: getHeaders()
            var req = RestAssured.given()
                .baseUri(url)
                .headers(headers)
                .relaxedHTTPSValidation()
//            .filter(AllureRestAssured())

            if (file != null) {
                req = req.contentType(ContentType.MULTIPART)
                req = req.multiPart(file.first, file.second)
                params.forEach { (k, v) ->
                    req.param(k, v)
                }
            } else if (contentType != null) {
                req = req.contentType(contentType)
            }
            params.forEach { (k, v) ->
                req.param(k, v)
            }

            params.forEach { (k, v) ->
                req.param(k, v)
            }

            val cookies = cookies ?: this.cookies
            if (cookies.size() > 0) {
                req = req.cookies(cookies)
            }
            logRequest(url = url, method = "POST", headers = headers)
            val res = req.post()

            Log.action("Ответ:", "trace")
            {
                Log.trace(StyleFactory.text("Код: ") + StyleFactory.result(res.statusCode.toString()))
                Log.trace(StyleFactory.text("Тело: ") + StyleFactory.xpath(res.body.asString()))
            }
            res
        }
    }

    fun putParamsImpl(
        url: String,
        params: Map<String, Any>,
        headers: Headers? = getHeaders(),
        contentType: ContentType? = ContentType.JSON,
        cookies: Cookies? = null,
        cls: Class<*>? = null
    ): Response {
        return executeResponse(
            url = url,
            method = "put",
            cls = cls
        ) {
            val headers = headers ?: getHeaders()
            var req = RestAssured.given()
                .baseUri(url)
                .headers(headers)
                .relaxedHTTPSValidation()

            if (contentType != null) {
                req = req.contentType(contentType)
            }

            params.forEach { (k, v) ->
                req.param(k, v)
            }

            val cookies = cookies ?: this.cookies
            if (cookies.size() > 0) {
                req = req.cookies(cookies)
            }
            logRequest(url = url, method = "PUT", headers = headers)
            val res = req.put()

            Log.action("Ответ:", "trace") {
                Log.trace(StyleFactory.text("Код: ") + StyleFactory.result(res.statusCode.toString()))
                Log.trace(StyleFactory.text("Тело: ") + StyleFactory.xpath(res.body.asString()))
            }
            res
        }
    }

    fun postImpFile(
        url: String,
        file: Pair<String, File>,
        formParams: Map<String, Any>,
        headers: Headers? = getHeaders(),
        contentType: ContentType = ContentType.MULTIPART,
        cookies: Cookies? = null,
        cls: Class<*>? = null
    ): Response {
        return executeResponse(
            url = url,
            method = "post",
            cls = cls
        ) {
            val headers = headers ?: getHeaders()
            var req = RestAssured.given()
                .baseUri(url)
                .headers(headers)
                .relaxedHTTPSValidation()
                .contentType(contentType)
                .filter(AllureRestAssured())

            val cookies = cookies ?: this.cookies
            if (cookies.size() > 0) {
                req = req.cookies(cookies)
            }

            req = req.formParams(formParams).multiPart(file.first, file.second)

            val res = req.post()
            logRequest(url = url, method = "POST", headers = headers)
            Log.trace(StyleFactory.text("Параметры формы: ") + StyleFactory.xpath(formParams.toPrettyJson().toString()))

            Log.action("Ответ:", "trace") {
                Log.trace(StyleFactory.text("Код: ") + StyleFactory.result(res.statusCode.toString()))
                Log.trace(StyleFactory.text("Тело: ") + StyleFactory.xpath(res.body.toString().toPrettyJson()))
            }
            res
        }
    }

    fun patchImp(
        url: String,
        jsonBody: String = "",
        headers: Headers? = getHeaders(),
        contentType: ContentType = ContentType.JSON,
        cls: Class<*>
    ) = postImp(
        url = url,
        jsonBody = jsonBody,
        headers = headers,
        contentType = contentType,
        formParam = false,
        usePatch = true,
        cls = cls
    )

    fun postImp(
        url: String,
        jsonBody: Any,
        headers: Headers? = null,
        contentType: ContentType? = ContentType.JSON,
        formParam: Boolean = false,
        usePatch: Boolean = false,
        additionalHeaders: Headers? = null,
        accept: String? = null,
        cookies: Cookies? = null,
        cls: Class<*>? = null
    ): Response {
        return executeResponse(
            url = url,
            method = if(usePatch) "patch" else "post",
            cls = cls
        ) {
            var headers = headers ?: getHeaders()

            if (additionalHeaders != null) {
                headers = Headers(headers.asList() + additionalHeaders.asList())
            }

            var req = RestAssured.given()
                .relaxedHTTPSValidation()

            if (headers.size() > 0) {
                req = req.headers(headers)
            }

            if (cookies != null && cookies.size() > 0) {
                req = req.cookies(cookies)
            } else {
                if (this.cookies.size() > 0) {
                    req = req.cookies(this.cookies)
                }
            }

            if (accept != null) {
                req = req.accept(accept)
            }

            if (contentType != null) {
                req = req.contentType(contentType)
            }
            logRequest(url = url, method = if(usePatch) "PATCH" else "POST", headers = headers)
            if (formParam) {
                jsonBody.toKeyValue().forEach { entry ->
                    req.formParam(entry.key, entry.value)
                }
            } else {
                if (jsonBody is ByteArray) {
                    req = req.body(jsonBody)
                } else if (jsonBody.toString().isNotEmpty()) {
                    req = req.body(jsonBody)
                    Log.tag(StyleFactory.xpath(jsonBody.toString()), REQUEST_JSON)
                } else {
                    Log.info("Body is null")
                }
            }
            if (usePatch) {
                req.patch(url)
            } else {
                req.post(url)
            }
        }
    }

    private val uuidPattern = "\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}".toRegex()
    private val numPattern = "/[0-9]+".toRegex()
    private fun removeUrlParams(url: String): String {
        return url
            .substringBefore("?")
            .replace(uuidPattern, "{uuid}")
            .replace(numPattern, "/{num}")
    }

    private fun executeResponse(
        url: String,
        method: String,
        cls: Class<*>? = null,
        executeFunc: () -> Response,
    ): Response {
        val response = kotlin.runCatching { executeFunc() }
        if (response.isFailure) {
            Log.error("Failure response: ${response.exceptionOrNull()?.message}")
        }
        var res = response.getOrNull()
        urlCalls.add(
            ApiLog(
                method = method,
                url = removeUrlParams(url)
            )
        )
        res?.let {
            logResponse(res!!)
        }

        if(res?.statusCode == 403) {
            Log.error("403 code")
        }

        val endpointInfo = getEndpointInfo()
        if (endpointInfo != null) {
            if (res == null || (endpointInfo.responseCode != UNDEF && endpointInfo.responseCode != res.statusCode)) {
                if(res == null && cls?.simpleName != "NoBodyResponse")
                Log.error("Incorrect response")

                if (addErrorMessages && res != null) {
                    messages.add(
                        LogMessage(
                            url = url,
                            type = "error",
                            method = method,
                            code = res.statusCode,
                            message = res.asString()
                        )
                    )
                }

                if (this.validateResponse) {
                    val code = res?.statusCode ?: UNDEF
                    if (res == null
                        || code == HttpStatus.SC_GATEWAY_TIMEOUT
                        || code == HttpStatus.SC_BAD_GATEWAY
                        || code == HttpStatus.SC_SERVICE_UNAVAILABLE) {
                        var newResponse: Response? = null
                        var retryCount = 0
                        return Log.action("retrying") {
                            while (retryCount < REAUTH_MAX_ATTEMPT_COUNT) {
                                try {
                                    newResponse = executeFunc()
                                    logResponse(newResponse!!)
                                    if (newResponse != null && newResponse!!.statusCode == endpointInfo.responseCode) {
                                        break
                                    }
                                } catch (e: Exception) {
                                }
                                retryCount++
                            }
                            if(newResponse == null) {
                                fail("Запрос не был выполнен")
                            }
                            res = newResponse
                            res!!
                        }
                    } else {
                        if (checkResponseCodes && res?.statusCode != HttpStatus.SC_UNAUTHORIZED && authLambda == null) {
                            fail("[$method] $url. '${endpointInfo.title}' - Некорректный код ответа. \nОжидаемый: ${endpointInfo.responseCode} \nФактичеcкий ${res?.statusCode}")
                        }
                    }
                } else if(res == null) {
                    throw Exception("Response is null")
                }
            }
        }

        if (res!!.statusCode == HttpStatus.SC_UNAUTHORIZED && authLambda != null) {
            if (reauthorize) {
                Log.action("Обновление токена авторизации") {
                    beforeReauthLambda?.invoke()

                    var newResponse: Response? = null
                    while (reauthRequestCount < REAUTH_MAX_ATTEMPT_COUNT) {
                        try {
                            authLambda!!()
                            reauthRequestCount = 0
                            newResponse = executeFunc()
                            if (newResponse != null) {
                                logResponse(newResponse!!)
                                break
                            }
                        } catch (e: Exception) {
                            reauthRequestCount++
                        }
                    }
                    newResponse?.let {
                        res = it
                    }
                }
            } else {
                null
            }
        } else {
            res
        }

        return res!!
    }

    fun optionsImpl(url: String): Response {
        return executeResponse(url, "options") {
            var req = RestAssured.given()
                // .baseUri(url)
                .headers(headers)
                .relaxedHTTPSValidation()

            req.options(url)
        }
    }

    fun putImpl(
        url: String,
        jsonBody: String,
        headers: Headers? = null,
        contentType: ContentType = ContentType.JSON,
        cookies: Cookies? = null,
        cls: Class<*>? = null
    ): Response {
        return executeResponse(
            url = url,
            method = "put",
            cls = cls
        ) {
            val headers = headers ?: getHeaders()

            var request = RestAssured.given()
                .baseUri(url)
                .headers(headers)
                .relaxedHTTPSValidation()
                .contentType(contentType)

            val cookies = cookies ?: this.cookies
            if (cookies.size() > 0) {
                request = request.cookies(cookies)
            }

            if (jsonBody.isNotEmpty()) {
                request = request.body(jsonBody)
            }

            logRequest(url = url, method = "PUT", headers = headers)
            Log.tag(StyleFactory.xpath(jsonBody), REQUEST_JSON)

            request.put()
        }
    }

    fun getImp(
        url: String,
        headers: Headers? = null,
        contentType: ContentType? = ContentType.JSON,
        additionalHeaders: Headers? = null,
        accept: String? = null,
        params: Any? = null,
        cookies: Cookies? = null,
        cls: Class<*>? = null
    ): Response {
        return executeResponse(
            url = url,
            method = "get",
            cls = cls
        ) {
            var headers = headers ?: getHeaders()

            if (additionalHeaders != null) {
                headers = Headers(headers.asList() + additionalHeaders.asList())
            }

            var request = RestAssured.given()
                .headers(headers)
                .relaxedHTTPSValidation()

            if (contentType != null) {
                request = request.contentType(contentType)
            }

            val cookies = cookies ?: this.cookies
            if (cookies.size() > 0) {
                request = request.cookies(cookies)
            }

            if (accept != null) {
                request = request.accept(accept)
            }

            val url = if(params != null) url + params.toUrlParams() else url

            logRequest(url = url, method = "GET", headers = headers)
            request[url]
        }
    }

    private fun getEndpointInfo(): EndpointInfo? {
        Thread.currentThread().stackTrace.filter {
            it.className.startsWith("com.apidemo")
        }.forEach { ste ->
            val cls = this::class.java.classLoader.loadClass(ste.className)
            val method = cls.methods.firstOrNull { ste.methodName == it.name }
            val res = method?.annotations?.firstOrNull { it is EndpointInfo } as? EndpointInfo
            if (res != null) {
                return res
            }
        }
        return null
    }

    fun delete(
        url: String,
        headers: Headers = getHeaders(),
        contentType: ContentType = ContentType.JSON,
        cookies: Cookies? = null,
        cls: Class<*>? = null
    ): Response {
        return executeResponse(
            url = url,
            method = "delete",
            cls = cls
        ) {
            var req = RestAssured.given()
                .headers(headers)
                .relaxedHTTPSValidation()
                .contentType(contentType)
            //.filter(AllureRestAssured())

            val cookies = cookies ?: this.cookies
            if (cookies.size() > 0) {
                req = req.cookies(cookies)
            }

            logRequest(url = url, method = "DELETE", headers = headers)

            req.delete(url)
        }
    }

    private fun logRequest(url: String = "", method: String = "", headers: Headers? = null) {
        Log.info(StyleFactory.arg(method) + StyleFactory.text(" ") + StyleFactory.selectorName(url))

         /*headers?.forEach() {
             Log.tag("Заголовок: $it")
         }*/
    }

    private fun logResponse(res: Response) {
        //return

        Log.action("Ответ:", "trace") {
            Log.trace(StyleFactory.text("Код: ") + StyleFactory.result(res.statusCode.toString()))
            /*res.headers.forEach {
                Log.tag("Заголовок: " + it.toString())
            }*/
            try {
                Log.trace(
                    StyleFactory.text("Тело: \n") + StyleFactory.xpath(
                        res.body.asString()//.toKeyValueObject().toPrettyJson()
                    )
                )
            } catch (e: Exception) {
                Log.error("Unable to lo response")
            }
        }
    }

    fun Any?.containsFile(): Boolean {
        if (this == null) return false
        return this::class.declaredMembers.find {
            it.returnType.toString().startsWith("java.io.File")
        } != null
    }

    fun Any.getFile(): Pair<String, File> {
        return this::class.declaredMembers.filter {
            it.returnType.toString().startsWith("java.io.File")
        }.map {
            it.name to it.call(this) as File
        }.first()
    }

    companion object {
        const val REQUEST_JSON = "request-json"
        const val EXPECTED_JSON = "expected-json"

        val urlCalls = HashSet<ApiLog>()

        val LOG_PATH = "build${File.separator}urllog${File.separator}/${NOW.toStringValue() + File.separator}"

        fun saveLogs() {
            if(urlCalls.isEmpty()) return

            Paths.get(LOG_PATH).createDirectories()

            File(LOG_PATH + "urllog_${System.currentTimeMillis()}.txt").bufferedWriter().use { out ->
                urlCalls.forEach {
                    out.write(it.method + " " + it.url)
                    out.newLine()
                }
            }
        }
    }
}

fun Any.toKeyValue(): Map<String, Any> {
    return this.toJson().toKeyValueObject() as Map<String, Any>
}

fun Map<*, *>.toKvList(): List<Pair<String, String>> {
    val result = ArrayList<Pair<String, String>>()

    fun processSubmap(submap: Map<*, *>, prefix: String = "") {
        submap.forEach { (k, v) ->
            if (v is Map<*, *>) {
                processSubmap(
                    v,
                    if (prefix.isEmpty()) k.toString() else "$prefix.$k"
                )
            } else {
                val left = if (prefix.isEmpty()) k.toString() else "$prefix.$k"
                result.add(left to v.toString())
            }
        }
    }

    processSubmap(this)
    return result

}
