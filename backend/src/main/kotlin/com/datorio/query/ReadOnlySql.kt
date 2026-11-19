package com.datorio.query

/**
 * Guard for SQL that comes from the UI and runs on a user's data source: only a statement
 * that reads data is accepted. The data source account should be read-only as well; this
 * check keeps a typo or a pasted script from running DDL or DML through the application.
 */
object ReadOnlySql {

    private val ALLOWED_STARTS = listOf("SELECT", "WITH", "SHOW", "DESCRIBE", "DESC", "EXPLAIN")

    /** Returns the SQL unchanged or throws IllegalArgumentException with the reason. */
    fun requireReadOnly(sql: String): String {
        val body = stripComments(sql).trim()
        require(body.isNotEmpty()) { "The query is empty" }
        val first = body.uppercase()
        require(ALLOWED_STARTS.any { first.startsWith(it) }) {
            "Only SELECT queries are allowed. Use the database admin tools for DDL/DML operations."
        }
        // A backslash escapes a quote in ClickHouse and not in PostgreSQL, so both readings must find one statement
        require(!hasSecondStatement(body, backslashEscapes = true) && !hasSecondStatement(body, backslashEscapes = false)) {
            "Only one statement is allowed in a query"
        }
        return sql
    }

    /** True when text follows a ";" outside string literals and quoted names: replaceAll(x, ',', ';') is one statement. */
    private fun hasSecondStatement(sql: String, backslashEscapes: Boolean): Boolean {
        var quote: Char? = null
        var i = 0
        while (i < sql.length) {
            val c = sql[i]
            when {
                quote != null -> {
                    if (backslashEscapes && c == '\\') i++
                    else if (c == quote) quote = null
                }
                c == '\'' || c == '"' || c == '`' -> quote = c
                c == ';' && sql.substring(i + 1).isNotBlank() -> return true
            }
            i++
        }
        return false
    }

    /** Removes -- and /* */ comments outside string literals. */
    fun stripComments(sql: String): String {
        val out = StringBuilder(sql.length)
        var i = 0
        var quote: Char? = null
        while (i < sql.length) {
            val c = sql[i]
            when {
                quote != null -> {
                    out.append(c)
                    if (c == '\\' && i + 1 < sql.length) { out.append(sql[i + 1]); i++ }
                    else if (c == quote) quote = null
                }
                c == '\'' || c == '"' || c == '`' -> { quote = c; out.append(c) }
                c == '-' && sql.startsWith("--", i) -> {
                    while (i < sql.length && sql[i] != '\n') i++
                    continue
                }
                c == '/' && sql.startsWith("/*", i) -> {
                    val end = sql.indexOf("*/", i + 2)
                    i = if (end < 0) sql.length else end + 2
                    out.append(' ')
                    continue
                }
                else -> out.append(c)
            }
            i++
        }
        return out.toString()
    }
}
