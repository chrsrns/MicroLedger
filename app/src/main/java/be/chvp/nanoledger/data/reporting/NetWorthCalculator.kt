package be.chvp.nanoledger.data.reporting

import be.chvp.nanoledger.data.Transaction
import java.math.BigDecimal

/**
 * Calculates net worth from a list of transactions.
 *
 * Net worth = Total Assets - Total Liabilities
 *
 * Accounts starting with "Assets" are considered assets.
 * Accounts starting with "Liabilities" are considered liabilities.
 */
class NetWorthCalculator {
    /**
     * Result containing net worth information.
     */
    data class NetWorthResult(
        val netWorth: BigDecimal,
        val totalAssets: BigDecimal,
        val totalLiabilities: BigDecimal,
    )

    /**
     * Calculates net worth from the given transactions.
     *
     * @param transactions List of transactions to process
     * @param decimalSeparator The user's decimal separator used to parse quantities
     * @return NetWorthResult containing the calculated values
     */
    fun calculate(
        transactions: List<Transaction>,
        decimalSeparator: String,
    ): NetWorthResult {
        var totalAssets = BigDecimal.ZERO
        var totalLiabilities = BigDecimal.ZERO

        for (transaction in transactions) {
            for (posting in transaction.postings) {
                val amount = posting.amount ?: continue
                val account = posting.account ?: continue
                val quantity = parseQuantity(amount.quantity, decimalSeparator)

                when {
                    account.startsWith("Assets", ignoreCase = true) -> {
                        totalAssets += quantity
                    }

                    account.startsWith("Liabilities", ignoreCase = true) -> {
                        // Liabilities are stored as negative in postings (credits)
                        // We treat them as positive amounts for net worth calculation
                        totalLiabilities += quantity.negate()
                    }
                }
            }
        }

        val netWorth = totalAssets.subtract(totalLiabilities)

        return NetWorthResult(
            netWorth = netWorth,
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
        )
    }
}
