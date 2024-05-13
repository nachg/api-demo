package com.apidemo.api.tests.acl

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.apidemo.api.realworld.constants.User
import com.apidemo.api.realworld.constants.Users
import com.apidemo.api.realworld.dto.ArticleModel
import com.apidemo.api.realworld.dto.ArticleUpdateRequest
import com.apidemo.api.realworld.dto.GetAllRequest
import com.apidemo.api.realworld.steps.AuthorizedApi
import com.apidemo.api.realworld.steps.createAuthorizedApi
import com.apidemo.util.ApiTestCase
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.testng.annotations.Test
import org.xpathqs.gwt.GIVEN

@Feature("ACL")
class GetArticleTest : ApiTestCase() {

    @Story("Articles/Put")
    @Test
    fun `Ошибка 403 для PUT запроса`() {
        GIVEN("Статьи от двух авторизованных пользователей") {
            getUserArticle(Users.default1) to getUserArticle(Users.default2)
        }.WHEN("При попытке отправить запросы на изменение не своей статье") {
            given.first.api.api.api.articles.put(ArticleUpdateRequest(
                id = given.second.article.slug!!,
                article = ArticleUpdateRequest.Article(
                    body = "update"
                )
            )) to
            given.second.api.api.api.articles.put(ArticleUpdateRequest(
                id = given.first.article.slug!!,
                article = ArticleUpdateRequest.Article(
                    body = "update"
                )
            ))
        }.THEN("Должны получить 403 ошибки") {
            assertAll {
                assertThat(actual.first.response.statusCode)
                    .isEqualTo(403)
                assertThat(actual.second.response.statusCode)
                    .isEqualTo(403)
            }
        }
    }

    data class UserArticle(
        val api: AuthorizedApi,
        val article: ArticleModel
    )

    private fun getUserArticle(user: User) : UserArticle {
        val api = createAuthorizedApi(user)
        val article = api.api.api.articles.getAll(
            GetAllRequest(author = api.user.username)
        )().articles.first()
        return UserArticle(
            api, article
        )
    }
}