package com.apidemo.api.tests.endpoints.articles

import com.apidemo.api.base.AuthorizedApiTestCase
import com.apidemo.api.realworld.dto.ArticleCreateRequest
import com.apidemo.api.realworld.dto.error
import com.apidemo.api.realworld.dto.request
import com.apidemo.api.realworld.dto.response
import com.apidemo.api.realworld.model.ArticlesCache
import com.apidemo.util.ddt.TestCase
import com.apidemo.util.ddt.testCase
import com.apidemo.util.shouldBeEqual
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import org.xpathqs.gwt.GIVEN
import java.util.*

@Feature("Endpoints")
class CreateTest : AuthorizedApiTestCase() {

    @Story("Articles/Create/+")
    @Test(dataProvider = "positiveTest")
    fun positive(tc: TestCase) = checkEndpoint(api.articles::create, tc)

    @Story("Articles/Create/-")
    @Test(dataProvider = "negativeTest")
    fun negative(tc: TestCase) = checkEndpointError(api.articles::create, tc)

    @DataProvider
    fun positiveTest(): Array<TestCase> {
        return arrayOf(
            testCase("Создание статьи без списка тэгов") {
                request {
                    title = UUID.randomUUID().toString()
                    description = "article description"
                    body = "article body"
                }
                response {
                    val given = (requestResult as ArticleCreateRequest).article
                    title = given.title
                    description = given.description
                    body = given.body
                    tagList = emptyList()
                }
            },
            testCase("Создание статьи со списком тэгов") {
                request {
                    title = UUID.randomUUID().toString()
                    description = "article description"
                    body = "article body"
                    tagList = listOf("tag1", "tag2")
                }
                response {
                    val given = (requestResult as ArticleCreateRequest).article
                    title = given.title
                    description = given.description
                    body = given.body
                    tagList = given.tagList
                }
            }
        )
    }

    @DataProvider
    fun negativeTest(): Array<TestCase> {
        return arrayOf(
            testCase("Создание статьи c пустыми полями") {
                request {}
                error {
                    this["description"] = listOf("can't be empty")
                    this["body"] = listOf("can't be empty")
                    this["title"] = listOf("can't be empty")
                }
            },

            testCase("Создание статьи c пустым названием") {
                request {
                    description = "article description"
                    body = "article body"
                }
                error {
                    this["title"] = listOf("can't be empty")
                }
            },

            testCase("Создание статьи c дубликатом в названии") {
                request {
                    title = ArticlesCache.withTag.title.toString()
                    description = "article description"
                    body = "article body"
                }
                error {
                    this["title"] = listOf("article name exists")
                }
            },
        )
    }
}
