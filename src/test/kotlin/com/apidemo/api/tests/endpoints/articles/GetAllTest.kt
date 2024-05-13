package com.apidemo.api.tests.endpoints.articles

import com.apidemo.api.base.AuthorizedApiTestCase

import com.apidemo.api.realworld.dto.ArticlesResponse
import com.apidemo.api.realworld.dto.GetAllRequest
import com.apidemo.api.realworld.model.ArticlesCache
import com.apidemo.util.ddt.TestCase
import com.apidemo.util.ddt.TestCaseWrapper
import com.apidemo.util.ddt.testCase
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import com.apidemo.util.ddt.response
import org.xpathqs.log.Log

@Feature("Endpoints")
class GetAllTest : AuthorizedApiTestCase() {

    @Story("Articles/GetAll/+")
    @Test(dataProvider = "positiveTest")
    fun test(tc: TestCase) = checkEndpoint(api.articles::getAll, tc)

    @DataProvider
    fun positiveTest(): Array<TestCase> {
        return arrayOf(
            testCase("Получение 10 первых статей") {
                request {
                    offset = 0
                    limit = 10
                }
                response {
                    val actual = it as ArticlesResponse
                    Log.action("Результат должен состоять из 10 статей") {
                        actual.articles.size == 10
                    }
                }
            },
            testCase("Поиск по существующему тэгу") {
                request {
                    tag = ArticlesCache.withTag.tagList.first()
                }
                response {
                    val given = requestResult as GetAllRequest
                    val actual = it as ArticlesResponse
                    Log.action("Результат должен содержать список статей с запрошенным тэгом '${given.tag}'") {
                        actual.articles.isNotEmpty() && actual.articles.none {
                            !it.tagList.contains(given.tag)
                        }
                    }
                }
            },
            testCase("Поиск по не существующему тэгу") {
                request {
                    tag = "not_exist"
                }
                response {
                    val actual = it as ArticlesResponse
                    Log.action("Результат должен содержать пустой список статей") {
                        actual.articles.isEmpty() && actual.articlesCount == 0
                    }
                }
            },
            testCase("Поиск по существующему автору") {
                request {
                    author = user.username
                }
                response {
                    val given = requestResult as GetAllRequest
                    val actual = it as ArticlesResponse
                    Log.action("Результат должен содержать список статей автора") {
                        actual.articles.isNotEmpty()
                                && actual.articles.none { it.author?.username != given.author}
                    }
                }
            },
            testCase("Поиск по не существующему автору") {
                request {
                    author = "not_exist"
                }
                response {
                    val actual = it as ArticlesResponse

                    Log.action("Результат должен содержать пустой список статей") {
                        actual.articles.isEmpty() && actual.articlesCount == 0
                    }
                }
            },
            testCase("Оффсет превышающий количество элементов") {
                request {
                    offset = 10_000_000
                }
                response {
                    val actual = it as ArticlesResponse
                    Log.action("Результат должен содержать пустой список статей") {
                        actual.articles.isEmpty()
                    }
                }
            },
        )
    }

    fun TestCaseWrapper.request(f: GetAllRequest.() -> Unit) {
        this.request = {
            GetAllRequest().apply(f)
        }
    }
}