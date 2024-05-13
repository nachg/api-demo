package com.apidemo.api

import com.apidemo.api.realworld.API
import com.apidemo.util.ApiTestCase
import com.apidemo.util.Smoke
import org.testng.annotations.Test

@Smoke
class SmokeTest : ApiTestCase() {
    @Test
    fun checkServiceAlive() {
        API.api.tags.getAll()()
    }
}