package com.apidemo.util

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.fail
import org.xpathqs.log.Log
import org.xpathqs.driver.model.IBaseModel
import org.xpathqs.driver.model.ModelProperty
import org.xpathqs.driver.navigation.annotations.Model
import java.math.BigDecimal
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMembers

data class KVData(
    val name: String,
    val value: String,
    val annotations: Collection<Annotation> = listOf(),
)

fun ModelProperty.toKvData(): KVData =  KVData(this.name, this.value, this.annotations)

class AllModelComparator(
    val kvExp: List<KVData>,
    val kvAct: List<KVData>
) {

    constructor(
        expected: IBaseModel,
        `actual`: IBaseModel
    ) : this(
        (expected.toKV() as List).map { KVData(it.name, it.value, it.annotations) },
        (actual.toKV() as List).map { KVData(it.name, it.value, it.annotations) }
    )

    constructor(
        expected: Any,
        actual: Any
    ) : this(
        expected.toKeyValue().mapToKvList(),
        actual.toKeyValue().mapToKvList()
    )

    fun compare() {
        val kvExp = kvExp.sortedBy { it.name }
        val kvAct = kvAct.sortedBy { it.name }

        if(kvExp.size != kvAct.size) {
            val s1 = kvExp.map { it.name }
            val s2 = kvAct.map { it.name }

            val s4 = s2 subtract s1
            val s5 = s1 subtract s2

            val diff = if(s4.size > s5.size) s4 else s5
            fail("Модели различаются: $diff")
        }

        assertAll {
            for (i in kvExp.indices) {
                compare(kvExp[i], kvAct[i])
            }
        }
    }

    fun compareIntersection() {
        val exp = kvExp.intersect(kvAct).sortedBy { it.name }
        val act = kvAct.intersect(kvExp).sortedBy { it.name }
        assertAll {
            for (i in exp.indices) {
                compare(exp[i], act[i])
            }
        }
    }

    fun compare(pExp: ModelProperty, pAct: ModelProperty) = compare(pExp.toKvData(), pAct.toKvData())
    fun compare(pExp: KVData, pAct: KVData) {

        Log.info("Проверка поля: '${pExp.name}', ожидаемое значение: '${pExp.value}', актуальное значение: '${pAct.value}'")

        if (pExp.annotations.find { it.annotationClass == Model.DataTypes.Date::class } != null) {
            compareDates(pExp.name, pExp.value, pAct.value)
        } else if (pExp.annotations.find { it.annotationClass == Model.DataTypes.Currency::class } != null) {
            compareCurrency(pExp.name, pExp.value, pAct.value)
        } else if (pExp.annotations.find { it.annotationClass == Model.DataTypes.Ignore::class } != null) {
            //skip this
        } else if (pExp.annotations.find { it.annotationClass == Model.ComparatorConfig.IgnoreSpaces::class } != null) {
            val act = pAct.value.replace(" ", "")
            val exp = pExp.value.replace(" ", "")
            if(act != exp) {
                Log.error(pAct.value)
            }
            assertThat(act, exp)
                .isEqualTo(pExp.value)
        } else {
            if(pAct.value != pExp.value) {
                Log.error(pAct.value)
            }
            assertThat(pAct.value, pAct.name)
                .isEqualTo(pExp.value)
        }
    }

    private fun compareDates(fieldName: String, expected: String, actual: String) {
        assertThat(processDate(expected), fieldName)
            .isEqualTo(processDate(actual))
    }

    private fun processDate(date: String): String {
        val splited = date.split(".")
        if(splited.size == 3) {
            val d = if(splited[0].length == 1) "0"+splited[0] else splited[0]
            val m = if(splited[1].length == 1) "0"+splited[1] else splited[1]
            val y = splited[2]
            return "$d.$m.$y"
        }
        return date
    }

    private fun compareCurrency(fieldName: String, expected: String, actual: String) {
        assertThat(
            processCurrency(expected)
                .compareTo(
                    processCurrency(actual)
                ),
            fieldName
        ).isEqualTo(0)
    }

    private fun processCurrency(cur: String): BigDecimal {
        val split = cur.split(",")
        if(split.size == 1) {
            return split[0].replace("\\D".toRegex(), "").toBigDecimal()
        } else if (split.size == 2) {
            return (split[0].replace("\\D".toRegex(), "")
                    + "."
                    + split[1].replace("\\D".toRegex(), "")).toBigDecimal()
        }
        return cur.toBigDecimal()
    }
}

private fun Map<*, *>.mapToKvList(): List<KVData> {
    val result = ArrayList<KVData>()

    fun processSubmap(submap: Map<*, *>, prefix: String = "") {
        submap.apply{} .forEach { (k,v) ->
            if(v is Map<*,*>) {
                processSubmap(
                    v,
                    if(prefix.isEmpty()) k.toString() else "$prefix.$k"
                )
            } else {
                val left = if(prefix.isEmpty()) k.toString() else "$prefix.$k"
                result.add(KVData(left, v?.toString() ?: ""))
            }
        }
    }

    processSubmap(this)
    return result
}

fun Any.copyFrom(other: Any) {
    val otherMap = other.toKeyValue()
    this::class.declaredMembers.forEach { prop ->
        runCatching {
            otherMap[prop.name]?.let { v ->
                (prop as? KMutableProperty<*>)
                    ?.setter?.call(this, v)
            }
        }
    }
}

class T1(
    var v: String
)

fun main() {
    val v1 = T1("asd")
    val v2 = T1("")
    v2.copyFrom(v1)
    println(v2.v)
}