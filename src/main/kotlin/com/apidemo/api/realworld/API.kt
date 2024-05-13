package com.apidemo.api.realworld

import com.apidemo.api.realworld.constants.Url
import com.apidemo.api.realworld.constants.Users
import com.apidemo.api.realworld.dto.ArticleCreateRequest
import com.apidemo.api.realworld.dto.LoginRequest
import com.apidemo.api.realworld.endpoints.api.Api
import com.apidemo.api.realworld.steps.AuthSteps
import com.apidemo.util.*
import org.xpathqs.gwt.GIVEN
import org.xpathqs.log.Log
import kotlin.reflect.full.declaredMemberProperties

object API : RealWorldApi()

open class RealWorldApi : Endpoint(), ApiHelperDelegate {
    override val selfUrl: String
        get() = Url.realworld

    val api = Api(this)

    var isAuthorized = false

    override val apiHelper by lazy {ApiHelper()}
}