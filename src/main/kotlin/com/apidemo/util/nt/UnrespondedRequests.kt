package com.apidemo.util.nt

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.timestamp

object UnrespondedRequests: IntIdTable(
    name = "unresponded_requests"
) {
    val ts = timestamp("ts")
    val url = varchar("url", 250)
}