package be.chvp.nanoledger.data.reporting

import java.math.BigDecimal

/**
 * Parses a posting quantity string into a [BigDecimal], honoring the user's
 * configured decimal separator.
 *
 * Mirrors the parsing recipe used in
 * [be.chvp.nanoledger.ui.common.TransactionFormViewModel]: non-numeric
 * characters (other than a leading minus sign and the decimal separator) are
 * stripped, the separator is normalized to a `.`, and unparseable input falls
 * back to [BigDecimal.ZERO].
 *
 * @param quantity Raw quantity string from an [be.chvp.nanoledger.data.Amount]
 * @param decimalSeparator The user's decimal separator (e.g. "." or ",")
 * @return The parsed quantity, or [BigDecimal.ZERO] if it cannot be parsed
 */
fun parseQuantity(
    quantity: String,
    decimalSeparator: String,
): BigDecimal {
    val cleaned =
        quantity
            .replace(Regex("[^-0-9$decimalSeparator]"), "")
            .replace(decimalSeparator, ".")
    return try {
        BigDecimal(cleaned)
    } catch (e: NumberFormatException) {
        BigDecimal.ZERO
    }
}
