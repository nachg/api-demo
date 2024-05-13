package com.apidemo.api.realworld.steps

import com.apidemo.api.realworld.API
import com.apidemo.api.realworld.RealWorldApi
import com.apidemo.api.realworld.dto.LoginRequest
import com.apidemo.api.realworld.model.AuthModel
import com.apidemo.api.realworld.constants.User
import com.apidemo.api.realworld.constants.Users
import com.apidemo.util.runWithRetryOnFailure
import io.restassured.http.Cookies
import io.restassured.http.Header
import io.restassured.http.Headers
import org.xpathqs.log.Log
import java.time.Duration

open class AuthSteps(
    private var api: RealWorldApi = API,
    private val user: User = Users.default1,
    private val retryCount: Int = 5,
    private val retryDelaySec: Long = 10
) {

    private fun onAuthFailed() {
        AuthModel.delete()
        api.apiHelper.headers = Headers()
        api.apiHelper.cookies = Cookies()
    }

    fun initAuthHeaders() : String {
        return synchronized(user) {
            runWithRetryOnFailure(
                retryCount = retryCount,
                delay = Duration.ofSeconds(retryDelaySec),
                skipOnError = true,
                failLambda = {
                    Log.error("failed lambda called")
                    onAuthFailed()
                }
            ) {
                api.apiHelper.auth {
                    api.apiHelper.onInvalidToken {
                        Log.error("onInvalidToken called")
                        onAuthFailed()
                    }
                    setAuthHeader()
                }
            } ?: throw Exception("Authorization failed")
        }
    }

    private fun setAuthHeader() : String {
        val token = AuthModel.getToken(
            user = user,
            api = api
        )
        api.apiHelper.headers = Headers(
            Header("Authorization", "Token $token")
        )

        api.isAuthorized = true
        return token
    }

    fun getAuthToken() : String {
        return api.api.users.login(LoginRequest(
            LoginRequest.User(
                email = user.login,
                password = user.password
            )
        ))().user.token
    }
}

data class AuthorizedApi(
    val api: RealWorldApi,
    val user: User
)

fun createAuthorizedApi(user: User) : AuthorizedApi{
    val api = RealWorldApi()
    AuthSteps(
        api = api,
        user = user
    ).initAuthHeaders()

    return AuthorizedApi(
        api = api,
        user = user
    )
}