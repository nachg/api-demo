package com.apidemo.api.realworld.steps

import com.apidemo.api.realworld.API
import com.apidemo.api.realworld.RealWorldApi
import com.apidemo.api.realworld.constants.User
import com.apidemo.api.realworld.constants.Users
import com.apidemo.api.realworld.dto.ArticleCreateRequest
import com.apidemo.api.realworld.dto.ArticleUpdateRequest
import com.apidemo.api.realworld.endpoints.api.articles.Articles
import com.apidemo.util.stepchain.StepChainContext
import java.util.UUID

class RealWorldContext(
    val user: User = Users.default1,
    val api: RealWorldApi = API
) : StepChainContext<RealWorldContext>(
) {
    val articles: Articles
        get() = api.api.articles

    private var slug = ""
    private var createRequest = ArticleCreateRequest(
        ArticleCreateRequest.Article().apply {
            title = UUID.randomUUID().toString()
            description = "description"
            body = "body"
        }
    )

    fun create(f: ArticleCreateRequest.Article.()->Unit) {
        createRequest.article.f()
    }

    init {
        newAction(
            title = AUTHORIZE,
            needToSkip = { api.isAuthorized }
        ) {
            AuthSteps(
                api = api,
                user = user
            ).initAuthHeaders()
        }

        newAction(
            title = CREATE,
        ) {
            slug = articles.create(
                createRequest
            )().article.slug!!
        }

        newAction(
            title = UPDATE,
        ) {
            articles.put(
                ArticleUpdateRequest(
                    id = slug,
                    ArticleUpdateRequest.Article(
                        body = "Updated body"
                    )
                )
            )
        }

        newAction(
            title = DELETE,
        ) {

        }
    }
    companion object {
        const val AUTHORIZE = "Авторизация"
        const val CREATE = "Создание новой статьи"
        const val UPDATE = "Обновление статьи"
        const val DELETE = "Удаление статьи"

        fun context(
            user: User = Users.default1,
            api: RealWorldApi = API,
            f: RealWorldContext.() -> Unit
        ): RealWorldContext {
            return RealWorldContext(
                user = user,
                api = api
            ).apply(f).run()
        }
    }
}