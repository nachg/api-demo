package com.apidemo.api.tests.func.articles

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.apidemo.api.base.AuthorizedApiTestCase
import com.apidemo.api.realworld.dto.ArticleCreateRequest
import com.apidemo.api.realworld.dto.GetAllRequest
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.testng.annotations.Test
import org.xpathqs.gwt.GIVEN
import java.util.*

@Feature("Func")
class ArticlesTest : AuthorizedApiTestCase() {

    @Story("Articles")
    @Test
    fun `Создание Удаление и Поиск по тэгу`() {
        GIVEN("Новый тэг") {
            UUID.randomUUID().toString()
        }.WHEN("Создана статья с этим тэгом") {
            api.articles.create(
                ArticleCreateRequest(
                    ArticleCreateRequest.Article(
                        title = UUID.randomUUID().toString(),
                        body = "body",
                        description = "description",
                        tagList = listOf(given)
                    )
                )
            )().article
        }.THEN("Тэг должен быть в списке тэгов") {
            assertThat(
                api.tags.getAll()().tags
            ).contains(given)
        }.THEN("Поиск по новому тэгу должен вернуть одну созданную статью") {
            api.articles.getAll(GetAllRequest(tag = given))().apply {
                assertThat(articlesCount)
                    .isEqualTo(1)
                assertThat(articles.first().slug)
                    .isEqualTo(actual.slug)
            }
        }.THEN("Удаление созданной статьи не должно удалять тэг") {
            api.articles.delete(actual.slug!!)
            assertThat(
                api.tags.getAll()().tags
            ).contains(given)
        }.THEN("После удаления поиск по тэгу должен вернуть пустой результат") {
            api.articles.getAll(GetAllRequest(tag = given))().apply {
                assertThat(articlesCount)
                    .isEqualTo(0)
                assertThat(articles)
                    .isEmpty()
            }
        }
    }

    fun t1() {
        GIVEN("expression") {
            2 to 3
        }.WHEN("call plus operator") {
            given.first + given.second
        }.THEN("Result should contain sum") {
            assertThat(actual)
                .isEqualTo(5)
        }
    }
}