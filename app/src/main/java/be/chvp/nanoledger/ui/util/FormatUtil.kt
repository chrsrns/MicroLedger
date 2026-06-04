package be.chvp.nanoledger.ui.util

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
