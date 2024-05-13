package com.apidemo.util.common

import com.apidemo.util.JsonReplace
import com.apidemo.util.ddt.TestCaseWrapper

data class BodyWithIdDto(
    var id: String = "",
    var body: Any? = null
)

fun TestCaseWrapper.request(l: BodyWithIdDto.() -> Unit) {
    this.request = {
        BodyWithIdDto().apply { l() }
    }
}

fun TestCaseWrapper.request(dto: BodyWithIdDto, l: () -> Collection<Pair<String, Any>>) {
    this.request = {
        val jr = JsonReplace(dto.body as String)
        val kv = l()
        kv.forEach {
            jr.update(it.first, it.second)
        }
        dto.body = jr.toString()
        dto
    }
}