package ph.chrsrns.microledger.data

import kotlin.test.Test
import kotlin.test.assertEquals

class LedgerFileTransformsTest {
    private val journal =
        listOf(
            "2024-01-01 Payee",
            "  expenses:a  1.00 EUR",
            "  assets:b",
            "",
            "2024-02-02 Other",
            "  expenses:c  2.00 EUR",
            "  assets:b",
        )

    @Test
    fun deleteTransactionSkipsRangeAndTrailingBlank() {
        assertEquals(
            "2024-02-02 Other\n  expenses:c  2.00 EUR\n  assets:b\n",
            emitDeleteTransaction(journal, 0, 2),
        )
    }

    @Test
    fun deleteTransactionKeepsNonBlankAfter() {
        val contents = listOf("a", "b", "c")
        assertEquals("a\n", emitDeleteTransaction(contents, 1, 2))
    }

    @Test
    fun replaceTransactionWritesTextRawAtFirstLine() {
        assertEquals(
            "REPLACED\n2024-02-02 Other\n  expenses:c  2.00 EUR\n  assets:b\n",
            emitReplaceTransaction(journal, 0, 2, "REPLACED\n"),
        )
    }

    @Test
    fun appendJoinsAfterBlankSeparator() {
        assertEquals(
            "a\n\nNEW TEXT",
            emitAppend(listOf("a"), "NEW TEXT"),
        )
        assertEquals(
            "a\n\nNEW TEXT",
            emitAppend(listOf("a", ""), "NEW TEXT"),
        )
        assertEquals("NEW TEXT", emitAppend(emptyList(), "NEW TEXT"))
    }

    @Test
    fun addTemplateEmptyFileStillEmitsText() {
        val out = emitAddTemplate(emptyList(), "; template line")
        assertEquals("; template line\n\n", out)
    }

    @Test
    fun addTemplateInsertsAtTopOfFile() {
        val contents =
            listOf(
                "; header comment",
                "2024-01-01 Payee",
                "  expenses:a  1.00 EUR",
            )
        val out = emitAddTemplate(contents, "; tmpl")
        assertEquals(
            "; tmpl\n\n; header comment\n2024-01-01 Payee\n  expenses:a  1.00 EUR\n",
            out,
        )
    }

    @Test
    fun addTemplateInsertsAtTopWhenOnlyComments() {
        val contents = listOf("; c1", "; c2")
        val out = emitAddTemplate(contents, "; tmpl")
        assertEquals("; tmpl\n\n; c1\n; c2\n", out)
    }

    @Test
    fun addTemplateInsertsAboveLeadingBlank() {
        val contents = listOf("", "; c1", "2024-01-01 Payee")
        val out = emitAddTemplate(contents, "; tmpl")
        assertEquals("; tmpl\n\n\n; c1\n2024-01-01 Payee\n", out)
    }

    @Test
    fun addTemplateInsertsAboveSeparatedCommentBlock() {
        // A comment block separated from transactions by blank lines no longer
        // captures the template insertion point mid-file where the parser's
        // initial-block scan cannot see it.
        val contents = listOf("; header", "", "2024-01-01 Payee")
        val out = emitAddTemplate(contents, "; tmpl")
        assertEquals("; tmpl\n\n; header\n\n2024-01-01 Payee\n", out)
    }

    @Test
    fun updateTemplateReplacesRange() {
        val contents = listOf("; old1", "; old2", "x")
        assertEquals(
            "; new\nx\n",
            emitUpdateTemplate(contents, 0, 1, "; new"),
        )
    }

    @Test
    fun deleteTemplateSkipsRangeKeepsTrailingBlank() {
        val contents = listOf("; t1", "; t2", "", "x")
        assertEquals("\nx\n", emitDeleteTemplate(contents, 0, 1))
    }
}
