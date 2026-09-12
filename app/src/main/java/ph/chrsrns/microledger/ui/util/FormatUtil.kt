package ph.chrsrns.microledger.ui.util

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

fun formatAmount(
    amount: BigDecimal,
    decimalSeparator: String,
): String {
    val separator = decimalSeparator.firstOrNull() ?: '.'
    val symbols =
        DecimalFormatSymbols(Locale.US).apply {
            this.decimalSeparator = separator
            groupingSeparator = if (separator == ',') '.' else ','
        }
    return DecimalFormat("#,##0.00", symbols).format(amount)
}

@Composable
fun amountColor(amount: BigDecimal): Color =
    when {
        amount > BigDecimal.ZERO -> MaterialTheme.colorScheme.primary
        amount < BigDecimal.ZERO -> MaterialTheme.colorScheme.error
        else -> LocalContentColor.current
    }

enum class Sign {
    POSITIVE,
    NEGATIVE,
    ZERO,
    BLANK,
    UNPARSEABLE,
}

fun postingAmountSign(
    quantity: String,
    decimalSeparator: String,
): Sign {
    if (quantity.isBlank()) return Sign.BLANK
    val separator = decimalSeparator.firstOrNull() ?: '.'
    val cleaned =
        quantity
            .filter { it == '-' || it.isDigit() || it == separator }
            .replace(separator, '.')
    if (cleaned.isEmpty()) return Sign.UNPARSEABLE
    return try {
        val value = BigDecimal(cleaned)
        when {
            value > BigDecimal.ZERO -> Sign.POSITIVE
            value < BigDecimal.ZERO -> Sign.NEGATIVE
            else -> Sign.ZERO
        }
    } catch (e: NumberFormatException) {
        Sign.UNPARSEABLE
    }
}
