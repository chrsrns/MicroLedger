package be.chvp.nanoledger.data.reporting

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthlyCashFlowCalculatorTest {
    private val calculator = MonthlyCashFlowCalculator()

    @Test
    fun emptyTransactionListShouldReturnZeroCashFlow() {
        val result = calculator.calculateForMonth(emptyList(), 2024, 1, ".")

        assertEquals(BigDecimal.ZERO, result.totalIncome)
        assertEquals(BigDecimal.ZERO, result.totalExpenses)
        assertEquals(BigDecimal.ZERO, result.netFlow)
        assertEquals("2024-01", result.period)
    }

    @Test
    fun singleIncomeTransactionShouldShowPositiveFlow() {
        val transactions =
            listOf(
                incomeTransaction(amount = "5000.00", date = "2024-01-15"),
            )

        val result = calculator.calculateForMonth(transactions, 2024, 1, ".")

        assertEquals("2024-01", result.period)
        // Income postings are credits (negative), we display as positive
        assertEquals(BigDecimal("5000.00"), result.totalIncome)
        assertEquals(BigDecimal.ZERO, result.totalExpenses)
        assertEquals(BigDecimal("5000.00"), result.netFlow)
    }

    @Test
    fun singleExpenseTransactionShouldShowNegativeFlow() {
        val transactions =
            listOf(
                expenseTransaction(
                    amount = "150.00",
                    date = "2024-01-10",
                    payee = "Grocery Store",
                ),
            )

        val result = calculator.calculateForMonth(transactions, 2024, 1, ".")

        assertEquals("2024-01", result.period)
        assertEquals(BigDecimal.ZERO, result.totalIncome)
        // Expense postings are debits (positive)
        assertEquals(BigDecimal("150.00"), result.totalExpenses)
        assertEquals(BigDecimal("-150.00"), result.netFlow)
    }

    @Test
    fun incomeAndExpenseTransactionShouldCalculateNetFlow() {
        val transactions =
            listOf(
                incomeTransaction(amount = "5000.00", date = "2024-02-01"),
                transaction(
                    date = "2024-02-15",
                    payee = "Landlord",
                    firstLine = 4,
                    lastLine = 6,
                    postings =
                        listOf(
                            posting("Expenses:Housing:Rent", amount("1200.00")),
                            posting("Assets:Checking", amount("-1200.00")),
                        ),
                ),
            )

        val result = calculator.calculateForMonth(transactions, 2024, 2, ".")

        assertEquals("2024-02", result.period)
        assertEquals(BigDecimal("5000.00"), result.totalIncome)
        assertEquals(BigDecimal("1200.00"), result.totalExpenses)
        assertEquals(BigDecimal("3800.00"), result.netFlow)
    }

    @Test
    fun transactionsOutsideMonthShouldBeExcluded() {
        val transactions =
            listOf(
                expenseTransaction(
                    amount = "50.00",
                    date = "2024-01-31",
                    payee = "January Expense",
                    firstLine = 1,
                ),
                expenseTransaction(
                    amount = "75.00",
                    date = "2024-03-01",
                    payee = "March Expense",
                    firstLine = 4,
                ),
            )

        val result = calculator.calculateForMonth(transactions, 2024, 2, ".")

        assertEquals("2024-02", result.period)
        // Both transactions are outside February
        assertEquals(BigDecimal.ZERO, result.totalIncome)
        assertEquals(BigDecimal.ZERO, result.totalExpenses)
        assertEquals(BigDecimal.ZERO, result.netFlow)
    }

    @Test
    fun transactionsInsideMonthShouldBeIncluded() {
        val transactions =
            listOf(
                expenseTransaction(
                    amount = "50.00",
                    date = "2024-02-05",
                    payee = "February Expense 1",
                    firstLine = 1,
                ),
                incomeTransaction(
                    amount = "3000.00",
                    date = "2024-02-15",
                    payee = "February Income",
                    firstLine = 4,
                ),
                expenseTransaction(
                    amount = "75.00",
                    date = "2024-02-28",
                    payee = "February Expense 2",
                    firstLine = 7,
                ),
            )

        val result = calculator.calculateForMonth(transactions, 2024, 2, ".")

        assertEquals("2024-02", result.period)
        assertEquals(BigDecimal("3000.00"), result.totalIncome)
        assertEquals(BigDecimal("125.00"), result.totalExpenses)
        assertEquals(BigDecimal("2875.00"), result.netFlow)
    }

    @Test
    fun transactionsOnMonthBoundaryShouldBeIncluded() {
        val transactions =
            listOf(
                // First day of month
                incomeTransaction(
                    amount = "1000.00",
                    date = "2024-03-01",
                    payee = "First Day",
                    firstLine = 1,
                ),
                // Last day of month
                expenseTransaction(
                    amount = "200.00",
                    date = "2024-03-31",
                    payee = "Last Day",
                    firstLine = 4,
                ),
            )

        val result = calculator.calculateForMonth(transactions, 2024, 3, ".")

        assertEquals("2024-03", result.period)
        assertEquals(BigDecimal("1000.00"), result.totalIncome)
        assertEquals(BigDecimal("200.00"), result.totalExpenses)
        assertEquals(BigDecimal("800.00"), result.netFlow)
    }

    @Test
    fun multipleIncomeTransactionsShouldSumCorrectly() {
        val transactions =
            listOf(
                transaction(
                    date = "2024-01-05",
                    payee = "Primary Salary",
                    firstLine = 1,
                    lastLine = 3,
                    postings =
                        listOf(
                            posting("Assets:Checking", amount("3000.00")),
                            posting("Income:Salary", amount("-3000.00")),
                        ),
                ),
                transaction(
                    date = "2024-01-10",
                    payee = "Side Gig",
                    firstLine = 4,
                    lastLine = 6,
                    postings =
                        listOf(
                            posting("Assets:Checking", amount("500.00")),
                            posting("Income:Freelance", amount("-500.00")),
                        ),
                ),
                transaction(
                    date = "2024-01-20",
                    payee = "Bonus",
                    firstLine = 7,
                    lastLine = 9,
                    postings =
                        listOf(
                            posting("Assets:Checking", amount("200.00")),
                            posting("Income:Bonus", amount("-200.00")),
                        ),
                ),
            )

        val result = calculator.calculateForMonth(transactions, 2024, 1, ".")

        assertEquals("2024-01", result.period)
        // Total income: 3000 + 500 + 200 = 3700
        assertEquals(BigDecimal("3700.00"), result.totalIncome)
        assertEquals(BigDecimal.ZERO, result.totalExpenses)
        assertEquals(BigDecimal("3700.00"), result.netFlow)
    }

    @Test
    fun multipleExpenseTransactionsShouldSumCorrectly() {
        val transactions =
            listOf(
                transaction(
                    date = "2024-01-02",
                    payee = "Grocery Store",
                    firstLine = 1,
                    lastLine = 3,
                    postings =
                        listOf(
                            posting("Expenses:Food:Groceries", amount("85.50")),
                            posting("Assets:Checking", amount("-85.50")),
                        ),
                ),
                transaction(
                    date = "2024-01-05",
                    payee = "Electric Bill",
                    firstLine = 4,
                    lastLine = 6,
                    postings =
                        listOf(
                            posting("Expenses:Utilities:Electric", amount("120.00")),
                            posting("Assets:Checking", amount("-120.00")),
                        ),
                ),
                transaction(
                    date = "2024-01-15",
                    payee = "Restaurant",
                    firstLine = 7,
                    lastLine = 9,
                    postings =
                        listOf(
                            posting("Expenses:Food:Dining", amount("45.00")),
                            posting("Assets:Checking", amount("-45.00")),
                        ),
                ),
            )

        val result = calculator.calculateForMonth(transactions, 2024, 1, ".")

        assertEquals("2024-01", result.period)
        assertEquals(BigDecimal.ZERO, result.totalIncome)
        // Total expenses: 85.50 + 120 + 45 = 250.50
        assertEquals(BigDecimal("250.50"), result.totalExpenses)
        assertEquals(BigDecimal("-250.50"), result.netFlow)
    }

    @Test
    fun mixedIncomeAndExpensesShouldCalculateNet() {
        val transactions =
            listOf(
                transaction(
                    date = "2024-01-05",
                    payee = "Salary",
                    firstLine = 1,
                    lastLine = 3,
                    postings =
                        listOf(
                            posting("Assets:Checking", amount("4000.00")),
                            posting("Income:Salary", amount("-4000.00")),
                        ),
                ),
                transaction(
                    date = "2024-01-10",
                    payee = "Rent",
                    firstLine = 4,
                    lastLine = 6,
                    postings =
                        listOf(
                            posting("Expenses:Housing:Rent", amount("1200.00")),
                            posting("Assets:Checking", amount("-1200.00")),
                        ),
                ),
                transaction(
                    date = "2024-01-15",
                    payee = "Utilities",
                    firstLine = 7,
                    lastLine = 9,
                    postings =
                        listOf(
                            posting("Expenses:Utilities", amount("150.00")),
                            posting("Assets:Checking", amount("-150.00")),
                        ),
                ),
                transaction(
                    date = "2024-01-20",
                    payee = "Freelance Work",
                    firstLine = 10,
                    lastLine = 12,
                    postings =
                        listOf(
                            posting("Assets:Checking", amount("800.00")),
                            posting("Income:Freelance", amount("-800.00")),
                        ),
                ),
            )

        val result = calculator.calculateForMonth(transactions, 2024, 1, ".")

        assertEquals("2024-01", result.period)
        // Income: 4000 + 800 = 4800
        assertEquals(BigDecimal("4800.00"), result.totalIncome)
        // Expenses: 1200 + 150 = 1350
        assertEquals(BigDecimal("1350.00"), result.totalExpenses)
        // Net: 4800 - 1350 = 3450
        assertEquals(BigDecimal("3450.00"), result.netFlow)
    }

    @Test
    fun yearBoundaryTransactionsShouldBeHandled() {
        val transactions =
            listOf(
                // December of previous year
                expenseTransaction(
                    amount = "100.00",
                    date = "2023-12-31",
                    payee = "December Expense",
                    firstLine = 1,
                ),
                // January of target year
                incomeTransaction(
                    amount = "3000.00",
                    date = "2024-01-15",
                    payee = "January Income",
                    firstLine = 4,
                ),
                // December of target year
                expenseTransaction(
                    amount = "200.00",
                    date = "2024-12-31",
                    payee = "December Expense",
                    firstLine = 7,
                ),
            )

        val result = calculator.calculateForMonth(transactions, 2024, 1, ".")

        assertEquals("2024-01", result.period)
        // Only January 2024 transaction should count
        assertEquals(BigDecimal("3000.00"), result.totalIncome)
        assertEquals(BigDecimal.ZERO, result.totalExpenses)
        assertEquals(BigDecimal("3000.00"), result.netFlow)
    }

    @Test
    fun transactionsShouldNotAffectOtherMonths() {
        val transactions =
            listOf(
                // January transaction
                expenseTransaction(
                    amount = "500.00",
                    date = "2024-01-15",
                    firstLine = 1,
                ),
                // February transaction
                incomeTransaction(
                    amount = "3000.00",
                    date = "2024-02-20",
                    firstLine = 4,
                ),
                // March transaction
                expenseTransaction(
                    amount = "200.00",
                    date = "2024-03-10",
                    firstLine = 7,
                ),
            )

        // Test January
        val janResult = calculator.calculateForMonth(transactions, 2024, 1, ".")
        assertEquals("2024-01", janResult.period)
        assertEquals(BigDecimal.ZERO, janResult.totalIncome)
        assertEquals(BigDecimal("500.00"), janResult.totalExpenses)

        // Test February
        val febResult = calculator.calculateForMonth(transactions, 2024, 2, ".")
        assertEquals("2024-02", febResult.period)
        assertEquals(BigDecimal("3000.00"), febResult.totalIncome)
        assertEquals(BigDecimal.ZERO, febResult.totalExpenses)

        // Test March
        val marResult = calculator.calculateForMonth(transactions, 2024, 3, ".")
        assertEquals("2024-03", marResult.period)
        assertEquals(BigDecimal.ZERO, marResult.totalIncome)
        assertEquals(BigDecimal("200.00"), marResult.totalExpenses)
    }

    @Test
    fun leapYearFebruaryShouldHandle29th() {
        val transactions =
            listOf(
                // 2024 is a leap year, February has 29 days
                incomeTransaction(
                    amount = "1000.00",
                    date = "2024-02-29",
                    payee = "Leap Day Income",
                    firstLine = 1,
                ),
            )

        val result = calculator.calculateForMonth(transactions, 2024, 2, ".")

        assertEquals("2024-02", result.period)
        assertEquals(BigDecimal("1000.00"), result.totalIncome)
    }

    @Test
    fun nonLeapYearFebruaryShouldExclude29th() {
        val transactions =
            listOf(
                // 2023 is not a leap year
                incomeTransaction(
                    amount = "1000.00",
                    date = "2023-02-28",
                    payee = "Feb 28 Income",
                    firstLine = 1,
                ),
                expenseTransaction(
                    amount = "500.00",
                    date = "2023-03-01",
                    payee = "March 1 Expense",
                    firstLine = 4,
                ),
            )

        val result = calculator.calculateForMonth(transactions, 2023, 2, ".")

        assertEquals("2023-02", result.period)
        // Feb 28 transaction should be included, March 1 should not
        assertEquals(BigDecimal("1000.00"), result.totalIncome)
        assertEquals(BigDecimal.ZERO, result.totalExpenses)
    }

    @Test
    fun shouldIgnoreNonIncomeAndNonExpensePostings() {
        val transactions =
            listOf(
                transaction(
                    date = "2024-01-15",
                    payee = "Transfer",
                    firstLine = 1,
                    lastLine = 3,
                    postings =
                        listOf(
                            // Asset to asset transfer - should not affect cash flow
                            posting("Assets:Savings", amount("1000.00")),
                            posting("Assets:Checking", amount("-1000.00")),
                        ),
                ),
                incomeTransaction(
                    amount = "2000.00",
                    date = "2024-01-20",
                    firstLine = 4,
                ),
            )

        val result = calculator.calculateForMonth(transactions, 2024, 1, ".")

        assertEquals("2024-01", result.period)
        // Only the income should count
        assertEquals(BigDecimal("2000.00"), result.totalIncome)
        assertEquals(BigDecimal.ZERO, result.totalExpenses)
    }

    @Test
    fun complexRealWorldScenario() {
        val transactions =
            listOf(
                // Opening balance
                openingTransaction(date = "2024-01-01", payee = "Opening Balance", firstLine = 1),
                // Regular salary
                incomeTransaction(
                    amount = "4500.00",
                    date = "2024-01-05",
                    payee = "Monthly Salary",
                    firstLine = 4,
                ),
                // Rent payment
                transaction(
                    date = "2024-01-01",
                    payee = "Landlord",
                    firstLine = 7,
                    lastLine = 9,
                    postings =
                        listOf(
                            posting("Expenses:Housing:Rent", amount("1200.00")),
                            posting("Assets:Checking", amount("-1200.00")),
                        ),
                ),
                // Multiple grocery trips
                expenseTransaction(
                    amount = "75.50",
                    date = "2024-01-08",
                    payee = "Supermarket",
                    firstLine = 10,
                ),
                expenseTransaction(
                    amount = "42.30",
                    date = "2024-01-15",
                    payee = "Corner Store",
                    firstLine = 13,
                ),
                expenseTransaction(
                    amount = "128.90",
                    date = "2024-01-22",
                    payee = "Grocery Store",
                    firstLine = 16,
                ),
                // Utility bills
                transaction(
                    date = "2024-01-10",
                    payee = "Electric Company",
                    firstLine = 19,
                    lastLine = 21,
                    postings =
                        listOf(
                            posting("Expenses:Utilities:Electric", amount("95.00")),
                            posting("Assets:Checking", amount("-95.00")),
                        ),
                ),
                transaction(
                    date = "2024-01-12",
                    payee = "Internet Provider",
                    firstLine = 22,
                    lastLine = 24,
                    postings =
                        listOf(
                            posting("Expenses:Utilities:Internet", amount("60.00")),
                            posting("Assets:Checking", amount("-60.00")),
                        ),
                ),
                // Dining out
                transaction(
                    date = "2024-01-18",
                    payee = "Pizza Place",
                    firstLine = 25,
                    lastLine = 27,
                    postings =
                        listOf(
                            posting("Expenses:Food:Dining", amount("35.00")),
                            posting("Assets:Checking", amount("-35.00")),
                        ),
                ),
                // Freelance income
                transaction(
                    date = "2024-01-25",
                    payee = "Client Payment",
                    firstLine = 28,
                    lastLine = 30,
                    postings =
                        listOf(
                            posting("Assets:Checking", amount("650.00")),
                            posting("Income:Freelance", amount("-650.00")),
                        ),
                ),
            )

        val result = calculator.calculateForMonth(transactions, 2024, 1, ".")

        assertEquals("2024-01", result.period)

        // Income: 4500 + 650 = 5150
        assertEquals(BigDecimal("5150.00"), result.totalIncome)

        // Expenses: 1200 + 75.50 + 42.30 + 128.90 + 95 + 60 + 35 = 1636.70
        assertEquals(BigDecimal("1636.70"), result.totalExpenses)

        // Net: 5150 - 1636.70 = 3513.30
        assertEquals(BigDecimal("3513.30"), result.netFlow)
    }

    // -------------------------------------------------------------------------
    // C1–C7: calculateForYear tests
    // -------------------------------------------------------------------------

    @Test
    fun calculateForYearShouldReturnTwelveResults() {
        val result = calculator.calculateForYear(emptyList(), 2024, ".")

        assertEquals(12, result.size)
    }

    @Test
    fun calculateForYearEmptyTransactionsShouldReturnTwelveZeroFlowMonths() {
        val result = calculator.calculateForYear(emptyList(), 2024, ".")

        assertEquals(12, result.size)
        result.forEach { month ->
            assertEquals(BigDecimal.ZERO, month.totalIncome)
            assertEquals(BigDecimal.ZERO, month.totalExpenses)
            assertEquals(BigDecimal.ZERO, month.netFlow)
        }
    }

    @Test
    fun calculateForYearResultsShouldBeOrderedJanuaryToDecember() {
        val result = calculator.calculateForYear(emptyList(), 2024, ".")

        assertEquals(12, result.size)
        val expectedPeriods =
            (1..12).map { month ->
                "2024-%02d".format(month)
            }
        result.forEachIndexed { index, cashFlowResult ->
            assertEquals(expectedPeriods[index], cashFlowResult.period)
        }
    }

    @Test
    fun calculateForYearPeriodStringShouldIdentifyEachMonth() {
        val result = calculator.calculateForYear(emptyList(), 2025, ".")

        assertEquals("2025-01", result[0].period)
        assertEquals("2025-06", result[5].period)
        assertEquals("2025-12", result[11].period)
    }

    @Test
    fun calculateForYearShouldMatchCalculateForMonthForEachMonth() {
        val transactions =
            listOf(
                incomeTransaction(amount = "3000.00", date = "2024-03-10", firstLine = 1),
                expenseTransaction(amount = "150.00", date = "2024-07-22", firstLine = 4),
                incomeTransaction(amount = "500.00", date = "2024-11-05", firstLine = 7),
            )

        val yearResult = calculator.calculateForYear(transactions, 2024, ".")

        (1..12).forEach { month ->
            val monthResult = calculator.calculateForMonth(transactions, 2024, month, ".")
            val yearMonthResult = yearResult[month - 1]
            assertEquals(
                monthResult.totalIncome,
                yearMonthResult.totalIncome,
                "totalIncome mismatch for month $month",
            )
            assertEquals(
                monthResult.totalExpenses,
                yearMonthResult.totalExpenses,
                "totalExpenses mismatch for month $month",
            )
            assertEquals(
                monthResult.netFlow,
                yearMonthResult.netFlow,
                "netFlow mismatch for month $month",
            )
            assertEquals(
                monthResult.period,
                yearMonthResult.period,
                "period mismatch for month $month",
            )
        }
    }

    @Test
    fun calculateForYearShouldOnlyIncludeTransactionsFromThatYear() {
        val transactions =
            listOf(
                incomeTransaction(amount = "1000.00", date = "2023-06-15", firstLine = 1),
                incomeTransaction(amount = "2000.00", date = "2024-06-15", firstLine = 4),
                incomeTransaction(amount = "3000.00", date = "2025-06-15", firstLine = 7),
            )

        val result = calculator.calculateForYear(transactions, 2024, ".")

        // Only the 2024 transaction should appear; sum across all months
        val totalIncomeAcrossYear = result.fold(BigDecimal.ZERO) { acc, m -> acc + m.totalIncome }
        assertEquals(BigDecimal("2000.00"), totalIncomeAcrossYear)
    }

    @Test
    fun calculateForYearWithTransactionsSpanningMultipleMonthsShouldDistributeCorrectly() {
        val transactions =
            listOf(
                incomeTransaction(amount = "5000.00", date = "2024-01-15", firstLine = 1),
                expenseTransaction(amount = "300.00", date = "2024-03-20", firstLine = 4),
            )

        val result = calculator.calculateForYear(transactions, 2024, ".")

        // January: income only
        assertEquals(BigDecimal("5000.00"), result[0].totalIncome)
        assertEquals(BigDecimal.ZERO, result[0].totalExpenses)

        // February: nothing
        assertEquals(BigDecimal.ZERO, result[1].totalIncome)
        assertEquals(BigDecimal.ZERO, result[1].totalExpenses)

        // March: expense only
        assertEquals(BigDecimal.ZERO, result[2].totalIncome)
        assertEquals(BigDecimal("300.00"), result[2].totalExpenses)

        // All other months: zero
        (3..11).forEach { idx ->
            assertEquals(
                BigDecimal.ZERO,
                result[idx].totalIncome,
                "month ${idx + 1} should have no income",
            )
            assertEquals(
                BigDecimal.ZERO,
                result[idx].totalExpenses,
                "month ${idx + 1} should have no expenses",
            )
        }
    }

    // -------------------------------------------------------------------------
    // M1: malformed date handling
    // -------------------------------------------------------------------------

    @Test
    fun malformedDateStringShouldBeIgnoredGracefully() {
        val transactions =
            listOf(
                transaction(
                    date = "not-a-date",
                    payee = "Bad Date",
                    firstLine = 1,
                    lastLine = 3,
                    postings =
                        listOf(
                            posting("Income:Salary", amount("-1000.00")),
                            posting("Assets:Checking", amount("1000.00")),
                        ),
                ),
                transaction(
                    date = "2024/01/15",
                    payee = "Wrong Separator",
                    firstLine = 4,
                    lastLine = 6,
                    postings =
                        listOf(
                            posting("Expenses:Food", amount("50.00")),
                            posting("Assets:Checking", amount("-50.00")),
                        ),
                ),
                // A valid transaction in January to confirm the filter works
                incomeTransaction(amount = "2000.00", date = "2024-01-10", firstLine = 7),
            )

        val result = calculator.calculateForMonth(transactions, 2024, 1, ".")

        // Only the valid transaction should be included; the malformed-date ones are excluded
        assertEquals(BigDecimal("2000.00"), result.totalIncome)
        assertEquals(BigDecimal.ZERO, result.totalExpenses)
    }

    // -------------------------------------------------------------------------
    // M3: asset/equity postings do not contribute to income or expenses
    // M4: liability postings do not contribute to income or expenses
    // -------------------------------------------------------------------------

    @Test
    fun assetAndEquityPostingsShouldNotContributeToCashFlow() {
        val transactions =
            listOf(
                // Asset-to-asset transfer
                transaction(
                    date = "2024-01-10",
                    payee = "Transfer to Savings",
                    firstLine = 1,
                    lastLine = 3,
                    postings =
                        listOf(
                            posting("Assets:Savings", amount("500.00")),
                            posting("Assets:Checking", amount("-500.00")),
                        ),
                ),
                // Equity opening balance
                openingTransaction(date = "2024-01-01", firstLine = 4),
                // A real income to confirm non-zero output is possible
                incomeTransaction(amount = "1000.00", date = "2024-01-15", firstLine = 7),
            )

        val result = calculator.calculateForMonth(transactions, 2024, 1, ".")

        // Asset and equity postings must not inflate income or expenses
        assertEquals(BigDecimal("1000.00"), result.totalIncome)
        assertEquals(BigDecimal.ZERO, result.totalExpenses)
    }

    @Test
    fun liabilityPostingsShouldNotContributeToCashFlow() {
        val transactions =
            listOf(
                // Credit card charge: expense + liability, no asset
                liabilityTransaction(
                    expenseAccount = "Expenses:Food",
                    amount = "120.00",
                    date = "2024-01-12",
                    firstLine = 1,
                ),
                // Liability repayment: asset decreases, liability decreases
                transaction(
                    date = "2024-01-20",
                    payee = "Credit Card Payment",
                    firstLine = 4,
                    lastLine = 6,
                    postings =
                        listOf(
                            posting("Liabilities:Credit Card", amount("120.00")),
                            posting("Assets:Checking", amount("-120.00")),
                        ),
                ),
            )

        val result = calculator.calculateForMonth(transactions, 2024, 1, ".")

        // The liability postings themselves should not appear in income
        // The expense posting from liabilityTransaction should still count
        assertEquals(BigDecimal.ZERO, result.totalIncome)
        assertEquals(BigDecimal("120.00"), result.totalExpenses)
    }

    // -------------------------------------------------------------------------
    // M5: multi-currency income/expenses are summed numerically
    // (CashFlowResult exposes a single BigDecimal total, with no currency axis)
    // -------------------------------------------------------------------------

    @Test
    fun multiCurrencyIncomeAndExpensesShouldSumAcrossCurrencies() {
        val transactions =
            listOf(
                incomeTransaction(
                    amount = "1000.00",
                    currency = "USD",
                    date = "2024-01-10",
                    firstLine = 1,
                ),
                incomeTransaction(
                    amount = "500.00",
                    currency = "EUR",
                    date = "2024-01-12",
                    firstLine = 4,
                ),
                expenseTransaction(
                    amount = "200.00",
                    currency = "USD",
                    date = "2024-01-15",
                    firstLine = 7,
                ),
                expenseTransaction(
                    amount = "100.00",
                    currency = "EUR",
                    date = "2024-01-18",
                    firstLine = 10,
                ),
            )

        val result = calculator.calculateForMonth(transactions, 2024, 1, ".")

        // Quantities are summed regardless of currency
        assertEquals(BigDecimal("1500.00"), result.totalIncome)
        assertEquals(BigDecimal("300.00"), result.totalExpenses)
        assertEquals(BigDecimal("1200.00"), result.netFlow)
    }

    // -------------------------------------------------------------------------
    // M2: a single transaction touching both income and expense accounts
    // should have both sides counted independently
    // -------------------------------------------------------------------------

    @Test
    fun incomeAndExpensePostingsInSameTransactionShouldBothBeCounted() {
        val transactions =
            listOf(
                transaction(
                    date = "2024-01-15",
                    payee = "Refund With Fee",
                    firstLine = 1,
                    lastLine = 4,
                    postings =
                        listOf(
                            // Income side (credit, stored negative)
                            posting("Income:Refund", amount("-100.00")),
                            // Expense side (debit, stored positive)
                            posting("Expenses:Fee", amount("30.00")),
                            // Asset settles the difference
                            posting("Assets:Checking", amount("70.00")),
                        ),
                ),
            )

        val result = calculator.calculateForMonth(transactions, 2024, 1, ".")

        assertEquals(BigDecimal("100.00"), result.totalIncome)
        assertEquals(BigDecimal("30.00"), result.totalExpenses)
        assertEquals(BigDecimal("70.00"), result.netFlow)
    }

    // -------------------------------------------------------------------------
    // Transaction list fields: incomeTransactions / expenseTransactions
    // -------------------------------------------------------------------------

    @Test
    fun emptyTransactionListShouldReturnEmptyTransactionLists() {
        val result = calculator.calculateForMonth(emptyList(), 2024, 1, ".")

        assertEquals(emptyList<Any>(), result.incomeTransactions)
        assertEquals(emptyList<Any>(), result.expenseTransactions)
    }

    @Test
    fun incomeTransactionShouldAppearInIncomeTransactions() {
        val tx = incomeTransaction(amount = "1000.00", date = "2024-01-10", firstLine = 1)
        val result = calculator.calculateForMonth(listOf(tx), 2024, 1, ".")

        assertEquals(listOf(tx), result.incomeTransactions)
        assertEquals(emptyList<Any>(), result.expenseTransactions)
    }

    @Test
    fun expenseTransactionShouldAppearInExpenseTransactions() {
        val tx = expenseTransaction(amount = "200.00", date = "2024-01-10", firstLine = 1)
        val result = calculator.calculateForMonth(listOf(tx), 2024, 1, ".")

        assertEquals(emptyList<Any>(), result.incomeTransactions)
        assertEquals(listOf(tx), result.expenseTransactions)
    }

    @Test
    fun mixedTransactionsShouldPopulateBothLists() {
        val income = incomeTransaction(amount = "3000.00", date = "2024-02-01", firstLine = 1)
        val expense = expenseTransaction(amount = "500.00", date = "2024-02-15", firstLine = 4)
        val result = calculator.calculateForMonth(listOf(income, expense), 2024, 2, ".")

        assertEquals(listOf(income), result.incomeTransactions)
        assertEquals(listOf(expense), result.expenseTransactions)
    }

    @Test
    fun outOfMonthTransactionsShouldNotAppearInTransactionLists() {
        val inMonth = incomeTransaction(amount = "1000.00", date = "2024-03-15", firstLine = 1)
        val outOfMonth = expenseTransaction(amount = "200.00", date = "2024-04-01", firstLine = 4)
        val result = calculator.calculateForMonth(listOf(inMonth, outOfMonth), 2024, 3, ".")

        assertEquals(listOf(inMonth), result.incomeTransactions)
        assertEquals(emptyList<Any>(), result.expenseTransactions)
    }

    @Test
    fun transactionTouchingBothIncomeAndExpenseShouldAppearInBothLists() {
        val tx =
            transaction(
                date = "2024-01-15",
                payee = "Refund With Fee",
                firstLine = 1,
                lastLine = 4,
                postings =
                    listOf(
                        posting("Income:Refund", amount("-100.00")),
                        posting("Expenses:Fee", amount("30.00")),
                        posting("Assets:Checking", amount("70.00")),
                    ),
            )
        val result = calculator.calculateForMonth(listOf(tx), 2024, 1, ".")

        assertEquals(listOf(tx), result.incomeTransactions)
        assertEquals(listOf(tx), result.expenseTransactions)
    }

    @Test
    fun transactionWithMultipleIncomePostingsShouldAppearOnceInIncomeList() {
        val tx =
            transaction(
                date = "2024-01-15",
                payee = "Split Income",
                firstLine = 1,
                lastLine = 4,
                postings =
                    listOf(
                        posting("Assets:Checking", amount("700.00")),
                        posting("Income:Salary", amount("-500.00")),
                        posting("Income:Bonus", amount("-200.00")),
                    ),
            )
        val result = calculator.calculateForMonth(listOf(tx), 2024, 1, ".")

        assertEquals(listOf(tx), result.incomeTransactions)
        assertEquals(emptyList<Any>(), result.expenseTransactions)
    }

    @Test
    fun transactionWithMultipleExpensePostingsShouldAppearOnceInExpenseList() {
        val tx =
            transaction(
                date = "2024-01-15",
                payee = "Shared Grocery Run",
                firstLine = 1,
                lastLine = 4,
                postings =
                    listOf(
                        posting("Expenses:Food:Groceries", amount("60.00")),
                        posting("Expenses:Food:Snacks", amount("15.00")),
                        posting("Assets:Checking", amount("-75.00")),
                    ),
            )
        val result = calculator.calculateForMonth(listOf(tx), 2024, 1, ".")

        assertEquals(emptyList<Any>(), result.incomeTransactions)
        assertEquals(listOf(tx), result.expenseTransactions)
    }

    @Test
    fun incomeTransactionListShouldBeSortedByFirstLine() {
        val tx1 = incomeTransaction(amount = "1000.00", date = "2024-01-20", firstLine = 7)
        val tx2 = incomeTransaction(amount = "2000.00", date = "2024-01-05", firstLine = 1)
        val tx3 = incomeTransaction(amount = "500.00", date = "2024-01-12", firstLine = 4)
        // Pass in deliberately unsorted order
        val result = calculator.calculateForMonth(listOf(tx1, tx2, tx3), 2024, 1, ".")

        assertEquals(listOf(tx2, tx3, tx1), result.incomeTransactions)
    }

    @Test
    fun expenseTransactionListShouldBeSortedByFirstLine() {
        val tx1 = expenseTransaction(amount = "100.00", date = "2024-01-20", firstLine = 7)
        val tx2 = expenseTransaction(amount = "50.00", date = "2024-01-02", firstLine = 1)
        val tx3 = expenseTransaction(amount = "75.00", date = "2024-01-10", firstLine = 4)
        val result = calculator.calculateForMonth(listOf(tx1, tx2, tx3), 2024, 1, ".")

        assertEquals(listOf(tx2, tx3, tx1), result.expenseTransactions)
    }

    @Test
    fun assetOnlyTransactionShouldNotAppearInEitherList() {
        val tx =
            transaction(
                date = "2024-01-10",
                payee = "Transfer",
                firstLine = 1,
                lastLine = 3,
                postings =
                    listOf(
                        posting("Assets:Savings", amount("500.00")),
                        posting("Assets:Checking", amount("-500.00")),
                    ),
            )
        val result = calculator.calculateForMonth(listOf(tx), 2024, 1, ".")

        assertEquals(emptyList<Any>(), result.incomeTransactions)
        assertEquals(emptyList<Any>(), result.expenseTransactions)
    }

    // -------------------------------------------------------------------------
    // M?: custom prefix lists should classify income/expense correctly
    // -------------------------------------------------------------------------

    @Test
    fun customPrefixListsShouldClassifyIncomeAndExpenses() {
        val transactions =
            listOf(
                transaction(
                    date = "2024-01-15",
                    payee = "Custom Prefixes",
                    firstLine = 1,
                    lastLine = 5,
                    postings =
                        listOf(
                            posting("MyIncome:Salary", amount("-3000.00")),
                            posting("MyExpenses:Food", amount("150.00")),
                            posting("Assets:Checking", amount("2850.00")),
                        ),
                ),
            )

        val result = calculator.calculateForMonth(
            transactions,
            2024,
            1,
            ".",
            incomePrefixes = listOf("MyIncome"),
            expensesPrefixes = listOf("MyExpenses"),
        )

        assertEquals("2024-01", result.period)
        assertEquals(BigDecimal("3000.00"), result.totalIncome)
        assertEquals(BigDecimal("150.00"), result.totalExpenses)
        assertEquals(BigDecimal("2850.00"), result.netFlow)
    }

    @Test
    fun multiplePrefixesForSameCashFlowTypeShouldMatchAny() {
        val transactions =
            listOf(
                transaction(
                    date = "2024-01-15",
                    payee = "Multiple Prefixes",
                    firstLine = 1,
                    lastLine = 5,
                    postings =
                        listOf(
                            posting("Income:Salary", amount("-2000.00")),
                            posting("Einkommen:Freelance", amount("-500.00")),
                            posting("Assets:Checking", amount("2500.00")),
                        ),
                ),
            )

        val result = calculator.calculateForMonth(
            transactions,
            2024,
            1,
            ".",
            incomePrefixes = listOf("Income", "Einkommen"),
        )

        assertEquals(BigDecimal("2500.00"), result.totalIncome)
        assertEquals(BigDecimal.ZERO, result.totalExpenses)
        assertEquals(BigDecimal("2500.00"), result.netFlow)
    }
}
