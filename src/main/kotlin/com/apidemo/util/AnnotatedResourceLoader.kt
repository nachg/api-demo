package com.apidemo.util

import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class FromResource(
    val path: String
)

interface AnnotatedResourceLoader {
    fun initProps(rootPath: String, defaultPath: String = "") {
        this::class.declaredMemberProperties.filter {
            it.hasAnnotation<FromResource>()
        }.forEach { prop ->
            val annotation = prop.findAnnotation<FromResource>()
            var content = this::class.java.classLoader.getResource(rootPath + annotation?.path)?.readText()
                ?: this::class.java.classLoader.getResource(defaultPath + annotation?.path)?.readText() ?: ""
            
            if(content.isNotEmpty() && prop is KMutableProperty<*>) {
                prop.setter.call(this, content)
            }
        }
    }
}