package ph.chrsrns.microledger.data.reporting

import ph.chrsrns.microledger.data.AccountType
import ph.chrsrns.microledger.data.Posting
import ph.chrsrns.microledger.data.Transaction
import ph.chrsrns.microledger.data.normalBalanceSign
import java.math.BigDecimal

// Shared posting walker for reporting calculators (V341/V342/V345).
// Owns elided-amount materialization, null filtering, quantity
// parsing, account classification, and V116 orientation. Yields the
// input Transaction (not the materialized copy) in posting order;
// unclassified accounts are never yielded.
internal fun forEachClassifiedAmount(
    inputs: ReportingInputs,
    yield: (Transaction, Posting, AccountType, BigDecimal) -> Unit,
) {
    val decimalSeparator = inputs.preferences.decimalSeparator
    for (transaction in inputs.transactions) {
        for (posting in materializeElidedAmounts(transaction, decimalSeparator).postings) {
            val amount = posting.amount ?: continue
            val account = posting.account ?: continue
            val type = inputs.preferences.prefixes.classify(account) ?: continue
            val quantity =
                parseQuantity(amount.quantity, decimalSeparator)
                    .multiply(BigDecimal(normalBalanceSign(type)))
            yield(transaction, posting, type, quantity)
        }
    }
}
