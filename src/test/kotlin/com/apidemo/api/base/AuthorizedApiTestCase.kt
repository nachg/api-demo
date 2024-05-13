package com.apidemo.api.base

import com.apidemo.api.realworld.API
import com.apidemo.api.realworld.RealWorldApi
import com.apidemo.api.realworld.constants.User
import com.apidemo.api.realworld.constants.Users
import com.apidemo.api.realworld.endpoints.api.Api
import com.apidemo.api.realworld.steps.AuthSteps
import com.apidemo.util.ApiTestCase
import org.testng.annotations.BeforeClass

open class AuthorizedApiTestCase(
    private val realWorldApi: RealWorldApi = API,
    val user: User = Users.default1
) : ApiTestCase() {
    val api = realWorldApi.api

    @BeforeClass
    fun auth() {
        AuthSteps(
            api = realWorldApi,
            user = user
        ).initAuthHeaders()
    }
}