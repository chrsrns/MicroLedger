package ph.chrsrns.microledger.data

import ph.chrsrns.microledger.data.Posting
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PostingTest {
    @Test
    fun postingWithoutAmountShouldNotFormatWithTrailingWhitespace() {
        val formatted = Posting("some account", null, null, null, null, null).format(80, false, false, false)

        assertEquals("    some account", formatted)
    }

    @Test
    fun isQualifyingTrueForOrdinaryPosting() {
        assertTrue(Posting("expenses:a", null, null, null, null, null).isQualifying())
    }

    @Test
    fun isQualifyingFalseForVirtualCommentAndBlankAccount() {
        assertFalse(Posting("(virtual)", null, null, null, null, null).isQualifying())
        assertFalse(Posting(null, null, null, null, null, "a comment").isQualifying())
        assertFalse(Posting(null, null, null, null, null, null).isQualifying())
        assertFalse(Posting("", null, null, null, null, null).isQualifying())
        assertFalse(Posting(" ", null, null, null, null, null).isQualifying())
    }

    @Test
    fun isStubTrueForBlankAccountNonVirtualNonComment() {
        assertTrue(Posting("", null, null, null, null, null).isStub())
        assertTrue(Posting(" ", null, null, null, null, null).isStub())
        assertTrue(Posting(null, null, null, null, null, null).isStub())
    }

    @Test
    fun isStubFalseForVirtualCommentAndNamedAccount() {
        assertFalse(Posting("(virtual)", null, null, null, null, null).isStub())
        assertFalse(Posting(null, null, null, null, null, "a comment").isStub())
        assertFalse(Posting("expenses:a", null, null, null, null, null).isStub())
    }
}
