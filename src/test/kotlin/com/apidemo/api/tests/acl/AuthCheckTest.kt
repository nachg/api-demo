package com.apidemo.api.tests.acl

import com.apidemo.api.base.AclTest
import com.apidemo.api.realworld.RealWorldApi
import io.qameta.allure.Feature

@Feature("ACL")
class AuthCheckTest : AclTest(RealWorldApi())