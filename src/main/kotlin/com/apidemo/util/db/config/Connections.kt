package com.apidemo.util.db.config

import com.vtb.common.util.db.config.Connection
import org.xpathqs.prop.Model

@Model
object Connections {
    val nt = Connection() //БД для нагрузочного тестирования
}