package com.apidemo.util

import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties

open class Endpoint(
    protected open val selfUrl: String = "",
    private val parent: Endpoint? = null,
) {
    open val url: String
        get() = (baseUrl + selfUrl)
            .replace("///", "/")
            .replace("//", "/")
            .replace(":/", "://")

    open val baseUrl: String
        get() {
            var base = (parent?.url ?: "")
            if(base.isNotEmpty()) {
                base += "/"
            }
            return base
        }

    val helper by lazy {
        var currentParent = this
        while (currentParent !is ApiHelperDelegate && currentParent.parent != null) {
            currentParent = currentParent.parent!!
        }
        (currentParent as? ApiHelperDelegate)?.apiHelper
            ?: throw Exception("parent is not an instance on the ApiHelper")
    }

    fun getInnerEndpoints(collection: ArrayList<EndpointContext> = ArrayList()) : ArrayList<EndpointContext> {
        val methods = this::class.declaredMemberFunctions.filter {
            it.returnType.toString().startsWith("com.apidemo.util.ResponseWrapper")
        }

        if(methods.isNotEmpty()) {
            collection.add(
                EndpointContext(
                    obj = this,
                    methods = methods
                )
            )
        }
        
        this::class.memberProperties.filter {
            runCatching {
                it.getter.call(this) is Endpoint
            }.getOrNull() == true
        }.forEach {
            (it.getter.call(this)!! as Endpoint).getInnerEndpoints(collection)
        }

        return collection
    }

    class EndpointContext(
        val obj: Endpoint,
        val methods: Collection<KFunction<*>>
    )

    /*
(this::class.declaredMemberFunctions.filter {
    it.returnType.toString().startsWith("com.apidemo.util.ResponseWrapper")
}.first().parameters.last().type.classifier as KClass<*>).createInstance()
     */
}

open class AbsoluteUrlEndpoint(
    selfUrl: String = "",
    parent: Endpoint? = null,
) : Endpoint(selfUrl, parent) {
    override val url: String
        get() = selfUrl
}

interface ApiHelperDelegate {
    val apiHelper: ApiHelper
}