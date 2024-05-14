package com.apidemo.api.tests.nt.util

import com.apidemo.api.realworld.API.apiHelper
import com.apidemo.api.realworld.RealWorldApi
import com.apidemo.api.realworld.constants.User
import com.apidemo.api.realworld.constants.Users
import com.apidemo.api.realworld.steps.createAuthorizedApi
import com.apidemo.util.ApiLogger
import com.apidemo.util.Loader
import com.apidemo.util.nt.DataProvider
import org.xpathqs.log.Log

data class RealWorldContext<T>(
    val api: RealWorldApi,
    val args: DataProvider<T>
)

object DataNt {
    init {
        Loader.loadProperties()
        Log.log = ApiLogger
    }

    private val userList = listOf(
        Users.default1,
        Users.default2
    )

    fun build() = build<Any>(
        argsLambda = null
    )

    fun<T> build(
        users: List<User> = userList,
        argsLambda: ((api: RealWorldApi) -> List<T>)?
    ) = DataProvider(
        users.map {
            createAuthorizedApi(it).apply {
                apiHelper.addErrorMessages = true
                apiHelper.validateResponse = false
            }
        }.map { api ->
            RealWorldContext(
                api = api.api,
                args = DataProvider(
                   argsLambda?.invoke(api.api) ?: emptyList()
                )
            )
        }
    )
}
