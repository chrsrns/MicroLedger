package ph.chrsrns.microledger.data

import ph.chrsrns.microledger.data.Posting
import kotlin.test.Test
import kotlin.test.assertEquals

class PostingTest {
    @Test
    fun postingWithoutAmountShouldNotFormatWithTrailingWhitespace() {
        val formatted = Posting("some account", null, null, null, null, null).format(80, false, false, false)

        assertEquals("    some account", formatted)
    }
}
