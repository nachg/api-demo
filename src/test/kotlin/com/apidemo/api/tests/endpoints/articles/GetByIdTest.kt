package com.apidemo.api.tests.endpoints.articles

import com.apidemo.api.base.AuthorizedApiTestCase
import com.apidemo.api.realworld.dto.systemError
import com.apidemo.api.realworld.model.ArticlesCache
import com.apidemo.util.ddt.request
import com.apidemo.util.ddt.testCase
import io.qameta.allure.Feature
import org.testng.annotations.Test
import java.util.UUID
import com.apidemo.api.realworld.dto.response
import io.qameta.allure.Story

@Feature("Endpoints")
class GetByIdTest : AuthorizedApiTestCase() {

    @Story("Articles/GetById/+")
    @Test
    fun `Запрос по номеру статьи`() {
        checkEndpoint(
            endpoint = api.articles::get,
            testcase = testCase("Запрос по номеру статьи") {
                request {
                    ArticlesCache.withTag.slug!!
                }
                response {
                    slug = ArticlesCache.withTag.slug
                    tagList = ArticlesCache.withTag.tagList
                }
            }
        )
    }

    @Story("Articles/GetById/-")
    @Test
    fun `Ошибка 404 при запросе несуществующей заявки`() {
        checkEndpointError(
            endpoint = api.articles::get,
            testcase = testCase("Ошибка 404 при запросе несуществующей заявки") {
                request {
                    UUID.randomUUID().toString()
                }
                systemError {
                    status = 404
                    error = "Not Found"
                    path = "/api/articles/${requestResult.toString()}"
                }
            }
        )
    }
}