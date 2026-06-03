package be.chvp.nanoledger.data.reporting

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class NetWorthCalculatorTest {
    private val calculator = NetWorthCalculator()

    @Test
    fun emptyTransactionListShouldReturnZeroNetWorth() {
        val result = calculator.calculate(emptyList(), ".")

        assertEquals(BigDecimal.ZERO, result.netWorth)
        assertEquals(BigDecimal.ZERO, result.totalAssets)
        assertEquals(BigDecimal.ZERO, result.totalLiabilities)
    }

    @Test
    fun singleAssetPostingShouldCalculatePositiveNetWorth() {
        val transactions = listOf(openingTransaction())

        val result = calculator.calculate(transactions, ".")

        assertEquals(BigDecimal("1000.00"), result.totalAssets)
        assertEquals(BigDecimal.ZERO, result.totalLiabilities)
        assertEquals(BigDecimal("1000.00"), result.netWorth)
    }

    @Test
    fun assetsAndLiabilitiesShouldCalculateCorrectNetWorth() {
        val transactions = listOf(
            transaction(
                date = "2024-01-15",
                payee = "Setup",
                firstLine = 1,
                lastLine = 4,
                postings = listOf(
                    posting("Assets:Checking", amount("5000.00")),
                    posting("Liabilities:Credit Card", amount("-500.00")),
                    posting("Equity:Opening Balances", amount("-4500.00")),
                ),
            ),
        )

        val result = calculator.calculate(transactions, ".")

        assertEquals(BigDecimal("5000.00"), result.totalAssets)
        assertEquals(BigDecimal("500.00"), result.totalLiabilities)
        assertEquals(BigDecimal("4500.00"), result.netWorth)
    }

    @Test
    fun multipleTransactionsShouldAggregateCorrectly() {
        val transactions = listOf(
            openingTransaction(
                amount = "1000.00",
                date = "2024-01-01",
                payee = "Opening",
                firstLine = 1
            ),
            incomeTransaction(
                amount = "2000.00",
                date = "2024-01-15",
                payee = "Paycheck",
                firstLine = 4
            ),
            liabilityTransaction(
                amount = "150.00",
                date = "2024-01-20",
                payee = "Credit Card Purchase",
                firstLine = 7,
            ),
        )

        val result = calculator.calculate(transactions, ".")

        // Assets: Checking = 1000 + 2000 = 3000
        assertEquals(BigDecimal("3000.00"), result.totalAssets)
        // Liabilities: Credit Card = 150 (stored as -150 in posting)
        assertEquals(BigDecimal("150.00"), result.totalLiabilities)
        // Net worth = 3000 - 150 = 2850
        assertEquals(BigDecimal("2850.00"), result.netWorth)
    }

    @Test
    fun multiCurrencyAssetsShouldSumSeparately() {
        val transactions = listOf(
            transaction(
                date = "2024-01-15",
                payee = "USD Opening",
                firstLine = 1,
                lastLine = 3,
                postings = listOf(
                    posting("Assets:Checking", amount("1000.00", "USD")),
                    posting("Equity:Opening", amount("-1000.00", "USD")),
                ),
            ),
            transaction(
                date = "2024-01-15",
                payee = "Euro Account Opening",
                firstLine = 4,
                lastLine = 6,
                postings = listOf(
                    posting("Assets:Euro Account", amount("500.00", "EUR")),
                    posting("Equity:Opening", amount("-500.00", "EUR")),
                ),
            ),
        )

        val result = calculator.calculate(transactions, ".")

        // The calculator should sum by currency separately or combine them
        // For this test, we verify that both currencies are included in total assets
        assertEquals(BigDecimal("1500.00"), result.totalAssets)
        assertEquals(BigDecimal.ZERO, result.totalLiabilities)
        assertEquals(BigDecimal("1500.00"), result.netWorth)
    }

    @Test
    fun postingsWithoutAmountShouldBeIgnored() {
        val transactions = listOf(
            transaction(
                date = "2024-01-15",
                payee = "Store",
                firstLine = 1,
                lastLine = 3,
                postings = listOf(
                    posting("Expenses:Food", amount("50.00")),
                    be.chvp.nanoledger.data.Posting(
                        account = "Assets:Checking",
                        amount = null,
                        cost = null,
                        assertion = null,
                        assertionCost = null,
                        comment = null,
                    ),
                ),
            ),
        )

        val result = calculator.calculate(transactions, ".")

        // Only the Expenses:Food posting with an amount should be considered
        // Assets:Checking has no amount so it's ignored
        assertEquals(BigDecimal.ZERO, result.totalAssets)
        assertEquals(BigDecimal.ZERO, result.totalLiabilities)
        assertEquals(BigDecimal.ZERO, result.netWorth)
    }

    @Test
    fun negativeAssetBalanceShouldReduceNetWorth() {
        val transactions = listOf(
            transaction(
                date = "2024-01-15",
                payee = "Overdraft",
                firstLine = 1,
                lastLine = 3,
                postings = listOf(
                    posting("Assets:Checking", amount("-200.00")),
                    posting("Equity:Adjustment", amount("200.00")),
                ),
            ),
        )

        val result = calculator.calculate(transactions, ".")

        // Assets can be negative (overdraft)
        assertEquals(BigDecimal("-200.00"), result.totalAssets)
        assertEquals(BigDecimal.ZERO, result.totalLiabilities)
        assertEquals(BigDecimal("-200.00"), result.netWorth)
    }

    @Test
    fun multipleAssetAndLiabilityAccountsShouldAggregateCorrectly() {
        val transactions = listOf(
            transaction(
                date = "2024-01-01",
                payee = "Setup",
                firstLine = 1,
                lastLine = 4,
                postings = listOf(
                    posting("Assets:Checking", amount("3000.00")),
                    posting("Assets:Savings", amount("5000.00")),
                    posting("Equity:Opening", amount("-8000.00")),
                ),
            ),
            transaction(
                date = "2024-01-15",
                payee = "Credit Cards",
                firstLine = 5,
                lastLine = 8,
                postings = listOf(
                    posting("Liabilities:Credit Card", amount("-500.00")),
                    posting("Liabilities:Loan", amount("-2000.00")),
                    posting("Assets:Checking", amount("2500.00")),
                ),
            ),
        )

        val result = calculator.calculate(transactions, ".")

        // Assets: Checking = 3000 + 2500 = 5500, Savings = 5000, Total = 10500
        assertEquals(BigDecimal("10500.00"), result.totalAssets)
        // Liabilities: Credit Card = 500, Loan = 2000, Total = 2500
        assertEquals(BigDecimal("2500.00"), result.totalLiabilities)
        // Net worth = 10500 - 2500 = 8000
        assertEquals(BigDecimal("8000.00"), result.netWorth)
    }

    @Test
    fun lowercaseAssetAccountShouldContributeToNetWorth() {
        val transactions = listOf(
            transaction(
                date = "2024-01-15",
                payee = "Opening Balance",
                firstLine = 1,
                lastLine = 3,
                postings = listOf(
                    posting("assets:checking", amount("2000.00")),
                    posting("equity:opening", amount("-2000.00")),
                ),
            ),
        )

        val result = calculator.calculate(transactions, ".")

        assertEquals(BigDecimal("2000.00"), result.totalAssets)
        assertEquals(BigDecimal.ZERO, result.totalLiabilities)
        assertEquals(BigDecimal("2000.00"), result.netWorth)
    }

    @Test
    fun lowercaseLiabilityAccountShouldContributeToNetWorth() {
        val transactions = listOf(
            transaction(
                date = "2024-01-15",
                payee = "Setup",
                firstLine = 1,
                lastLine = 4,
                postings = listOf(
                    posting("assets:checking", amount("3000.00")),
                    posting("liabilities:credit card", amount("-800.00")),
                    posting("equity:opening balances", amount("-2200.00")),
                ),
            ),
        )

        val result = calculator.calculate(transactions, ".")

        assertEquals(BigDecimal("3000.00"), result.totalAssets)
        assertEquals(BigDecimal("800.00"), result.totalLiabilities)
        assertEquals(BigDecimal("2200.00"), result.netWorth)
    }
}
