package com.apidemo.api.tests.func

import com.apidemo.api.base.AuthorizedApiTestCase
import com.apidemo.api.realworld.steps.RealWorldContext
import com.apidemo.api.realworld.steps.RealWorldContext.Companion.context
import org.testng.annotations.Test
import org.xpathqs.log.Log

class ContextDemoTest : AuthorizedApiTestCase() {

    @Test
    fun test() {
        val slug = context {
            create {
                body = "specific body"
            }
            action(RealWorldContext.DELETE) {
                Log.info("override delete action")
                articles.delete(slug)
            }
        }
    }
}