package com.apidemo.util.nt

class DataProvider<T>(
    val data: List<T>
) {
    private var i = 0

    @Synchronized
    fun next(): T {
        return if(i < data.size) {
            data[i++]
        } else {
            i = 0
            data[i]
        }
    }

    @Synchronized
    fun nextRandom(): T {
        return data[data.indices.random()]
    }
}