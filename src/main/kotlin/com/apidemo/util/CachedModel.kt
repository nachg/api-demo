package com.apidemo.util

import org.xpathqs.cache.base.IExpirableCachedModel
import org.xpathqs.cache.impl.ExpiredCachedModel
import org.xpathqs.cache.impl.PersistentCache
import org.xpathqs.log.Log
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

open class CachedModel : IExpirableCachedModel by ExpiredCachedModel(
    expCache = PersistentCache()
) {
    private val path: String get() = "build/cache/" +
            this.javaClass.name.substringBeforeLast(".").replace(".", "/") + "/"

    private val filename = this.javaClass.simpleName.lowercase() + ".json"
    private val filePath: String get() = path + filename

    init {
        Path.of(path).createDirectories()

        (expCache as PersistentCache<*>).changePath(filePath)
        (expCache as PersistentCache<*>).load()
    }

    fun delete() {
        Log.info("Delete model log of '$filePath': ${
            Files.deleteIfExists(
                Path.of(filePath) 
            )
        }")
        (expCache as PersistentCache).load()
    }
}