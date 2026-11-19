package com.datorio.query

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ReadOnlySqlTest {

    @Test
    fun `accepts a semicolon inside a string literal or a quoted name`() {
        val sql = "SELECT replaceAll(label, ',', ';') AS \"a;b\", `c;d`\nFROM t\nORDER BY first_send DESC"
        assertEquals(sql, ReadOnlySql.requireReadOnly(sql))
        ReadOnlySql.requireReadOnly("SELECT 'it''s; fine'")
    }

    @Test
    fun `accepts one trailing semicolon`() {
        ReadOnlySql.requireReadOnly("SELECT 1;")
        ReadOnlySql.requireReadOnly("SELECT 1; -- the end\n")
    }

    @Test
    fun `refuses a second statement`() {
        val error = assertThrows<IllegalArgumentException> { ReadOnlySql.requireReadOnly("SELECT 1; DROP TABLE t") }
        assertEquals("Only one statement is allowed in a query", error.message)
        assertThrows<IllegalArgumentException> { ReadOnlySql.requireReadOnly("SELECT 1;;") }
    }

    @Test
    fun `refuses a second statement after a literal that ends with a backslash in PostgreSQL`() {
        // PostgreSQL reads 'C:\' as a whole literal; a backslash escape would hide the DROP inside the literal
        assertThrows<IllegalArgumentException> { ReadOnlySql.requireReadOnly("SELECT 'C:\\'; DROP TABLE t; SELECT ''") }
    }

    @Test
    fun `refuses a statement that does not read`() {
        val error = assertThrows<IllegalArgumentException> { ReadOnlySql.requireReadOnly("DROP TABLE t") }
        assertEquals("Only SELECT queries are allowed. Use the database admin tools for DDL/DML operations.", error.message)
    }
}
