package com.apidemo.util.nt

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.timestamp

object LogMessages: IntIdTable(
    name = "log_messages"
) {
    val ts = timestamp("ts")
    val type = varchar("type",50)
    val method = varchar("method",10)
    val url = varchar("url", 250)
    val code = integer("code")
    val message = text("message")
}