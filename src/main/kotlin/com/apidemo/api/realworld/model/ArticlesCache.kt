package com.apidemo.api.realworld.model

import com.apidemo.api.realworld.API
import com.apidemo.api.realworld.dto.ArticleCreateRequest
import com.apidemo.api.realworld.endpoints.api.Api
import com.apidemo.util.CachedModel
import org.xpathqs.cache.base.fromCache
import java.time.Duration
import java.util.*

object ArticlesCache: ArticleModelCls()
open class ArticleModelCls(
    val api: Api = API.api
) : CachedModel() {

    val withTag = fromCache(
        key = "withTag",
        liveTime = Duration.ofDays(10)
    ) {
        api.articles.create(
            ArticleCreateRequest(
                article = ArticleCreateRequest.Article(
                    title = UUID.randomUUID().toString(),
                    description = "description",
                    body = "body",
                    tagList = listOf("tag1")
                ),
            )
        )().article!!
    }
}