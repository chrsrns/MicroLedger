package ph.chrsrns.microledger.data.reporting

import ph.chrsrns.microledger.data.Amount
import ph.chrsrns.microledger.data.Posting
import ph.chrsrns.microledger.data.Transaction
import java.math.BigDecimal

/**
 * Computes the amount that would balance a set of postings, or `null` when the
 * balancing amount is ambiguous.
 *
 * Only qualifying postings (non-virtual, non-comment, non-blank account) are
 * considered. The result is `null` when:
 *
 * - any qualifying posting has a cost
 * - any qualifying posting has an assertion without an amount
 * - any qualifying posting has an assertion currency that differs from its
 *   amount currency
 * - more than one distinct currency occurs across amount-carrying postings
 *
 * Otherwise the result is the negated sum of the sibling quantities in the
 * single sibling currency (empty string when there is none).
 */
internal fun balancingAmount(
    postings: List<Posting>,
    decimalSeparator: String,
): Amount? {
    val qualifying = postings.filter { it.isQualifying() }

    if (qualifying.any { it.cost != null }) return null
    if (qualifying.any { it.assertion != null && it.amount == null }) return null
    if (qualifying.any {
            it.assertion != null &&
                it.assertion.currency.isNotEmpty() &&
                it.assertion.currency != it.amount?.currency
        }
    ) {
        return null
    }

    val currencies = qualifying.mapNotNull { it.amount?.currency }.distinct()
    if (currencies.size > 1) return null

    val quantity =
        qualifying
            .mapNotNull { it.amount }
            .fold(BigDecimal.ZERO) { acc, a ->
                acc + parseQuantity(a.quantity, decimalSeparator)
            }.negate()
            .toPlainString()
    val currency = currencies.singleOrNull() ?: ""

    return Amount(quantity, currency, "$quantity $currency".trim())
}

/**
 * Materializes the amount of a single elided (balancing) posting in a transaction.
 *
 * A posting is elided when it is qualifying and has no amount — ledger syntax
 * treats it as balancing the rest of the transaction. Materialization happens
 * only when exactly one elided posting exists and [balancingAmount] produces a
 * result for the sibling postings. Otherwise the transaction is returned
 * unchanged and the elided posting is skipped by the calculators.
 */
internal fun materializeElidedAmounts(
    transaction: Transaction,
    decimalSeparator: String,
): Transaction {
    val elided = transaction.postings.filter { it.isQualifying() && it.amount == null }
    if (elided.size != 1) return transaction

    val materialized = balancingAmount(transaction.postings, decimalSeparator) ?: return transaction

    return transaction.copy(
        postings =
            transaction.postings.map { p ->
                if (p === elided[0]) p.copy(amount = materialized) else p
            },
    )
}
