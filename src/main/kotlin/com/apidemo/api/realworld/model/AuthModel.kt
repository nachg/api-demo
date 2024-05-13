package com.apidemo.api.realworld.model

import com.apidemo.api.realworld.API
import com.apidemo.api.realworld.RealWorldApi
import com.apidemo.api.realworld.constants.User
import com.apidemo.api.realworld.steps.AuthSteps
import com.apidemo.util.CachedModel
import org.xpathqs.cache.base.fromCache
import org.xpathqs.log.Log
import java.time.Duration

object AuthModel : CachedModel() {
    fun getToken(user: User, api: RealWorldApi = API) : String {
        return fromCache(
            key = "token${user.login}",
            liveTime = Duration.ofMinutes(10),
            updateInitTime = true
        ) {
            synchronized(user) {
                Log.action("Получение токена авторизации") {
                    AuthSteps(
                        api = api,
                        user = user
                    ).getAuthToken()
                }
            }
        }
    }
}