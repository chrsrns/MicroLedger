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
        // currency -> (totalAssets, totalLiabilities)
        val totals = mutableMapOf<String, Pair<BigDecimal, BigDecimal>>()

        forEachClassifiedAmount(inputs) { _, posting, type, quantity ->
            val currency = posting.amount?.currency ?: return@forEachClassifiedAmount
            val (assets, liabilities) = totals[currency] ?: (BigDecimal.ZERO to BigDecimal.ZERO)
            when (type) {
                AccountType.ASSETS -> totals[currency] = (assets + quantity) to liabilities
                AccountType.LIABILITIES -> totals[currency] = assets to (liabilities + quantity)
                else -> {}
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
