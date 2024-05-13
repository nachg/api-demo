package com.apidemo.api.tests.func

import com.apidemo.api.base.AuthorizedApiTestCase
import com.apidemo.api.realworld.steps.RealWorldContext
import com.apidemo.api.realworld.steps.RealWorldContext.Companion.context
import org.testng.annotations.Test

class DemoTest : AuthorizedApiTestCase() {

    @Test
    fun test() {
        context {
            create {
                body = "specific body"
            }
            //lastAction = RealWorldContext.CREATE
        }
    }
}