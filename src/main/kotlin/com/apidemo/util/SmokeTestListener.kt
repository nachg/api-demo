package com.apidemo.util

import org.testng.ITestContext
import org.testng.ITestListener
import org.testng.ITestResult
import kotlin.reflect.full.findAnnotation

class SmokeTestListener : ITestListener {
    override fun onTestFailure(result: ITestResult) {
        result.testClass.realClass.kotlin.findAnnotation<Smoke>()?.let {
            smokeTestFailure = true
        }
    }

    override fun onTestSkipped(p0: ITestResult?) {
    }

    override fun onTestFailedButWithinSuccessPercentage(p0: ITestResult?) {
    }

    override fun onStart(p0: ITestContext?) {
    }

    override fun onFinish(p0: ITestContext?) {
    }

    override fun onTestStart(p0: ITestResult?) {
    }

    override fun onTestSuccess(p0: ITestResult?) {
    }

    companion object {
        var smokeTestFailure = false
    }
}