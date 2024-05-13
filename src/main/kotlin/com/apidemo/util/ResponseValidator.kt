package com.apidemo.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.apidemo.util.annotations.Verification
import com.apidemo.util.ddt.TestCaseWrapper
import org.xpathqs.log.Log
import org.xpathqs.log.style.StyleFactory.arg
import org.xpathqs.log.style.StyleFactory.result
import org.xpathqs.log.style.StyleFactory.text
import java.math.BigDecimal
import kotlin.reflect.full.memberProperties

class ResponseValidator(
    private val expected: Any?,
    private val actual: Any?
) {

    fun validate(wrapper: TestCaseWrapper) {
        expected!!::class.memberProperties.filter {
            it.annotations.filterIsInstance<Verification.Ignore>().isEmpty() || wrapper.checkAll
        }.forEach {
            var actual = it.call(actual)
            var expected = it.call(expected)
            if(actual != null && expected != null) {
                if(actual!!.javaClass.name.startsWith("com.apidemo")) {
                    ResponseValidator(
                        actual = actual,
                        expected = expected
                    ).validate(wrapper)
                } else if (actual is Map<*,*> && expected is Map<*,*>) {
                    AllModelComparator(
                        expected = expected,
                        actual = actual
                    ).compare()
                } else {
                    Log.action(text("Проверка поля '") + arg(it.name) + text("', ожидаемое значение: ") + result(expected.toString())) {
                        if(actual is BigDecimal || expected is BigDecimal) {
                            Log.trace(text("Актуальное значение: ") + result(actual.toString()))
                            val actualStr = (actual as? BigDecimal)?.stripTrailingZeros()?.toPlainString() ?: actual.toString()
                            val expectedStr = (expected as? BigDecimal)?.stripTrailingZeros()?.toPlainString() ?: expected.toString()
                            if(actualStr != expectedStr) {
                                Log.error("Ошибка соответствия полей BigDecimal")
                            }
                            assertThat(actualStr)
                                .isEqualTo(expectedStr)
                        } else {
                            if(expected == null) return@action
                            if(expected.toString().isEmpty() && wrapper.ignoreEmptyString) {
                                Log.trace("Игнорируем пусте знаение")
                                return@action
                            }

                            if(!wrapper.caseSensetive) {
                                if(actual is String && expected is String) {
                                    actual = (actual as String).lowercase().removeSuffix(".00")
                                    expected = (expected as String).lowercase().removeSuffix(".00")
                                }
                            }

                            Log.trace(text("Актуальное значение: ") + result(actual.toString()))
                            if(actual != expected) {
                                Log.error("Ошибка соответствия полей")
                            }
                            assertThat(actual = actual, name = it.name)
                                .isEqualTo(expected)
                        }
                    }
                }
            }
        }
    }
}