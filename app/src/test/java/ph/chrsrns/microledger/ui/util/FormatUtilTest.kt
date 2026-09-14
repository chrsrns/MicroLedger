package ph.chrsrns.microledger.ui.util

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatUtilTest {
    @Test
    fun blankQuantityReturnsBlank() {
        assertEquals(Sign.BLANK, postingAmountSign("", "."))
        assertEquals(Sign.BLANK, postingAmountSign("   ", "."))
    }

    @Test
    fun positiveDotDecimal() {
        assertEquals(Sign.POSITIVE, postingAmountSign("1000.00", "."))
    }

    @Test
    fun negativeDotDecimal() {
        assertEquals(Sign.NEGATIVE, postingAmountSign("-500.50", "."))
    }

    @Test
    fun positiveCommaDecimal() {
        assertEquals(Sign.POSITIVE, postingAmountSign("1000,00", ","))
    }

    @Test
    fun negativeCommaDecimal() {
        assertEquals(Sign.NEGATIVE, postingAmountSign("-123,45", ","))
    }

    @Test
    fun zeroReturnsZero() {
        assertEquals(Sign.ZERO, postingAmountSign("0", "."))
        assertEquals(Sign.ZERO, postingAmountSign("0,00", ","))
    }

    @Test
    fun unparseableStringReturnsUnparseable() {
        assertEquals(Sign.UNPARSEABLE, postingAmountSign("not-a-number", "."))
    }

    @Test
    fun emptyCleanedInputReturnsUnparseable() {
        assertEquals(Sign.UNPARSEABLE, postingAmountSign("abc", "."))
    }

    @Test
    fun filtersCurrencyAndSpacingCharacters() {
        assertEquals(Sign.POSITIVE, postingAmountSign("€ 1000.00", "."))
        assertEquals(Sign.NEGATIVE, postingAmountSign("$ -500.50", "."))
    }

    @Test
    fun multiCharSeparatorUsesFirstChar() {
        assertEquals(Sign.POSITIVE, postingAmountSign("1,23", ","))
    }

    @Test
    fun regexMetacharacterSeparatorDoesNotCrash() {
        val result = postingAmountSign("1\\23", "\\")
        assertEquals(Sign.POSITIVE, result)
    }
}
