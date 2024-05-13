package com.apidemo.util.stepchain

import assertk.assertAll
import assertk.fail
import org.xpathqs.log.Log

open class StepChainContext<out T:StepChainContext<T>> {
    protected val actions = ArrayList<StepAction<@UnsafeVariance T>>()

    private var assertLambda: (()->Unit)? = null

    open fun setup(){}
    open fun tearDown(){}

    fun newAction(
        title: String,
        needToSkip: ((T) -> Boolean) = {false},
        action: Lambda<T>
    ) : StepAction<@UnsafeVariance T> {
        return StepAction(
            title = title,
            needToSkip = needToSkip,
            action = action
        ).apply {
            actions.add(this)
        }
    }

    fun action(key: String, lambda: Lambda<T>) : T {
        actions[key]?.apply {
            action = lambda
        }
        return this as T
    }

    fun beforeAction(key: String, lambda: Lambda<T>) : T{
        actions[key]?.apply {
            beforeAction = lambda
        }
        return this as T
    }

    fun afterAction(key: String, lambda: Lambda<T>) : T{
        actions[key]?.apply {
            afterAction = lambda
        }
        return this as T
    }

    fun removeAction(key: String) : T{
        actions[key]?.let {
            actions.remove(it)
        }
        return this as T
    }

    fun assert(lambda: ()->Unit) : T {
        assertLambda = lambda
        return this as T
    }

    fun run() : T {
        setup()
        getExecutionList().forEach { executeAction(it) }
        if(assertLambda != null) {
            Log.action("Блок проверок") {
                assertAll {
                    assertLambda!!()
                }
            }
        }
        tearDown()
        return this as T
    }

    var firstAction = ""
    var lastAction = ""

    private fun getExecutionList(): MutableList<StepAction<@UnsafeVariance T>> {
        val start = firstAction.ifEmpty { actions.first().title }
        val end = lastAction.ifEmpty { actions.last().title }

        return actions.subList(
            fromIndex = actions.indexOf(actions[start]),
            toIndex = actions.indexOf(actions[end]) + 1
        )
    }

    fun remove(actionName: String) {
        actions[actionName]?.let {
            actions.remove(it)
        }
    }

    private fun executeAction(action: StepAction<T>) {
        Log.step(action.title) {
            this as T
            action.beforeAction?.invoke(this)

            if(!action.needToSkip(this)) {
                action.action(this)

                action.retryLambda?.let { lambda ->
                    repeat(action.retryCount + 1) {
                        if(!lambda.invoke(this)) {
                            Log.error("Action ${action.title} executed with error, retrying...")
                            action.action(this)
                        } else {
                            return@let
                        }
                    }
                    val msg = "Action ${action.title} was not able to complete without errors"
                    if(action.failOnError) {
                        fail(msg)
                    } else {
                        Log.error(msg)
                    }
                }

                action.afterAction?.invoke(this)

                action.assertLambda?.let {
                    assertAll {
                        Log.action("Проверка результата") {
                            it.invoke()
                        }
                    }
                }
            }
        }
    }

    private operator fun List<StepAction<T>>.get(key: String): StepAction<T>? {
        return this.find { it.title == key }
    }
}