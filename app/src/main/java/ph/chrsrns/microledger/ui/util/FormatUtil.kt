package ph.chrsrns.microledger.ui.util

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import ph.chrsrns.microledger.R
import ph.chrsrns.microledger.data.Amount
import ph.chrsrns.microledger.ui.theme.AccountAssetsColorDark
import ph.chrsrns.microledger.ui.theme.AccountAssetsColorLight
import ph.chrsrns.microledger.ui.theme.AccountEquityColorDark
import ph.chrsrns.microledger.ui.theme.AccountEquityColorLight
import ph.chrsrns.microledger.ui.theme.AccountExpensesColorDark
import ph.chrsrns.microledger.ui.theme.AccountExpensesColorLight
import ph.chrsrns.microledger.ui.theme.AccountIncomeColorDark
import ph.chrsrns.microledger.ui.theme.AccountIncomeColorLight
import ph.chrsrns.microledger.ui.theme.AccountLiabilitiesColorDark
import ph.chrsrns.microledger.ui.theme.AccountLiabilitiesColorLight
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

@Composable
fun postingAmountColor(
    amount: Amount?,
    decimalSeparator: String,
): Color? =
    when (postingAmountSign(amount?.quantity ?: "", decimalSeparator)) {
        Sign.POSITIVE -> MaterialTheme.colorScheme.primary
        Sign.NEGATIVE -> MaterialTheme.colorScheme.error
        Sign.ZERO,
        Sign.BLANK,
        Sign.UNPARSEABLE,
        -> null
    }

@Composable
fun statusChipLabel(status: String?): String? {
    val s = status?.trim() ?: return null
    if (s.isBlank()) return null
    return when (s) {
        "*" -> stringResource(R.string.status_cleared_chip)
        "!" -> stringResource(R.string.status_pending_chip)
        else -> null
    }
}

@Composable
fun accountTypeColor(
    account: String?,
    assets: List<String>,
    liabilities: List<String>,
    equity: List<String>,
    income: List<String>,
    expenses: List<String>,
): Color? {
    val accountName = account?.trim() ?: return null
    if (accountName.isBlank()) return null
    val isDark = isSystemInDarkTheme()

    fun matches(prefixes: List<String>) = prefixes.any { accountName.startsWith(it, ignoreCase = true) }
    return when {
        matches(assets) -> if (isDark) AccountAssetsColorDark else AccountAssetsColorLight
        matches(liabilities) -> if (isDark) AccountLiabilitiesColorDark else AccountLiabilitiesColorLight
        matches(equity) -> if (isDark) AccountEquityColorDark else AccountEquityColorLight
        matches(income) -> if (isDark) AccountIncomeColorDark else AccountIncomeColorLight
        matches(expenses) -> if (isDark) AccountExpensesColorDark else AccountExpensesColorLight
        else -> null
    }
}
