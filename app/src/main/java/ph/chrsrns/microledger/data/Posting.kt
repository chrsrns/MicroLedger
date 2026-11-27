package ph.chrsrns.microledger.data

data class Posting(
    val account: String?,
    val amount: Amount?,
    val cost: Cost?,
    val assertion: Amount?,
    val assertionCost: Cost?,
    val comment: String?,
) {
    fun contains(query: String): Boolean {
        if (account?.contains(query, ignoreCase = true) ?: false) {
            return true
        }
        if (comment?.contains(query, ignoreCase = true) ?: false) {
            return true
        }
        if (amount?.contains(query) ?: false) {
            return true
        }
        if (cost?.contains(query) ?: false) {
            return true
        }
        if (assertion?.contains(query) ?: false) {
            return true
        }
        if (assertionCost?.contains(query) ?: false) {
            return true
        }

        return false
    }

    fun isComment() = comment != null && listOf(account, amount, cost, assertion, assertionCost).all { it == null }

    fun fullAmountDisplayString(): String {
        var result = ""
        result += amount?.original ?: ""
        if (cost != null) {
            result += " ${cost.type.repr} ${cost.amount.original}"
        }
        if (assertion != null) {
            result += " = ${assertion.original}"
        }
        if (assertionCost != null) {
            result += " ${assertionCost.type.repr} ${assertionCost.amount.original}"
        }
        if (comment != null) {
            result += "  ; $comment"
        }
        return result.trim()
    }

    fun format(
        width: Int,
        currencyBeforeAmount: Boolean,
        currencyAmountSpacing: Boolean,
        currencyEnabled: Boolean,
    ) = formatCore(width, currencyBeforeAmount, currencyAmountSpacing, currencyEnabled, lenient = false)

    /**
     * Formats this posting with support for currency-only amounts (no quantity).
     *
     * Unlike [format], this lenient version will output amounts that have a
     * currency but no quantity. This is useful for template postings where
     * only the currency is specified (e.g., "Expenses:Misc  EUR").
     *
     * Uses [Amount.formatLenient] for amount and assertion formatting to
     * preserve currency-only values during serialization.
     *
     * @param width The desired width for alignment (minimum 2 spaces between account and amount)
     * @param currencyBeforeAmount If true, currency appears before quantity
     * @param currencyAmountSpacing If true, adds a space between currency and quantity
     * @param currencyEnabled If false, returns only quantities (or empty amounts)
     * @return Formatted posting line with 4-space indentation
     */
    fun formatLenient(
        width: Int,
        currencyBeforeAmount: Boolean,
        currencyAmountSpacing: Boolean,
        currencyEnabled: Boolean,
    ) = formatCore(width, currencyBeforeAmount, currencyAmountSpacing, currencyEnabled, lenient = true)

    private fun formatCore(
        width: Int,
        currencyBeforeAmount: Boolean,
        currencyAmountSpacing: Boolean,
        currencyEnabled: Boolean,
        lenient: Boolean,
    ): String {
        fun include(amount: Amount?) = (amount?.quantity ?: "") != "" || (lenient && (amount?.currency ?: "") != "")

        val formatAmount: (Amount) -> String =
            if (lenient) {
                { it.formatLenient(currencyBeforeAmount, currencyAmountSpacing, currencyEnabled) }
            } else {
                { it.format(currencyBeforeAmount, currencyAmountSpacing, currencyEnabled) }
            }

        var fullAmountString = ""
        if (include(amount)) {
            fullAmountString += formatAmount(amount!!)
        }
        if (include(cost?.amount)) {
            fullAmountString += ' ' +
                cost!!.format(
                    currencyBeforeAmount,
                    currencyAmountSpacing,
                    currencyEnabled,
                )
        }
        if (include(assertion)) {
            fullAmountString += " = " + formatAmount(assertion!!)
        }
        if (include(assertionCost?.amount)) {
            fullAmountString += ' ' +
                assertionCost!!.format(
                    currencyBeforeAmount,
                    currencyAmountSpacing,
                    currencyEnabled,
                )
        }
        fullAmountString = fullAmountString.trim()
        val fillWidth =
            (width - fullAmountString.length - (account ?: "").length - 4).coerceAtLeast(2)
        val spaces = " ".repeat(fillWidth)
        if (isComment()) {
            return "    ; $comment"
        }
        var result = "    ${account ?: ""}$spaces$fullAmountString"
        if ((comment ?: "") != "") {
            result += "  ; $comment"
        }
        return result.trimEnd()
    }

    fun isEmpty() =
        listOf(
            account,
            amount?.quantity,
            cost?.amount?.quantity,
            assertion?.quantity,
            assertionCost?.amount?.quantity,
            comment,
        ).all { (it ?: "") == "" }

    fun isVirtual() = account?.let { it.startsWith("(") && it.endsWith(")") } ?: false

    fun isQualifying() = !isVirtual() && !isComment() && !account.isNullOrBlank()

    fun isStub() = !isVirtual() && !isComment() && account.isNullOrBlank()
}
