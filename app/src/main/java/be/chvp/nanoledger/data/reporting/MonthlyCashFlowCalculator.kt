package be.chvp.nanoledger.data.reporting

import be.chvp.nanoledger.data.Transaction
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
    )

    /**
     * Calculates cash flow for a specific month.
     *
     * @param transactions List of transactions to process
     * @param year The year (e.g., 2024)
     * @param month The month (1-12)
     * @param decimalSeparator The user's decimal separator used to parse quantities
     * @return CashFlowResult for the specified month
     */
    fun calculateForMonth(
        transactions: List<Transaction>,
        year: Int,
        month: Int,
        decimalSeparator: String,
    ): CashFlowResult {
        val period = String.format(Locale.US, "%04d-%02d", year, month)
        var totalIncome = BigDecimal.ZERO
        var totalExpenses = BigDecimal.ZERO

        for (transaction in transactions) {
            if (!isTransactionInMonth(transaction.date, year, month)) {
                continue
            }

            for (posting in transaction.postings) {
                val amount = posting.amount ?: continue
                val account = posting.account ?: continue
                val quantity = parseQuantity(amount.quantity, decimalSeparator)

                when {
                    account.startsWith("Income", ignoreCase = true) -> {
                        // Income postings are credits (negative amounts in ledger)
                        // Display as positive for cash flow
                        totalIncome += quantity.negate()
                    }

                    account.startsWith("Expenses", ignoreCase = true) -> {
                        // Expense postings are debits (positive amounts in ledger)
                        // Keep as positive for cash flow
                        totalExpenses += quantity
                    }
                }
            }
        }

        val netFlow = totalIncome.subtract(totalExpenses)

        return CashFlowResult(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netFlow = netFlow,
            period = period,
        )
    }

    /**
     * Calculates cash flow for multiple months.
     *
     * @param transactions List of transactions to process
     * @param year The year
     * @param decimalSeparator The user's decimal separator used to parse quantities
     * @return List of CashFlowResult for each month in the year
     */
    fun calculateForYear(
        transactions: List<Transaction>,
        year: Int,
        decimalSeparator: String,
    ): List<CashFlowResult> =
        (1..12).map { month ->
            calculateForMonth(transactions, year, month, decimalSeparator)
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
