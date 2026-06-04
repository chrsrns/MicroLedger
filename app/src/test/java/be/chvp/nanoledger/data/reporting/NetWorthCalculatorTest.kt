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
        val transactions =
            listOf(
                transaction(
                    date = "2024-01-15",
                    payee = "Setup",
                    firstLine = 1,
                    lastLine = 4,
                    postings =
                        listOf(
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
        val transactions =
            listOf(
                openingTransaction(
                    amount = "1000.00",
                    date = "2024-01-01",
                    payee = "Opening",
                    firstLine = 1,
                ),
                incomeTransaction(
                    amount = "2000.00",
                    date = "2024-01-15",
                    payee = "Paycheck",
                    firstLine = 4,
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
        val transactions =
            listOf(
                transaction(
                    date = "2024-01-15",
                    payee = "USD Opening",
                    firstLine = 1,
                    lastLine = 3,
                    postings =
                        listOf(
                            posting("Assets:Checking", amount("1000.00", "USD")),
                            posting("Equity:Opening", amount("-1000.00", "USD")),
                        ),
                ),
                transaction(
                    date = "2024-01-15",
                    payee = "Euro Account Opening",
                    firstLine = 4,
                    lastLine = 6,
                    postings =
                        listOf(
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
        val transactions =
            listOf(
                transaction(
                    date = "2024-01-15",
                    payee = "Store",
                    firstLine = 1,
                    lastLine = 3,
                    postings =
                        listOf(
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
        val transactions =
            listOf(
                transaction(
                    date = "2024-01-15",
                    payee = "Overdraft",
                    firstLine = 1,
                    lastLine = 3,
                    postings =
                        listOf(
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
        val transactions =
            listOf(
                transaction(
                    date = "2024-01-01",
                    payee = "Setup",
                    firstLine = 1,
                    lastLine = 4,
                    postings =
                        listOf(
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
                    postings =
                        listOf(
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
        val transactions =
            listOf(
                transaction(
                    date = "2024-01-15",
                    payee = "Opening Balance",
                    firstLine = 1,
                    lastLine = 3,
                    postings =
                        listOf(
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
        val transactions =
            listOf(
                transaction(
                    date = "2024-01-15",
                    payee = "Setup",
                    firstLine = 1,
                    lastLine = 4,
                    postings =
                        listOf(
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

    // -------------------------------------------------------------------------
    // N1: malformed quantity string should be treated as zero
    // -------------------------------------------------------------------------

    @Test
    fun malformedQuantityStringShouldTreatAsZero() {
        val transactions =
            listOf(
                transaction(
                    date = "2024-01-15",
                    payee = "Bad Amount",
                    firstLine = 1,
                    lastLine = 3,
                    postings =
                        listOf(
                            posting("Assets:Checking", amount("not-a-number")),
                            posting("Equity:Opening Balances", amount("-1000.00")),
                        ),
                ),
                // A valid asset posting alongside the malformed one
                openingTransaction(amount = "500.00", date = "2024-01-16", firstLine = 4),
            )

        val result = calculator.calculate(transactions, ".")

        // The malformed quantity contributes 0; only the valid 500.00 should count
        assertEquals(BigDecimal("500.00"), result.totalAssets)
        assertEquals(BigDecimal.ZERO, result.totalLiabilities)
        assertEquals(BigDecimal("500.00"), result.netWorth)
    }

    // -------------------------------------------------------------------------
    // N5: equity, income, and expense postings do not affect net worth
    // -------------------------------------------------------------------------

    @Test
    fun equityIncomeAndExpensePostingsShouldNotAffectNetWorth() {
        val transactions =
            listOf(
                // Opening balance posts to equity — should not show in assets/liabilities
                openingTransaction(amount = "2000.00", date = "2024-01-01", firstLine = 1),
                // Income transaction: asset receives money, income decreases
                incomeTransaction(amount = "3000.00", date = "2024-01-05", firstLine = 4),
                // Expense transaction: expense account increases, asset decreases
                expenseTransaction(amount = "400.00", date = "2024-01-10", firstLine = 7),
            )

        val result = calculator.calculate(transactions, ".")

        // totalAssets = 2000 (opening) + 3000 (income deposit) - 400 (expense withdrawal) = 4600
        assertEquals(BigDecimal("4600.00"), result.totalAssets)
        // Liabilities are untouched
        assertEquals(BigDecimal.ZERO, result.totalLiabilities)
        // Equity, income, and expense postings do not themselves contribute to assets or liabilities
        assertEquals(BigDecimal("4600.00"), result.netWorth)
    }

    // -------------------------------------------------------------------------
    // N6: netWorth == totalAssets - totalLiabilities invariant
    // -------------------------------------------------------------------------

    @Test
    fun netWorthAlwaysEqualsAssetMinusLiabilities() {
        val transactions =
            listOf(
                openingTransaction(amount = "5000.00", date = "2024-01-01", firstLine = 1),
                liabilityTransaction(amount = "1200.00", date = "2024-01-05", firstLine = 4),
                incomeTransaction(amount = "3500.00", date = "2024-01-10", firstLine = 7),
                expenseTransaction(amount = "250.00", date = "2024-01-15", firstLine = 10),
            )

        val result = calculator.calculate(transactions, ".")

        // The invariant must hold regardless of the specific values
        assertEquals(
            result.totalAssets - result.totalLiabilities,
            result.netWorth,
        )
    }

    // -------------------------------------------------------------------------
    // N2: liabilities held in multiple currencies should sum into totalLiabilities
    // (symmetric with the existing multi-currency assets test)
    // -------------------------------------------------------------------------

    @Test
    fun multiCurrencyLiabilitiesShouldSum() {
        val transactions =
            listOf(
                transaction(
                    date = "2024-01-15",
                    payee = "USD Credit Card",
                    firstLine = 1,
                    lastLine = 3,
                    postings =
                        listOf(
                            posting("Liabilities:Credit Card", amount("-800.00", "USD")),
                            posting("Assets:Checking", amount("800.00", "USD")),
                        ),
                ),
                transaction(
                    date = "2024-01-16",
                    payee = "EUR Loan",
                    firstLine = 4,
                    lastLine = 6,
                    postings =
                        listOf(
                            posting("Liabilities:Loan", amount("-300.00", "EUR")),
                            posting("Assets:Checking", amount("300.00", "EUR")),
                        ),
                ),
            )

        val result = calculator.calculate(transactions, ".")

        // Liabilities across currencies are summed numerically
        assertEquals(BigDecimal("1100.00"), result.totalLiabilities)
    }

    // -------------------------------------------------------------------------
    // N4: mixed-case account type prefixes should still contribute to net worth
    // -------------------------------------------------------------------------

    @Test
    fun mixedCaseAccountTypesShouldContributeToNetWorth() {
        val transactions =
            listOf(
                transaction(
                    date = "2024-01-15",
                    payee = "Mixed Case Setup",
                    firstLine = 1,
                    lastLine = 4,
                    postings =
                        listOf(
                            posting("ASSETS:Checking", amount("3000.00")),
                            posting("LIABILITIES:Loan", amount("-800.00")),
                            posting("EqUiTy:Opening", amount("-2200.00")),
                        ),
                ),
            )

        val result = calculator.calculate(transactions, ".")

        assertEquals(BigDecimal("3000.00"), result.totalAssets)
        assertEquals(BigDecimal("800.00"), result.totalLiabilities)
        assertEquals(BigDecimal("2200.00"), result.netWorth)
    }

    // -------------------------------------------------------------------------
    // N3: a liability account holding a positive stored balance (unusual, e.g.
    // an overpaid credit card) should still be reflected consistently
    // -------------------------------------------------------------------------

    @Test
    fun positiveBalanceLiabilityShouldBeReflectedConsistently() {
        val transactions =
            listOf(
                transaction(
                    date = "2024-01-15",
                    payee = "Overpaid Credit Card",
                    firstLine = 1,
                    lastLine = 3,
                    postings =
                        listOf(
                            // Positive stored amount on a liability account
                            posting("Liabilities:Credit Card", amount("500.00")),
                            posting("Assets:Checking", amount("-500.00")),
                        ),
                ),
            )

        val result = calculator.calculate(transactions, ".")

        // Assets reduced by the 500 withdrawal
        assertEquals(BigDecimal("-500.00"), result.totalAssets)
        // A positive liability posting negates to a negative liability total
        assertEquals(BigDecimal("-500.00"), result.totalLiabilities)
        // Invariant still holds: netWorth == assets - liabilities
        assertEquals(result.totalAssets - result.totalLiabilities, result.netWorth)
    }
}
