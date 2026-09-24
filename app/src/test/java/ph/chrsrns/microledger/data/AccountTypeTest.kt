package ph.chrsrns.microledger.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccountTypeTest {
    private val prefixes =
        AccountTypePrefixes(
            assets = listOf("Assets"),
            liabilities = listOf("Liabilities"),
            equity = listOf("Equity"),
            income = listOf("Income"),
            expenses = listOf("Expenses"),
        )

    @Test
    fun classifyAssetsAccount() {
        assertEquals(AccountType.ASSETS, prefixes.classify("Assets:Checking"))
    }

    @Test
    fun classifyLiabilitiesAccount() {
        assertEquals(AccountType.LIABILITIES, prefixes.classify("Liabilities:Credit Card"))
    }

    @Test
    fun classifyEquityAccount() {
        assertEquals(AccountType.EQUITY, prefixes.classify("Equity:Opening Balances"))
    }

    @Test
    fun classifyIncomeAccount() {
        assertEquals(AccountType.INCOME, prefixes.classify("Income:Salary"))
    }

    @Test
    fun classifyExpensesAccount() {
        assertEquals(AccountType.EXPENSES, prefixes.classify("Expenses:Food"))
    }

    @Test
    fun classifyIsCaseInsensitive() {
        assertEquals(AccountType.ASSETS, prefixes.classify("assets:checking"))
    }

    @Test
    fun classifyTrimsAccount() {
        assertEquals(AccountType.ASSETS, prefixes.classify("  Assets:Checking  "))
    }

    @Test
    fun classifyBlankReturnsNull() {
        assertNull(prefixes.classify(""))
        assertNull(prefixes.classify("   "))
    }

    @Test
    fun classifyNullReturnsNull() {
        assertNull(prefixes.classify(null))
    }

    @Test
    fun classifyUnmatchedReturnsNull() {
        assertNull(prefixes.classify("Custom:Account"))
    }

    @Test
    fun classifyFirstMatchingTypeWins() {
        val overlapping =
            AccountTypePrefixes(
                assets = listOf("A"),
                liabilities = listOf("A", "Liabilities"),
                equity = emptyList(),
                income = emptyList(),
                expenses = emptyList(),
            )
        assertEquals(AccountType.ASSETS, overlapping.classify("A:Sub"))
    }

    @Test
    fun classifyEmptyPrefixesReturnsNull() {
        assertNull(AccountTypePrefixes.EMPTY.classify("Assets:Checking"))
    }

    @Test
    fun classifyMultiplePrefixesForSameType() {
        val multi =
            AccountTypePrefixes(
                assets = listOf("Assets", "Aktiva"),
                liabilities = emptyList(),
                equity = emptyList(),
                income = emptyList(),
                expenses = emptyList(),
            )
        assertEquals(AccountType.ASSETS, multi.classify("Aktiva:Savings"))
    }
}
