package ph.chrsrns.microledger.data.reporting

import ph.chrsrns.microledger.data.AccountType
import java.math.BigDecimal

/**
 * Calculates net worth from a list of transactions.
 *
 * Net worth = Total Assets - Total Liabilities, reported per currency.
 */
class NetWorthCalculator {
    /**
     * Net worth information for a single currency.
     */
    data class CurrencyNetWorth(
        val currency: String,
        val netWorth: BigDecimal,
        val totalAssets: BigDecimal,
        val totalLiabilities: BigDecimal,
    )

    /**
     * Calculates net worth from the given inputs.
     *
     * @param inputs Transactions and reporting preferences to process
     * @return One entry per distinct currency among asset/liability postings,
     *   sorted by currency; currencies are never summed across each other
     */
    fun calculate(inputs: ReportingInputs): List<CurrencyNetWorth> {
        val decimalSeparator = inputs.preferences.decimalSeparator
        val prefixes = inputs.preferences.prefixes

        // currency -> (totalAssets, totalLiabilities)
        val totals = mutableMapOf<String, Pair<BigDecimal, BigDecimal>>()

        for (transaction in inputs.transactions) {
            for (posting in materializeElidedAmounts(transaction, decimalSeparator).postings) {
                val amount = posting.amount ?: continue
                val account = posting.account ?: continue
                val quantity = parseQuantity(amount.quantity, decimalSeparator)
                val (assets, liabilities) = totals[amount.currency] ?: (BigDecimal.ZERO to BigDecimal.ZERO)

                when (prefixes.classify(account)) {
                    AccountType.ASSETS -> {
                        totals[amount.currency] = (assets + quantity) to liabilities
                    }

                    AccountType.LIABILITIES -> {
                        // Liabilities are stored as negative in postings (credits)
                        // We treat them as positive amounts for net worth calculation
                        totals[amount.currency] = assets to (liabilities + quantity.negate())
                    }

                    else -> {}
                }
            }
        }

        return totals.entries.sortedBy { it.key }.map { (currency, pair) ->
            val (totalAssets, totalLiabilities) = pair
            CurrencyNetWorth(
                currency = currency,
                netWorth = totalAssets.subtract(totalLiabilities),
                totalAssets = totalAssets,
                totalLiabilities = totalLiabilities,
            )
        }
    }
}
