package com.apidemo.api.tests.endpoints.tags

import com.apidemo.api.base.AuthorizedApiTestCase
import com.apidemo.api.realworld.dto.TagsResponse
import com.apidemo.util.ddt.response
import com.apidemo.util.ddt.testCase
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.testng.annotations.Test
import org.xpathqs.log.Log

@Feature("Endpoints")
class GetAllTest : AuthorizedApiTestCase() {

    @Story("Tags/GetAll/+")
    @Test
    fun getTags() {
        checkEndpoint(
            endpoint = api.tags::getAll,
            testcase = testCase("Метод /tags должен возвращать список тэгов") {
                response {
                    Log.action("Список тэгов не должен быть пустым") {
                        val actual = it as TagsResponse
                        actual.tags.isNotEmpty()
                    }
                }
            }
        )
    }
}