package ph.chrsrns.microledger.ui.common

import ph.chrsrns.microledger.data.Amount
import ph.chrsrns.microledger.data.Cost
import ph.chrsrns.microledger.data.CostType
import ph.chrsrns.microledger.data.Posting

// Pure per-field Posting transforms. Single owner of preserve-sibling,
// collapse, and cascade semantics for form posting edits (V331-V333,
// V349, V352-V353, V361-V362). No Compose imports — JVM-testable.

fun Posting.withQuantity(quantity: String): Posting {
    val currency = amount?.currency ?: ""
    val original = amount?.original ?: ""
    return withAmount(
        if (quantity != "" || original != "" || currency != "") {
            Amount(quantity, currency, original)
        } else {
            null
        },
    )
}

fun Posting.withCurrency(currency: String): Posting {
    val quantity = amount?.quantity ?: ""
    val original = amount?.original ?: ""
    return withAmount(
        if (quantity != "" || original != "" || currency != "") {
            Amount(quantity, currency, original)
        } else {
            null
        },
    )
}

fun Posting.withCostType(type: CostType): Posting =
    withCost(Cost(Amount(cost?.amount?.quantity ?: "", cost?.amount?.currency ?: "", ""), type))

fun Posting.withCostQuantity(quantity: String): Posting =
    withCost(Cost(Amount(quantity, cost?.amount?.currency ?: "", ""), cost?.type ?: CostType.UNIT))

fun Posting.withCostCurrency(currency: String): Posting =
    withCost(Cost(Amount(cost?.amount?.quantity ?: "", currency, ""), cost?.type ?: CostType.UNIT))

fun Posting.withAssertionQuantity(quantity: String): Posting =
    withAssertion(Amount(quantity, assertion?.currency ?: "", ""))

fun Posting.withAssertionCurrency(currency: String): Posting =
    withAssertion(Amount(assertion?.quantity ?: "", currency, ""))

fun Posting.withAssertionCostType(type: CostType): Posting =
    withAssertionCost(
        Cost(
            Amount(assertionCost?.amount?.quantity ?: "", assertionCost?.amount?.currency ?: "", ""),
            type,
        ),
    )

fun Posting.withAssertionCostQuantity(quantity: String): Posting =
    withAssertionCost(
        Cost(Amount(quantity, assertionCost?.amount?.currency ?: "", ""), assertionCost?.type ?: CostType.UNIT),
    )

fun Posting.withAssertionCostCurrency(currency: String): Posting =
    withAssertionCost(
        Cost(Amount(assertionCost?.amount?.quantity ?: "", currency, ""), assertionCost?.type ?: CostType.UNIT),
    )

// Presence toggles. `onValue` supplied by caller (V353): helpers never
// read PreferencesDataSource. Account-off cascades (V332).

fun Posting.withAccountEnabled(on: Boolean): Posting {
    val result = withAccount(if (on) "" else null)
    return if (on) {
        result
    } else {
        result
            .withAmount(null)
            .withCost(null)
            .withAssertion(null)
            .withAssertionCost(null)
    }
}

fun Posting.withAmountEnabled(
    on: Boolean,
    onValue: Amount,
): Posting = withAmount(if (on) onValue else null)

fun Posting.withCostEnabled(
    on: Boolean,
    onValue: Cost,
): Posting = withCost(if (on) onValue else null)

fun Posting.withAssertionEnabled(
    on: Boolean,
    onValue: Amount,
): Posting = withAssertion(if (on) onValue else null)

fun Posting.withAssertionCostEnabled(
    on: Boolean,
    onValue: Cost,
): Posting = withAssertionCost(if (on) onValue else null)

fun Posting.withCommentEnabled(on: Boolean): Posting = withComment(if (on) "" else null)

// Posting-list ops. The stored `postings` list always ends with one
// sentinel (V124); ops index the stored list including the sentinel
// slot (V361). All out-of-range indices are no-ops (V349).

fun withSentinel(
    postings: List<Posting>,
    sentinel: () -> Posting,
): List<Posting> = postings + sentinel()

fun realPostings(postings: List<Posting>): List<Posting> = postings.dropLast(1)

fun updatePostingAt(
    postings: List<Posting>,
    index: Int,
    sentinel: () -> Posting,
    transform: (Posting) -> Posting,
): List<Posting> {
    if (index !in postings.indices) return postings
    val reals = realPostings(postings)
    val newReals =
        if (index == postings.lastIndex) {
            reals + transform(postings[index])
        } else {
            reals.mapIndexed { i, p -> if (i == index) transform(p) else p }
        }
    return newReals + sentinel()
}

fun insertPosting(
    postings: List<Posting>,
    posting: Posting,
    sentinel: () -> Posting,
): List<Posting> = realPostings(postings) + posting + sentinel()

fun removePostingAt(
    postings: List<Posting>,
    index: Int,
    sentinel: () -> Posting,
): List<Posting> {
    if (index !in postings.indices || index == postings.lastIndex) return postings
    return realPostings(postings).filterIndexed { i, _ -> i != index } + sentinel()
}
