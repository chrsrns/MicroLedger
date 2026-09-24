package ph.chrsrns.microledger.data.reporting

import ph.chrsrns.microledger.data.AccountType
import ph.chrsrns.microledger.data.Transaction
import java.math.BigDecimal
import java.util.Locale

/**
 * Calculates monthly cash flow from a list of transactions.
 *
 * Cash flow = Total Income - Total Expenses for a given period.
 */
class MonthlyCashFlowCalculator {
    /**
     * Result containing cash flow information for a period.
     */
    data class CashFlowResult(
        val totalIncome: BigDecimal,
        val totalExpenses: BigDecimal,
        val netFlow: BigDecimal,
        val period: String,
        val incomeTransactions: List<Transaction>,
        val expenseTransactions: List<Transaction>,
    )

    /**
     * Calculates cash flow for a specific month.
     *
     * @param inputs Transactions and reporting preferences to process
     * @param year The year (e.g., 2024)
     * @param month The month (1-12)
     * @return CashFlowResult for the specified month
     */
    fun calculateForMonth(
        inputs: ReportingInputs,
        year: Int,
        month: Int,
    ): CashFlowResult {
        val decimalSeparator = inputs.preferences.decimalSeparator
        val prefixes = inputs.preferences.prefixes
        val period = String.format(Locale.US, "%04d-%02d", year, month)
        var totalIncome = BigDecimal.ZERO
        var totalExpenses = BigDecimal.ZERO
        val incomeTransactionSet = mutableSetOf<Transaction>()
        val expenseTransactionSet = mutableSetOf<Transaction>()

        for (transaction in inputs.transactions) {
            if (!isTransactionInMonth(transaction.date, year, month)) {
                continue
            }

            for (posting in transaction.postings) {
                val amount = posting.amount ?: continue
                val account = posting.account ?: continue
                val quantity = parseQuantity(amount.quantity, decimalSeparator)

                when (prefixes.classify(account)) {
                    AccountType.INCOME -> {
                        // Income postings are credits (negative amounts in ledger)
                        // Display as positive for cash flow
                        totalIncome += quantity.negate()
                        incomeTransactionSet.add(transaction)
                    }

                    AccountType.EXPENSES -> {
                        // Expense postings are debits (positive amounts in ledger)
                        // Keep as positive for cash flow
                        totalExpenses += quantity
                        expenseTransactionSet.add(transaction)
                    }

                    else -> {}
                }
            }
        }

        val netFlow = totalIncome.subtract(totalExpenses)

        return CashFlowResult(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netFlow = netFlow,
            period = period,
            incomeTransactions = incomeTransactionSet.sortedBy { it.firstLine },
            expenseTransactions = expenseTransactionSet.sortedBy { it.firstLine },
        )
    }

    private fun isTransactionInMonth(
        date: String,
        year: Int,
        month: Int,
    ): Boolean {
        return try {
            val parts = date.split("-")
            if (parts.size < 2) return false

            val transactionYear = parts[0].toInt()
            val transactionMonth = parts[1].toInt()

            transactionYear == year && transactionMonth == month
        } catch (e: Exception) {
            false
        }
    }
}
