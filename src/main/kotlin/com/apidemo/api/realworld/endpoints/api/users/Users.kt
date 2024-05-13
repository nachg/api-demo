package com.apidemo.api.realworld.endpoints.api.users

import com.apidemo.api.realworld.dto.LoginRequest
import com.apidemo.api.realworld.dto.UserModel
import com.apidemo.api.realworld.dto.UserResponse
import com.apidemo.util.Endpoint

class Users(parent: Endpoint): Endpoint("/users", parent) {
    fun login(request: LoginRequest) = helper.post<UserResponse>(url = "$url/login", request = request)
}