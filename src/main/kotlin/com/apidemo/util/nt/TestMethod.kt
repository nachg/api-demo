package com.apidemo.util.nt

data class TestMethod<T>(
    val title: String,
    val lambda: (DataProvider<T>)->Any?
)