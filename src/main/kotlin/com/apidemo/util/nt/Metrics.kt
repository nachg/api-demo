package com.apidemo.util.nt

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.timestamp

object MetricsTable: IntIdTable(
    name = "metrics"
) {
    val startTime = timestamp("start_time") //Время старта
    val method = varchar("key", 250) //Название тестируемого метода
    val mode =  varchar("mode", 50) //Режим

    val avg_ts = long("avg") //среднее время выполнения
    val min_ts = long("min") //минимальное
    val max_ts = long("max") //максимальное
    val total_execution_ts = long("total") //все время выполнения тестов в режиме

    val requestsCount = integer("requests_count").nullable() //количество отправленных запросов
    val count = integer("count") //количество повторов в однопоточном режиме, либо количество потоков - в многопоточном
    val errors = integer("errors") //количество запросов завершенных с ошибками
    val unresponded = integer("unresponded").nullable() //количество запросов без ответа

    val version = varchar("version", 10) //версия тестируемого приложения
    val comment = varchar("comment", 250) //произвельный комментарий
}