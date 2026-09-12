package ph.chrsrns.microledger.data.reporting

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class QuantityParsingTest {
    @Test
    fun blankQuantityReturnsZero() {
        assertEquals(BigDecimal.ZERO, parseQuantity("", "."))
    }

    @Test
    fun positiveDotDecimal() {
        assertEquals(BigDecimal("1000.00"), parseQuantity("1000.00", "."))
    }

    @Test
    fun negativeDotDecimal() {
        assertEquals(BigDecimal("-500.50"), parseQuantity("-500.50", "."))
    }

    @Test
    fun positiveCommaDecimal() {
        assertEquals(BigDecimal("1000.00"), parseQuantity("1000,00", ","))
    }

    @Test
    fun negativeCommaDecimal() {
        assertEquals(BigDecimal("-123.45"), parseQuantity("-123,45", ","))
    }

    @Test
    fun unparseableStringReturnsZero() {
        assertEquals(BigDecimal.ZERO, parseQuantity("not-a-number", "."))
    }

    @Test
    fun multiCharSeparatorUsesFirstChar() {
        assertEquals(BigDecimal("1.23"), parseQuantity("1,23", ",."))
    }

    @Test
    fun regexMetacharacterSeparatorDoesNotCrash() {
        // Before the fix a regex-special separator would throw PatternSyntaxException.
        // We only need to assert no crash and a stable result.
        val result = parseQuantity("1\\23", "\\")
        assertEquals(BigDecimal("1.23"), result)
    }
}
