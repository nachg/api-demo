package com.apidemo.api.tests.endpoints.articles

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.apidemo.api.base.AuthorizedApiTestCase
import com.apidemo.api.realworld.model.ArticleItems
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.testng.annotations.Test
import org.xpathqs.gwt.GIVEN

@Feature("Endpoints")
class DeleteTest : AuthorizedApiTestCase() {

    @Story("Articles/Delete/+")
    @Test
    fun `Удаление новой статьи`() {
        GIVEN("ID новой заявки") {
            ArticleItems.withTag.slug!!
        }.WHEN("Отправлен запрос на удаление") {
            api.articles.delete(given)
        }.THEN("Заявка должна быть удаленна") {
            assertThat(api.articles.get(given).response.statusCode)
                .isEqualTo(404)
        }
    }
}