package ph.chrsrns.microledger.data.reporting

import ph.chrsrns.microledger.data.Amount
import ph.chrsrns.microledger.data.Cost
import ph.chrsrns.microledger.data.CostType
import ph.chrsrns.microledger.data.Posting
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BalancingAmountTest {
    private fun posting(
        account: String?,
        amount: Amount? = null,
        cost: Cost? = null,
        assertion: Amount? = null,
        comment: String? = null,
    ) = Posting(account, amount, cost, assertion, null, comment)

    @Test
    fun singleCurrencyReturnsNegatedSum() {
        val result =
            balancingAmount(
                listOf(
                    posting("a", Amount("10.00", "EUR", "10.00 EUR")),
                    posting("b", Amount("4.50", "EUR", "4.50 EUR")),
                    posting("c"),
                ),
                ".",
            )

        assertEquals(Amount("-14.50", "EUR", "-14.50 EUR"), result)
    }

    @Test
    fun noCurrencyReturnsEmptyCurrency() {
        val result =
            balancingAmount(
                listOf(
                    posting("a", Amount("10.5", "", "10.5")),
                    posting("b"),
                ),
                ".",
            )

        assertEquals(Amount("-10.5", "", "-10.5"), result)
    }

    @Test
    fun honorsDecimalSeparator() {
        val result =
            balancingAmount(
                listOf(
                    posting("a", Amount("10,5", "EUR", "10,5 EUR")),
                    posting("b"),
                ),
                ",",
            )

        assertEquals(Amount("-10.5", "EUR", "-10.5 EUR"), result)
    }

    @Test
    fun costPresentReturnsNull() {
        val result =
            balancingAmount(
                listOf(
                    posting(
                        "a",
                        Amount("10.00", "EUR", "10.00 EUR"),
                        Cost(Amount("5.00", "EUR", "5.00 EUR"), CostType.UNIT),
                    ),
                    posting("b"),
                ),
                ".",
            )

        assertNull(result)
    }

    @Test
    fun assertionWithoutAmountReturnsNull() {
        val result =
            balancingAmount(
                listOf(
                    posting("a", null, null, Amount("5.00", "EUR", "5.00 EUR")),
                    posting("b"),
                ),
                ".",
            )

        assertNull(result)
    }

    @Test
    fun assertionCurrencyMismatchReturnsNull() {
        val result =
            balancingAmount(
                listOf(
                    posting(
                        "a",
                        Amount("10.00", "EUR", "10.00 EUR"),
                        null,
                        Amount("10.00", "USD", "10.00 USD"),
                    ),
                    posting("b"),
                ),
                ".",
            )

        assertNull(result)
    }

    @Test
    fun matchingAssertionCurrencyIsAccepted() {
        val result =
            balancingAmount(
                listOf(
                    posting(
                        "a",
                        Amount("10.00", "EUR", "10.00 EUR"),
                        null,
                        Amount("10.00", "EUR", "10.00 EUR"),
                    ),
                    posting("b"),
                ),
                ".",
            )

        assertEquals(Amount("-10.00", "EUR", "-10.00 EUR"), result)
    }

    @Test
    fun multipleCurrenciesReturnsNull() {
        val result =
            balancingAmount(
                listOf(
                    posting("a", Amount("10.00", "EUR", "10.00 EUR")),
                    posting("b", Amount("5.00", "USD", "5.00 USD")),
                    posting("c"),
                ),
                ".",
            )

        assertNull(result)
    }

    @Test
    fun nonQualifyingPostingsAreIgnored() {
        val result =
            balancingAmount(
                listOf(
                    posting("a", Amount("10.00", "EUR", "10.00 EUR")),
                    posting("(virtual)", Amount("5.00", "USD", "5.00 USD")),
                    posting(null, null, null, null, "a comment"),
                    posting(" ", Amount("99.00", "JPY", "99.00 JPY")),
                    posting("b"),
                ),
                ".",
            )

        assertEquals(Amount("-10.00", "EUR", "-10.00 EUR"), result)
    }

    @Test
    fun emptyListReturnsZeroCurrencylessAmount() {
        val result = balancingAmount(emptyList(), ".")

        assertEquals(Amount("0", "", "0"), result)
    }
}
