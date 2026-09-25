package ph.chrsrns.microledger.data.reporting

import ph.chrsrns.microledger.data.Amount
import ph.chrsrns.microledger.data.Transaction
import java.math.BigDecimal

/**
 * Materializes the amount of a single elided (balancing) posting in a transaction.
 *
 * A posting is elided when it is non-virtual, non-comment, has a non-blank
 * account, and has no amount — ledger syntax treats it as balancing the rest of
 * the transaction. Materialization happens only when the sibling postings make
 * the missing amount unambiguous:
 *
 * - exactly one elided posting exists
 * - no sibling has a cost
 * - no sibling has an assertion without an amount
 * - no sibling has an assertion currency that differs from its amount currency
 * - at most one distinct currency across siblings that have an amount
 *
 * When these hold, the elided posting receives the negated sum of the sibling
 * quantities in the single sibling currency. Otherwise the transaction is
 * returned unchanged and the elided posting is skipped by the calculators.
 */
internal fun materializeElidedAmounts(
    transaction: Transaction,
    decimalSeparator: String,
): Transaction {
    val siblings =
        transaction.postings.filter { p ->
            !p.isVirtual() && !p.isComment() && !p.account.isNullOrBlank()
        }
    val elided = siblings.filter { it.amount == null }
    if (elided.size != 1) return transaction

    if (siblings.any { it.cost != null }) return transaction
    if (siblings.any { it.assertion != null && it.amount == null }) return transaction
    if (siblings.any {
            it.assertion != null &&
                it.assertion.currency.isNotEmpty() &&
                it.assertion.currency != it.amount?.currency
        }
    ) {
        return transaction
    }

    val currencies = siblings.mapNotNull { it.amount?.currency }.distinct()
    if (currencies.size > 1) return transaction

    val quantity =
        siblings
            .mapNotNull { it.amount }
            .fold(BigDecimal.ZERO) { acc, a ->
                acc + parseQuantity(a.quantity, decimalSeparator)
            }.negate()
            .toPlainString()
    val currency = currencies.singleOrNull() ?: ""
    val materialized = Amount(quantity, currency, "$quantity $currency".trim())

    return transaction.copy(
        postings =
            transaction.postings.map { p ->
                if (p === elided[0]) p.withAmount(materialized) else p
            },
    )
}
