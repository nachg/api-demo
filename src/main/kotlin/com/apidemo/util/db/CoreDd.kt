package com.apidemo.util.db

import com.apidemo.util.Loader
import com.apidemo.util.nt.LogMessages
import com.apidemo.util.nt.MetricsTable
import com.apidemo.util.nt.UnrespondedRequests
import com.vtb.common.util.db.config.Connection
import com.apidemo.util.db.config.Connections
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.jdbc.PgArray
import org.xpathqs.log.Log
import java.util.UUID

open class DBCls(
    val config: Connection
) {
    private val connections = HashMap<Connection, Database>()

    private fun getDb(config: Connection): Database {
        return connections.getOrPut(config) {
            Log.info("New DB connection was established")

            Database.connect(
                url = config.url,
                user = config.login,
                password = config.password,
                driver = "org.postgresql.Driver",
            )
        }
    }

    fun select(sql: String): HashMap<String, Any?> {
        val db = getDb(config)
        return transaction(db) {
           val row = HashMap<String, Any?>()
           exec(sql) { rs ->
               val md = rs.metaData
               val columns = md.columnCount

               while (rs.next()) {
                   for (i in 1..columns) {
                       row[md.getColumnName(i)] = rs.getObject(i)
                   }
               }
           }
           row
        }
    }

    fun insert(table: String, data: HashMap<String, Any?>, deleteBeforeInstert: Boolean = true) {
        val db = getDb(config)
        transaction(db) {
            val columns = data.filter { (k,v) ->
                v is String || v == null || v is PgArray
            }.map {
                it.key
            }.joinToString()

            val values = data.filter { (k,v) ->
                v is String || v == null || v is PgArray
            }.values.map {
                when (it) {
                    is String -> "'$it'"
                    is PgArray -> {
                        if(it.toString().lowercase().contains("null")) {
                            "'{null}'"
                        } else if(it.toString() == "{}") {
                            "'{}'"
                        } else {
                            "'{\"${it.toString().removeSurrounding(prefix = "{", suffix = "}")}\"}'"
                        }
                    }
                    else -> it
                }
            }.joinToString()

            var sql = "INSERT INTO $table ($columns) VALUES ($values)"
            println("Execute sql: $sql")

            exec(sql)
            commit()
        }
    }

    fun update(table: String, column: String, condition: String) {
        exec(
            """
                UPDATE $table SET $column WHERE $condition
            """.trimIndent()
        )
    }

    fun update(table: String, columns: Map<String, String>, condition: String) {

    }

    fun exec(sql: String) {
        val db = getDb(config)
        transaction(db) {
            println("Execute sql: $sql")
            exec(sql)
            commit()
        }
    }

    fun copy(selectQuery: String, vararg updateColumns: Pair<String, String>) = copy(selectQuery, true, updateColumns = updateColumns)
    fun copy(selectQuery: String, randomId: Boolean, vararg updateColumns: Pair<String, String>) {
        val table = selectQuery.substringAfter("from ").substringBefore(" where").trim()
        var r = select(selectQuery)
        if(r.isEmpty()) {
            throw Exception("Select query returned empty result")
        }
        updateColumns.forEach { (k,v) ->
            r[k] = v
        }
        if(randomId) {
            r["id"] = UUID.randomUUID().toString()
        }
        insert(table, r)
    }

    fun <T> transaction(statement: Transaction.() -> T): T =
        transaction(getDb(config), statement)
}

fun main() {
    Loader.loadProperties()
    val db = DBCls(Connections.nt)
    db.transaction {
        addLogger(StdOutSqlLogger)

        SchemaUtils.createMissingTablesAndColumns(
            MetricsTable,
            LogMessages,
            UnrespondedRequests
        )
    }

}