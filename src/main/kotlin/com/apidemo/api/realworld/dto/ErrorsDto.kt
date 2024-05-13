package com.apidemo.api.realworld.dto

import com.apidemo.util.ddt.TestCaseWrapper
import java.util.HashMap

data class ErrorsDto(
    val errors: Map<String, Collection<String>>
)

fun TestCaseWrapper.error(f: HashMap<String, Collection<String>>.() -> Unit) {
    this.response = {
        ErrorsDto(
            HashMap<String, Collection<String>>().apply(f)
        )
    }
}

data class SystemErrorDto(
    var timestamp: String? = null,
    var status: Int? = null,
    var error: String? = null,
    var path: String? = null
)

fun TestCaseWrapper.systemError(f: SystemErrorDto.() -> Unit) {
    this.response = {
        SystemErrorDto().apply(f)
    }
}