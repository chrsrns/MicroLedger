package ph.chrsrns.microledger.ui.common

import ph.chrsrns.microledger.data.Amount
import ph.chrsrns.microledger.data.Cost
import ph.chrsrns.microledger.data.CostType
import ph.chrsrns.microledger.data.Posting
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class PostingFieldEditsTest {
    private val sentinel = Posting("", null, null, null, null, null)
    private fun real(n: Int) =
        Posting("Expenses:Food:$n", Amount("1.00", "€", "1.00 €"), null, null, null, null)

    @Test
    fun withQuantityPreservesOriginalAndCurrency() {
        val p = Posting("A", Amount("5", "€", "5 €"), null, null, null, null)
        assertEquals(Amount("7", "€", "5 €"), p.withQuantity("7").amount)
    }

    @Test
    fun withQuantityCollapsesWhenAllEmpty() {
        val p = Posting("A", Amount("", "", ""), null, null, null, null)
        assertNull(p.withQuantity("").amount)
    }

    @Test
    fun withCurrencyPreservesOriginalAndQuantity() {
        val p = Posting("A", Amount("5", "$", "5 $"), null, null, null, null)
        assertEquals(Amount("5", "€", "5 $"), p.withCurrency("€").amount)
    }

    @Test
    fun costHelpersDropOriginalAndNeverCollapse() {
        val p = Posting("A", null, Cost(Amount("2", "$", "orig"), CostType.TOTAL), null, null, null)
        assertEquals(Cost(Amount("2", "$", ""), CostType.UNIT), p.withCostType(CostType.UNIT).cost)
        assertEquals(Cost(Amount("9", "$", ""), CostType.TOTAL), p.withCostQuantity("9").cost)
        assertEquals(Cost(Amount("2", "€", ""), CostType.TOTAL), p.withCostCurrency("€").cost)
    }

    @Test
    fun assertionHelpersDropOriginal() {
        val p = Posting("A", null, null, Amount("1", "$", "orig"), null, null)
        assertEquals(Amount("3", "$", ""), p.withAssertionQuantity("3").assertion)
        assertEquals(Amount("1", "€", ""), p.withAssertionCurrency("€").assertion)
    }

    @Test
    fun accountOffCascades() {
        val p =
            Posting(
                "A",
                Amount("1", "€", ""),
                Cost(Amount("2", "€", ""), CostType.UNIT),
                Amount("3", "€", ""),
                Cost(Amount("4", "€", ""), CostType.UNIT),
                "c",
            )
        val r = p.withAccountEnabled(false)
        assertNull(r.account)
        assertNull(r.amount)
        assertNull(r.cost)
        assertNull(r.assertion)
        assertNull(r.assertionCost)
        assertEquals("c", r.comment)
    }

    @Test
    fun togglesUseSuppliedOnValue() {
        val p = Posting("A", null, null, null, null, null)
        val on = Amount("", "€", "")
        assertEquals(on, p.withAmountEnabled(true, on).amount)
        assertNull(p.withAmountEnabled(false, on).amount)
        assertEquals(Cost(on, CostType.UNIT), p.withCostEnabled(true, Cost(on, CostType.UNIT)).cost)
        assertEquals("", p.withCommentEnabled(true).comment)
        assertNull(p.withCommentEnabled(false).comment)
    }

    @Test
    fun updatePostingAtReplacesRealIndex() {
        val list = listOf(real(1), real(2), sentinel)
        val r = updatePostingAt(list, 0, { sentinel }) { it.withAccount("X") }
        assertEquals("X", r[0].account)
        assertEquals(sentinel, r.last())
        assertEquals(3, r.size)
    }

    @Test
    fun updatePostingAtSentinelSlotAppendsReal() {
        val list = listOf(real(1), sentinel)
        val edited = real(9)
        val r = updatePostingAt(list, 1, { sentinel }) { edited }
        assertEquals(listOf(real(1), edited, sentinel), r)
    }

    @Test
    fun updatePostingAtOutOfRangeNoOp() {
        val list = listOf(real(1), sentinel)
        assertSame(list, updatePostingAt(list, 5, { sentinel }) { it })
        assertSame(list, updatePostingAt(list, -1, { sentinel }) { it })
    }

    @Test
    fun removePostingAtDropsRealOnly() {
        val list = listOf(real(1), real(2), sentinel)
        assertEquals(listOf(real(1), sentinel), removePostingAt(list, 1, { sentinel }))
        assertSame(list, removePostingAt(list, 2, { sentinel }))
        assertSame(list, removePostingAt(list, 9, { sentinel }))
    }

    @Test
    fun insertPostingBeforeSentinel() {
        val list = listOf(real(1), sentinel)
        val blank = Posting("", null, null, null, null, null)
        assertEquals(listOf(real(1), blank, sentinel), insertPosting(list, blank) { sentinel })
    }

    @Test
    fun withSentinelAlwaysAppends() {
        assertEquals(listOf(sentinel), withSentinel(emptyList()) { sentinel })
        assertEquals(listOf(real(1), sentinel), withSentinel(listOf(real(1))) { sentinel })
    }
}
