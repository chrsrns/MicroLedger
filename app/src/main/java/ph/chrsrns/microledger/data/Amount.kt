package ph.chrsrns.microledger.data

data class Amount(
    val quantity: String,
    val currency: String,
    val original: String,
) {
    fun contains(query: String) = original.contains(query, ignoreCase = true)

    fun format(
        currencyBeforeAmount: Boolean,
        currencyAmountSpacing: Boolean,
        currencyEnabled: Boolean,
    ): String {
        if (!currencyEnabled) {
            return quantity.trim()
        }
        val spacer = if (currencyAmountSpacing) " " else ""
        val result = if (currencyBeforeAmount) "$currency$spacer$quantity" else "$quantity$spacer$currency"
        return result.trim()
    }

    /**
     * Formats this amount with support for currency-only amounts (no quantity).
     *
     * Unlike [format], this lenient version will output just the currency when
     * quantity is empty but currency is present. This is useful for template
     * postings where only the currency is specified without an amount.
     *
     * @param currencyBeforeAmount If true, currency appears before quantity
     * @param currencyAmountSpacing If true, adds a space between currency and quantity
     * @param currencyEnabled If false, returns only the quantity (or empty if no quantity)
     * @return Formatted amount string, or just the currency if quantity is empty
     */
    fun formatLenient(
        currencyBeforeAmount: Boolean,
        currencyAmountSpacing: Boolean,
        currencyEnabled: Boolean,
    ): String {
        if (!currencyEnabled) {
            return quantity.trim()
        }
        // If quantity is empty but currency is present, return just the currency
        if (quantity.isEmpty() && currency.isNotEmpty()) {
            return currency
        }
        val spacer = if (currencyAmountSpacing) " " else ""
        val result =
            if (currencyBeforeAmount) "$currency$spacer$quantity" else "$quantity$spacer$currency"
        return result.trim()
    }
}
