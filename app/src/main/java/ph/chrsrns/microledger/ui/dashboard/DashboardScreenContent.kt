package ph.chrsrns.microledger.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ph.chrsrns.microledger.R
import ph.chrsrns.microledger.data.AccountType
import ph.chrsrns.microledger.data.displaySign
import ph.chrsrns.microledger.data.reporting.AccountBalanceCalculator
import ph.chrsrns.microledger.data.reporting.MonthlyCashFlowCalculator
import ph.chrsrns.microledger.data.reporting.NetWorthCalculator
import ph.chrsrns.microledger.ui.util.amountColor
import ph.chrsrns.microledger.ui.util.formatAmount
import java.math.BigDecimal

@Composable
fun DashboardScreenContent(
    netWorth: List<NetWorthCalculator.CurrencyNetWorth>?,
    accountBalances: AccountBalanceCalculator.AccountBalancesResult?,
    cashFlow: MonthlyCashFlowCalculator.CashFlowResult?,
    decimalSeparator: String,
    onBackClick: () -> Unit,
    onAccountClick: () -> Unit,
    onCashFlowClick: () -> Unit = {},
    showTopBar: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
    hasFile: Boolean = true,
) {
    val content: @Composable (PaddingValues) -> Unit = { contentPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .alpha(if (hasFile) 1f else 0.38f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NetWorthCard(netWorth, decimalSeparator)
            CashFlowCard(
                cashFlow,
                decimalSeparator,
                onClick = if (hasFile) onCashFlowClick else ({}),
            )
            AccountBalancesCard(
                accountBalances,
                decimalSeparator,
                onClick = if (hasFile) onAccountClick else ({}),
            )
        }
    }

    if (showTopBar) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    title = { Text(stringResource(R.string.dashboard)) },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                )
            },
            modifier = Modifier.imePadding(),
        ) { contentPadding ->
            content(contentPadding)
        }
    } else {
        content(contentPadding)
    }
}

@Composable
fun NetWorthCard(
    netWorth: List<NetWorthCalculator.CurrencyNetWorth>?,
    decimalSeparator: String,
) {
    DashboardCard(title = stringResource(R.string.net_worth)) {
        if (netWorth.isNullOrEmpty()) {
            NoDataText()
        } else if (netWorth.size == 1) {
            val single = netWorth.single()
            Text(
                formatAmount(single.netWorth, decimalSeparator),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor(single.netWorth),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            AmountRow(
                label = stringResource(R.string.total_assets),
                amount = single.totalAssets,
                decimalSeparator = decimalSeparator,
            )
            AmountRow(
                label = stringResource(R.string.total_liabilities),
                amount = single.totalLiabilities.multiply(BigDecimal(displaySign(AccountType.LIABILITIES))),
                decimalSeparator = decimalSeparator,
            )
        } else {
            netWorth.forEach { entry ->
                AmountRow(
                    label = entry.currency.ifBlank { "—" },
                    amount = entry.netWorth,
                    decimalSeparator = decimalSeparator,
                    bold = true,
                )
            }
        }
    }
}

@Composable
fun CashFlowCard(
    cashFlow: MonthlyCashFlowCalculator.CashFlowResult?,
    decimalSeparator: String,
    onClick: () -> Unit = {},
) {
    DashboardCard(
        title = stringResource(R.string.cash_flow_this_month),
        subtitle = cashFlow?.period,
        onClick = onClick,
    ) {
        if (cashFlow == null) {
            NoDataText()
        } else {
            AmountRow(
                label = stringResource(R.string.income),
                amount = cashFlow.totalIncome,
                decimalSeparator = decimalSeparator,
            )
            AmountRow(
                label = stringResource(R.string.expenses),
                amount = cashFlow.totalExpenses.multiply(BigDecimal(displaySign(AccountType.EXPENSES))),
                decimalSeparator = decimalSeparator,
            )
            Spacer(Modifier.height(4.dp))
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))
            AmountRow(
                label = stringResource(R.string.net_flow),
                amount = cashFlow.netFlow,
                decimalSeparator = decimalSeparator,
                bold = true,
            )
        }
    }
}

@Composable
fun AccountBalancesCard(
    accountBalances: AccountBalanceCalculator.AccountBalancesResult?,
    decimalSeparator: String,
    onClick: () -> Unit,
) {
    DashboardCard(
        title = stringResource(R.string.account_balances),
        onClick = onClick,
    ) {
        if (accountBalances == null || (accountBalances.assets.isEmpty() && accountBalances.liabilities.isEmpty())) {
            NoDataText()
        } else {
            if (accountBalances.assets.isNotEmpty()) {
                AccountGroupHeader(stringResource(R.string.assets))
                accountBalances.assets.forEach { balance ->
                    AmountRow(
                        label = balance.account,
                        amount = balance.balance,
                        currency = balance.currency,
                        decimalSeparator = decimalSeparator,
                        labelStyle = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (accountBalances.liabilities.isNotEmpty()) {
                if (accountBalances.assets.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                }
                AccountGroupHeader(stringResource(R.string.liabilities))
                accountBalances.liabilities.forEach { balance ->
                    AmountRow(
                        label = balance.account,
                        amount = balance.balance.multiply(BigDecimal(displaySign(AccountType.LIABILITIES))),
                        currency = balance.currency,
                        decimalSeparator = decimalSeparator,
                        labelStyle = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable { onClick() }
                    } else {
                        Modifier
                    },
                ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.6f),
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun AccountGroupHeader(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 2.dp),
    )
}

@Composable
fun AmountRow(
    label: String,
    amount: BigDecimal,
    decimalSeparator: String,
    bold: Boolean = false,
    labelStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    currency: String? = null,
) {
    val displayAmount = amount
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
    ) {
        Text(
            label,
            style = labelStyle,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!currency.isNullOrBlank()) {
                Text(
                    currency,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.6f),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                formatAmount(displayAmount, decimalSeparator),
                modifier = Modifier.widthIn(min = 80.dp),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
                color = amountColor(displayAmount),
            )
        }
    }
}

@Composable
fun NoDataText() {
    Text(
        stringResource(R.string.no_data),
        style = MaterialTheme.typography.bodyMedium,
        color = LocalContentColor.current.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
}
