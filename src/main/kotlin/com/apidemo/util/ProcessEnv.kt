package com.apidemo.util

import org.xpathqs.prop.base.IValueProcessor
import org.xpathqs.prop.impl.ValueProcessorDecorator

class ProcessEnv(source: IValueProcessor) : ValueProcessorDecorator(source) {
    override fun selfProcess(obj: Any): Any {
        if(obj is String) {
            if(obj.contains("\${")) {
                val start = obj.indexOf("\${")
                val end = obj.indexOf("}", start + 2)
                val varName = obj.substring(start + 2, end)
                if(System.getenv(varName) != null) {
                    val varValue = System.getenv(varName)
                    return obj.replace("\${$varName}", varValue)
                }
            }
        }
        return obj
    }
}