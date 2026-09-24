package ph.chrsrns.microledger.data.reporting

import ph.chrsrns.microledger.data.AccountType
import java.math.BigDecimal

/**
 * Calculates net worth from a list of transactions.
 *
 * Net worth = Total Assets - Total Liabilities
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
     * Calculates net worth from the given inputs.
     *
     * @param inputs Transactions and reporting preferences to process
     * @return NetWorthResult containing the calculated values
     */
    fun calculate(inputs: ReportingInputs): NetWorthResult {
        val decimalSeparator = inputs.preferences.decimalSeparator
        val prefixes = inputs.preferences.prefixes
        var totalAssets = BigDecimal.ZERO
        var totalLiabilities = BigDecimal.ZERO

        for (transaction in inputs.transactions) {
            for (posting in transaction.postings) {
                val amount = posting.amount ?: continue
                val account = posting.account ?: continue
                val quantity = parseQuantity(amount.quantity, decimalSeparator)

                when (prefixes.classify(account)) {
                    AccountType.ASSETS -> {
                        totalAssets += quantity
                    }

                    AccountType.LIABILITIES -> {
                        // Liabilities are stored as negative in postings (credits)
                        // We treat them as positive amounts for net worth calculation
                        totalLiabilities += quantity.negate()
                    }

                    else -> {}
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
