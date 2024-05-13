package com.apidemo.api.realworld.endpoints.api.articles

import com.apidemo.api.realworld.dto.*
import com.apidemo.util.Endpoint
import com.apidemo.util.annotations.EndpointInfo
import com.apidemo.util.common.NoBodyResponse

class Articles(parent: Endpoint): Endpoint("/articles", parent) {
    @EndpointInfo(title = "GET запрос на получение статьи", responseCode = 200, errorCode = 404, errorCls = SystemErrorDto::class, authRequired = false)
    fun get(id: String = "") = helper.get<ArticleResponse>(url = "$url/$id")

    @EndpointInfo(title = "PUT запрос на изменение статьи", responseCode = 200, errorCode = 404, errorCls = SystemErrorDto::class, authRequired = true)
    fun put(request: ArticleUpdateRequest = ArticleUpdateRequest()) = helper.put<ArticleResponse>(url = "$url/${request.id}", request = request)

    @EndpointInfo(title = "GET запрос на получение списка статей", responseCode = 200, errorCode = 422, errorCls = ErrorsDto::class, authRequired = false)
    fun getAll(request: GetAllRequest = GetAllRequest()) = helper.get<ArticlesResponse>(url = url, params = request)

    @EndpointInfo(title = "POST запрос на Создание новой статьи", responseCode = 200, errorCode = 422, errorCls = ErrorsDto::class, authRequired = true)
    fun create(request: ArticleCreateRequest = ArticleCreateRequest()) = helper.post<ArticleResponse>(url = url, request = request)

    @EndpointInfo(title = "DELETE запрос на удаление статьи", responseCode = 204, authRequired = true)
    fun delete(id: String = "") = helper.delete<NoBodyResponse>(url = "$url/$id")
}