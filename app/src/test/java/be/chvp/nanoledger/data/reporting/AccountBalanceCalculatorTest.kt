package be.chvp.nanoledger.data.reporting

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountBalanceCalculatorTest {
    private val calculator = AccountBalanceCalculator()

    @Test
    fun emptyTransactionListShouldReturnEmptyBalances() {
        val result = calculator.calculate(emptyList(), ".")

        assertTrue(result.assets.isEmpty())
        assertTrue(result.liabilities.isEmpty())
        assertTrue(result.equity.isEmpty())
        assertTrue(result.income.isEmpty())
        assertTrue(result.expenses.isEmpty())
    }

    @Test
    fun singleAssetAccountShouldReturnCorrectBalance() {
        val transactions = listOf(openingTransaction())

        val result = calculator.calculate(transactions, ".")

        assertEquals(1, result.assets.size)
        assertEquals("Assets:Checking", result.assets[0].account)
        assertEquals(BigDecimal("1000.00"), result.assets[0].balance)
        assertEquals("$", result.assets[0].currency)
        assertTrue(result.liabilities.isEmpty())
    }

    @Test
    fun multipleAssetAccountsShouldReturnSeparateBalances() {
        val transactions = listOf(
            transaction(
                date = "2024-01-15",
                payee = "Opening Balances",
                firstLine = 1,
                lastLine = 4,
                postings = listOf(
                    posting("Assets:Checking", amount("1000.00")),
                    posting("Assets:Savings", amount("5000.00")),
                    posting("Equity:Opening Balances", amount("-6000.00")),
                ),
            ),
        )

        val result = calculator.calculate(transactions, ".")

        assertEquals(2, result.assets.size)
        val checkingBalance = result.assets.find { it.account == "Assets:Checking" }
        val savingsBalance = result.assets.find { it.account == "Assets:Savings" }
        assertEquals(BigDecimal("1000.00"), checkingBalance?.balance)
        assertEquals(BigDecimal("5000.00"), savingsBalance?.balance)
    }

    @Test
    fun hierarchicalAccountsShouldBeTrackedSeparately() {
        val transactions = listOf(
            transaction(
                date = "2024-01-15",
                payee = "Setup",
                firstLine = 1,
                lastLine = 5,
                postings = listOf(
                    posting("Assets:Bank:Checking", amount("1000.00")),
                    posting("Assets:Bank:Savings", amount("5000.00")),
                    posting("Assets:Cash", amount("200.00")),
                    posting("Equity:Opening", amount("-6200.00")),
                ),
            ),
        )

        val result = calculator.calculate(transactions, ".")

        assertEquals(3, result.assets.size)
        assertTrue(result.assets.any {
            it.account == "Assets:Bank:Checking" && it.balance == BigDecimal(
                "1000.00"
            )
        })
        assertTrue(result.assets.any {
            it.account == "Assets:Bank:Savings" && it.balance == BigDecimal(
                "5000.00"
            )
        })
        assertTrue(result.assets.any { it.account == "Assets:Cash" && it.balance == BigDecimal("200.00") })
    }

    @Test
    fun allAccountTypesShouldBeClassifiedCorrectly() {
        val transactions = listOf(
            transaction(
                date = "2024-01-15",
                payee = "Complete Setup",
                firstLine = 1,
                lastLine = 6,
                postings = listOf(
                    posting("Assets:Checking", amount("1000.00")),
                    posting("Liabilities:Credit Card", amount("-200.00")),
                    posting("Equity:Opening Balances", amount("-800.00")),
                    posting("Income:Salary", amount("-5000.00")),
                    posting("Expenses:Food", amount("3000.00")),
                    posting("Assets:Checking", amount("2000.00")),
                ),
            ),
        )

        val result = calculator.calculate(transactions, ".")

        assertEquals(1, result.assets.size)
        assertEquals("Assets:Checking", result.assets[0].account)
        assertEquals(BigDecimal("3000.00"), result.assets[0].balance)

        assertEquals(1, result.liabilities.size)
        assertEquals("Liabilities:Credit Card", result.liabilities[0].account)
        assertEquals(BigDecimal("200.00"), result.liabilities[0].balance)

        assertEquals(1, result.equity.size)
        assertEquals("Equity:Opening Balances", result.equity[0].account)
        assertEquals(BigDecimal("800.00"), result.equity[0].balance)

        assertEquals(1, result.income.size)
        assertEquals("Income:Salary", result.income[0].account)
        assertEquals(BigDecimal("5000.00"), result.income[0].balance)

        assertEquals(1, result.expenses.size)
        assertEquals("Expenses:Food", result.expenses[0].account)
        assertEquals(BigDecimal("3000.00"), result.expenses[0].balance)
    }

    @Test
    fun mixedIncomeAndExpenseShouldCalculateCorrectly() {
        val transactions = listOf(
            transaction(
                date = "2024-01-15",
                payee = "Paycheck",
                firstLine = 1,
                lastLine = 3,
                postings = listOf(
                    posting("Assets:Checking", amount("3000.00")),
                    posting("Liabilities:Credit Card", amount("-500.00")),
                    posting("Income:Salary", amount("-2500.00")),
                ),
            ),
        )

        val result = calculator.calculate(transactions, ".")

        assertEquals(1, result.assets.size)
        assertEquals("Assets:Checking", result.assets[0].account)
        assertEquals(BigDecimal("3000.00"), result.assets[0].balance)

        assertEquals(1, result.liabilities.size)
        assertEquals("Liabilities:Credit Card", result.liabilities[0].account)
        assertEquals(BigDecimal("500.00"), result.liabilities[0].balance)

        assertEquals(1, result.income.size)
        assertEquals("Income:Salary", result.income[0].account)
        assertEquals(BigDecimal("2500.00"), result.income[0].balance)
    }

    @Test
    fun multiCurrencyAccountShouldShowSeparateBalances() {
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
                payee = "EUR Opening",
                firstLine = 4,
                lastLine = 6,
                postings = listOf(
                    posting("Assets:Checking", amount("500.00", "EUR")),
                    posting("Equity:Opening", amount("-500.00", "EUR")),
                ),
            ),
        )

        val result = calculator.calculate(transactions, ".")

        assertEquals(2, result.assets.size)
        val usdBalance = result.assets.find { it.currency == "USD" }
        val eurBalance = result.assets.find { it.currency == "EUR" }
        assertEquals(BigDecimal("1000.00"), usdBalance?.balance)
        assertEquals(BigDecimal("500.00"), eurBalance?.balance)
    }

    @Test
    fun accountBalanceShouldAggregateMultipleTransactions() {
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
                payee = "Deposit",
                firstLine = 4
            ),
            expenseTransaction(
                amount = "500.00",
                date = "2024-01-20",
                payee = "Withdrawal",
                firstLine = 7
            ),
        )

        val result = calculator.calculate(transactions, ".")

        assertEquals(1, result.assets.size)
        assertEquals("Assets:Checking", result.assets[0].account)
        // 1000 + 2000 - 500 = 2500
        assertEquals(BigDecimal("2500.00"), result.assets[0].balance)
    }

    @Test
    fun incomeAndExpenseBalancesShouldAccumulate() {
        val transactions = listOf(
            incomeTransaction(
                amount = "5000.00",
                date = "2024-01-01",
                payee = "Salary",
                firstLine = 1
            ),
            transaction(
                date = "2024-01-15",
                payee = "Bonus",
                firstLine = 4,
                lastLine = 6,
                postings = listOf(
                    posting("Assets:Checking", amount("1000.00")),
                    posting("Income:Bonus", amount("-1000.00")),
                ),
            ),
            expenseTransaction(
                amount = "200.00",
                date = "2024-01-10",
                payee = "Groceries",
                firstLine = 7
            ),
            expenseTransaction(
                amount = "150.00",
                date = "2024-01-20",
                payee = "Restaurant",
                firstLine = 10
            ),
        )

        val result = calculator.calculate(transactions, ".")

        // Income: Salary = 5000, Bonus = 1000 (stored as negative in postings)
        assertEquals(2, result.income.size)
        assertEquals(
            BigDecimal("5000.00"),
            result.income.find { it.account == "Income:Salary" }?.balance
        )
        assertEquals(
            BigDecimal("1000.00"),
            result.income.find { it.account == "Income:Bonus" }?.balance
        )

        // Expenses: Food = 200 + 150 = 350
        assertEquals(1, result.expenses.size)
        assertEquals(BigDecimal("350.00"), result.expenses[0].balance)
    }

    @Test
    fun postingsWithoutAmountShouldNotAffectBalances() {
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

        // Only Expenses:Food should have a balance
        assertEquals(1, result.expenses.size)
        assertEquals(BigDecimal("50.00"), result.expenses[0].balance)
        // Assets:Checking has no posting with amount, so it shouldn't appear
        assertTrue(result.assets.isEmpty())
    }

    @Test
    fun accountsWithZeroBalanceShouldBeIncluded() {
        val transactions = listOf(
            transaction(
                date = "2024-01-01",
                payee = "Transfer",
                firstLine = 1,
                lastLine = 4,
                postings = listOf(
                    posting("Assets:Checking", amount("1000.00")),
                    posting("Assets:Savings", amount("-1000.00")),
                ),
            ),
        )

        val result = calculator.calculate(transactions, ".")

        assertEquals(2, result.assets.size)
        assertEquals(
            BigDecimal("1000.00"),
            result.assets.find { it.account == "Assets:Checking" }?.balance
        )
        assertEquals(
            BigDecimal("-1000.00"),
            result.assets.find { it.account == "Assets:Savings" }?.balance
        )
    }

    @Test
    fun lowercaseAccountTypesShouldBeClassifiedCorrectly() {
        val transactions = listOf(
            transaction(
                date = "2024-01-15",
                payee = "Lowercase Setup",
                firstLine = 1,
                lastLine = 6,
                postings = listOf(
                    posting("assets:checking", amount("1000.00")),
                    posting("liabilities:credit card", amount("-200.00")),
                    posting("equity:opening balances", amount("-500.00")),
                    posting("income:salary", amount("-800.00")),
                    posting("expenses:food", amount("500.00")),
                ),
            ),
        )

        val result = calculator.calculate(transactions, ".")

        assertEquals(1, result.assets.size)
        assertEquals(1, result.liabilities.size)
        assertEquals(1, result.equity.size)
        assertEquals(1, result.income.size)
        assertEquals(1, result.expenses.size)
    }

    @Test
    fun lowercaseAssetAccountShouldTrackBalance() {
        val transactions = listOf(
            transaction(
                date = "2024-01-15",
                payee = "Opening Balance",
                firstLine = 1,
                lastLine = 3,
                postings = listOf(
                    posting("assets:bank:checking", amount("2500.00")),
                    posting("equity:opening", amount("-2500.00")),
                ),
            ),
        )

        val result = calculator.calculate(transactions, ".")

        assertEquals(1, result.assets.size)
        assertEquals("assets:bank:checking", result.assets[0].account)
        assertEquals(BigDecimal("2500.00"), result.assets[0].balance)
        assertTrue(result.liabilities.isEmpty())
    }
}
