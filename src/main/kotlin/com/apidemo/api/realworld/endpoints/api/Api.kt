package com.apidemo.api.realworld.endpoints.api

import com.apidemo.api.realworld.endpoints.api.articles.Articles
import com.apidemo.api.realworld.endpoints.api.tags.Tags
import com.apidemo.api.realworld.endpoints.api.users.Users
import com.apidemo.util.Endpoint

class Api(parent: Endpoint): Endpoint("/api/", parent) {
    val articles = Articles(this)
    val tags = Tags(this)
    val users = Users(this)
}