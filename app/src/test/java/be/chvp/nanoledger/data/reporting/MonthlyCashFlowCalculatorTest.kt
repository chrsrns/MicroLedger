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
        val transactions = listOf(
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
        val transactions = listOf(
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
        val transactions = listOf(
            incomeTransaction(amount = "5000.00", date = "2024-02-01"),
            transaction(
                date = "2024-02-15",
                payee = "Landlord",
                firstLine = 4,
                lastLine = 6,
                postings = listOf(
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
        val transactions = listOf(
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
        val transactions = listOf(
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
        val transactions = listOf(
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
        val transactions = listOf(
            transaction(
                date = "2024-01-05",
                payee = "Primary Salary",
                firstLine = 1,
                lastLine = 3,
                postings = listOf(
                    posting("Assets:Checking", amount("3000.00")),
                    posting("Income:Salary", amount("-3000.00")),
                ),
            ),
            transaction(
                date = "2024-01-10",
                payee = "Side Gig",
                firstLine = 4,
                lastLine = 6,
                postings = listOf(
                    posting("Assets:Checking", amount("500.00")),
                    posting("Income:Freelance", amount("-500.00")),
                ),
            ),
            transaction(
                date = "2024-01-20",
                payee = "Bonus",
                firstLine = 7,
                lastLine = 9,
                postings = listOf(
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
        val transactions = listOf(
            transaction(
                date = "2024-01-02",
                payee = "Grocery Store",
                firstLine = 1,
                lastLine = 3,
                postings = listOf(
                    posting("Expenses:Food:Groceries", amount("85.50")),
                    posting("Assets:Checking", amount("-85.50")),
                ),
            ),
            transaction(
                date = "2024-01-05",
                payee = "Electric Bill",
                firstLine = 4,
                lastLine = 6,
                postings = listOf(
                    posting("Expenses:Utilities:Electric", amount("120.00")),
                    posting("Assets:Checking", amount("-120.00")),
                ),
            ),
            transaction(
                date = "2024-01-15",
                payee = "Restaurant",
                firstLine = 7,
                lastLine = 9,
                postings = listOf(
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
        val transactions = listOf(
            transaction(
                date = "2024-01-05",
                payee = "Salary",
                firstLine = 1,
                lastLine = 3,
                postings = listOf(
                    posting("Assets:Checking", amount("4000.00")),
                    posting("Income:Salary", amount("-4000.00")),
                ),
            ),
            transaction(
                date = "2024-01-10",
                payee = "Rent",
                firstLine = 4,
                lastLine = 6,
                postings = listOf(
                    posting("Expenses:Housing:Rent", amount("1200.00")),
                    posting("Assets:Checking", amount("-1200.00")),
                ),
            ),
            transaction(
                date = "2024-01-15",
                payee = "Utilities",
                firstLine = 7,
                lastLine = 9,
                postings = listOf(
                    posting("Expenses:Utilities", amount("150.00")),
                    posting("Assets:Checking", amount("-150.00")),
                ),
            ),
            transaction(
                date = "2024-01-20",
                payee = "Freelance Work",
                firstLine = 10,
                lastLine = 12,
                postings = listOf(
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
        val transactions = listOf(
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
        val transactions = listOf(
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
        val transactions = listOf(
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
        val transactions = listOf(
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
        val transactions = listOf(
            transaction(
                date = "2024-01-15",
                payee = "Transfer",
                firstLine = 1,
                lastLine = 3,
                postings = listOf(
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
        val transactions = listOf(
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
                postings = listOf(
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
                postings = listOf(
                    posting("Expenses:Utilities:Electric", amount("95.00")),
                    posting("Assets:Checking", amount("-95.00")),
                ),
            ),
            transaction(
                date = "2024-01-12",
                payee = "Internet Provider",
                firstLine = 22,
                lastLine = 24,
                postings = listOf(
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
                postings = listOf(
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
                postings = listOf(
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
}
