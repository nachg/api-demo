package com.apidemo.api.realworld.constants

object Global {
    val profile = System.getenv("stand") ?: "local"
}