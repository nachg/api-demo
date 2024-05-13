package com.apidemo.api.base

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import com.apidemo.api.realworld.API
import com.apidemo.util.ApiTestCase
import com.apidemo.util.Endpoint
import com.apidemo.util.ResponseWrapper
import com.apidemo.util.annotations.EndpointInfo
import io.qameta.allure.Feature
import io.qameta.allure.Story
import org.testng.SkipException
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation

@Feature("ACL")
open class AclTest(private val root: Endpoint) : ApiTestCase() {

    val authorized = ArrayList<Endpoint.EndpointContext>()
    val notAuthorized = ArrayList<Endpoint.EndpointContext>()
    val undefined = ArrayList<Endpoint.EndpointContext>()

    data class MethodContext(
        val obj: Any,
        val method: KFunction<*>
    )

    @Story("Authorized Access")
    @Test(dataProvider = "authorizedMethods")
    fun testAuth(context : MethodContext) {
        val method = context.method
        val response = when (method.parameters.size) {
            2 -> {
                val request = (method.parameters.last().type.classifier as KClass<*>).createInstance()
                method.call(context.obj, request)
            }
            1 -> {
                method.call(context.obj)
            }
            else -> {
                SkipException("У метода должен быть один аргумент")
            }
        }

        assertThat((response as ResponseWrapper<*>).response.statusCode, "Код ответа должен быть 401")
            .isEqualTo(401)
    }


    @Story("Not Authorized Access")
    @Test(dataProvider = "notAuthMethods")
    fun notAuthorized(context : MethodContext) {
        val method = context.method
        val response = when (method.parameters.size) {
            2 -> {
                val request = (method.parameters.last().type.classifier as KClass<*>).createInstance()
                method.call(context.obj, request)
            }
            1 -> {
                method.call(context.obj)
            }
            else -> {
                SkipException("У метода должен быть один аргумент")
            }
        }

        assertThat((response as ResponseWrapper<*>).response.statusCode, "Код ответа должен быть 401")
            .isNotEqualTo(401)
    }

    @DataProvider
    fun authorizedMethods() = authorized.flatMap { m ->
        m.methods.map {
            MethodContext(
                m.obj, it
            )
        }
    }.toTypedArray()

    @DataProvider
    fun notAuthMethods() = notAuthorized.flatMap { m ->
        m.methods.map {
            MethodContext(
                m.obj, it
            )
        }
    }.toTypedArray()

    @BeforeClass
    fun grabEndpoints() {
        root.getInnerEndpoints().forEach {
            val auth = ArrayList<KFunction<*>>()
            val nonAuth = ArrayList<KFunction<*>>()
            val undef = ArrayList<KFunction<*>>()

            it.methods.forEach { m ->
                val endpointInfo = m.findAnnotation<EndpointInfo>()
                if(endpointInfo != null) {
                    if(endpointInfo.authRequired) {
                        auth.add(m)
                    } else {
                        nonAuth.add(m)
                    }
                } else {
                    undef.add(m)
                }
            }

            if(auth.isNotEmpty()) {
                authorized.add(
                    Endpoint.EndpointContext(
                        it.obj,
                        auth
                    )
                )
            }
            if(nonAuth.isNotEmpty()) {
                notAuthorized.add(
                    Endpoint.EndpointContext(
                        it.obj,
                        nonAuth
                    )
                )
            }
            if(undef.isNotEmpty()) {
                undefined.add(
                    Endpoint.EndpointContext(
                        it.obj,
                        nonAuth
                    )
                )
            }
        }
    }
}