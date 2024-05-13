package com.apidemo.api.realworld.dto

import com.apidemo.util.ddt.TestCaseWrapper
import com.fasterxml.jackson.annotation.JsonIgnore

data class ArticleCreateRequest(var article: Article = Article()) {
    data class Article(
        var title: String = "",
        var description: String = "",
        var body: String = "",
        var tagList: List<String> = emptyList(),
    )
}

data class ArticleUpdateRequest(
    @JsonIgnore
    var id: String = "",
    var article: Article = Article()
) {
    data class Article(
        var title: String? = null,
        var description: String? = null,
        var body: String? = null,
    )
}

data class ArticleModel(
    var id: String? = null,
    var slug: String? = null,
    var title: String? = null,
    var description: String? = null,
    var body: String? = null,
    var tagList: List<String> = emptyList(),
    var createdAt: String? = null,
    var updatedAt: String? = null,
    var favorited: Boolean? = null,
    var favoritesCount: Long? = null,
    var author: ProfileModel? = null,
)

data class ArticleResponse(var article: ArticleModel)

data class ArticlesResponse(var articles: List<ArticleModel>, var articlesCount: Int)

data class TagsResponse(var tags: List<String>)

data class GetAllRequest(
    var limit: Int = 10,
    var offset: Int = 0,
    var tag: String? = null,
    var author: String? = null,
)

fun TestCaseWrapper.request(f: ArticleCreateRequest.Article.() -> Unit) {
    this.request = {
        ArticleCreateRequest(
            ArticleCreateRequest.Article().apply(f)
        )
    }
}

fun TestCaseWrapper.response(f: ArticleModel.() -> Unit) {
    this.response = {
        ArticleResponse(
            ArticleModel().apply(f)
        )
    }
}