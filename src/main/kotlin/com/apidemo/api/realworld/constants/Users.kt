package com.apidemo.api.realworld.constants

import org.xpathqs.prop.Model

@Model
object Users {
    val default1 = User()
    val default2 = User()
}

data class User(
    val username: String = "test_1",
    val login: String = "test_1@email.com",
    val password: String = "test_1@email.com"
)