package com.apidemo.api.realworld.endpoints.api.tags

import com.apidemo.api.realworld.dto.TagsResponse
import com.apidemo.util.Endpoint
import com.apidemo.util.annotations.EndpointInfo

class Tags(parent: Endpoint): Endpoint("/tags", parent) {
    @EndpointInfo(title = "Получение списка тэгов", responseCode = 200, authRequired = false)
    fun getAll() = helper.get<TagsResponse>(url)
}