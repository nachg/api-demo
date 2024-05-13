package com.apidemo.cli.swagger

fun main() {
    val urlLog = UrlLogLoader().parse()

    Swaggers.realWorld.check(urlLog)
}