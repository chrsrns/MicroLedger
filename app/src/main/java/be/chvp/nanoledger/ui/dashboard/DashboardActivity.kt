package be.chvp.nanoledger.ui.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import be.chvp.nanoledger.R
import be.chvp.nanoledger.data.reporting.AccountBalanceCalculator
import be.chvp.nanoledger.data.reporting.MonthlyCashFlowCalculator
import be.chvp.nanoledger.data.reporting.NetWorthCalculator
import be.chvp.nanoledger.ui.theme.NanoLedgerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@AndroidEntryPoint
class DashboardActivity : ComponentActivity() {
    private val dashboardViewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NanoLedgerTheme {
                DashboardScreen(
                    onBackClick = { finish() },
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel = viewModel(),
    onBackClick: () -> Unit,
) {
    val netWorth by dashboardViewModel.netWorth.observeAsState()
    val accountBalances by dashboardViewModel.accountBalances.observeAsState()
    val cashFlow by dashboardViewModel.currentMonthCashFlow.observeAsState()
    val decimalSeparator by dashboardViewModel.decimalSeparator.observeAsState(".")

    DashboardScreenContent(
        netWorth = netWorth,
        accountBalances = accountBalances,
        cashFlow = cashFlow,
        decimalSeparator = decimalSeparator,
        onBackClick = onBackClick,
    )
}

@Composable
fun DashboardScreenContent(
    netWorth: NetWorthCalculator.NetWorthResult?,
    accountBalances: AccountBalanceCalculator.AccountBalancesResult?,
    cashFlow: MonthlyCashFlowCalculator.CashFlowResult?,
    decimalSeparator: String,
    onBackClick: () -> Unit,
) {
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        modifier = Modifier.imePadding(),
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NetWorthCard(netWorth, decimalSeparator)
            CashFlowCard(cashFlow, decimalSeparator)
            AccountBalancesCard(accountBalances, decimalSeparator)
        }
    }
}

@Composable
fun NetWorthCard(
    netWorth: NetWorthCalculator.NetWorthResult?,
    decimalSeparator: String,
) {
    DashboardCard(title = stringResource(R.string.net_worth)) {
        if (netWorth == null) {
            NoDataText()
        } else {
            Text(
                formatAmount(netWorth.netWorth, decimalSeparator),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor(netWorth.netWorth),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            AmountRow(
                label = stringResource(R.string.total_assets),
                amount = netWorth.totalAssets,
                decimalSeparator = decimalSeparator,
            )
            AmountRow(
                label = stringResource(R.string.total_liabilities),
                amount = netWorth.totalLiabilities,
                decimalSeparator = decimalSeparator,
                negate = true,
            )
        }
    }
}

@Composable
fun CashFlowCard(
    cashFlow: MonthlyCashFlowCalculator.CashFlowResult?,
    decimalSeparator: String,
) {
    DashboardCard(
        title = stringResource(R.string.cash_flow_this_month),
        subtitle = cashFlow?.period,
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
                amount = cashFlow.totalExpenses,
                decimalSeparator = decimalSeparator,
                negate = true,
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
) {
    DashboardCard(title = stringResource(R.string.account_balances)) {
        if (accountBalances == null || (accountBalances.assets.isEmpty() && accountBalances.liabilities.isEmpty())) {
            NoDataText()
        } else {
            if (accountBalances.assets.isNotEmpty()) {
                AccountGroupHeader(stringResource(R.string.assets))
                accountBalances.assets.forEach { balance ->
                    AmountRow(
                        label = balance.account,
                        amount = balance.balance,
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
                        amount = balance.balance,
                        decimalSeparator = decimalSeparator,
                        negate = true,
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
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
    negate: Boolean = false,
    bold: Boolean = false,
    labelStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val displayAmount = if (negate) amount.negate() else amount
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            label,
            style = labelStyle,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Text(
            formatAmount(displayAmount, decimalSeparator),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            color = amountColor(displayAmount),
        )
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

fun formatAmount(
    amount: BigDecimal,
    decimalSeparator: String,
): String {
    // Honor the user's configured decimal separator (see PreferencesDataSource);
    // pick a sensible grouping separator that doesn't collide with it.
    val separator = decimalSeparator.firstOrNull() ?: '.'
    val symbols =
        DecimalFormatSymbols(Locale.US).apply {
            this.decimalSeparator = separator
            groupingSeparator = if (separator == ',') '.' else ','
        }
    return DecimalFormat("#,##0.00", symbols).format(amount)
}

@Composable
fun amountColor(amount: BigDecimal): Color {
    return when {
        amount > BigDecimal.ZERO -> MaterialTheme.colorScheme.primary
        amount < BigDecimal.ZERO -> MaterialTheme.colorScheme.error
        else -> LocalContentColor.current
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    NanoLedgerTheme {
        DashboardScreenContent(
            netWorth = NetWorthCalculator.NetWorthResult(
                netWorth = BigDecimal("8450.00"),
                totalAssets = BigDecimal("10000.00"),
                totalLiabilities = BigDecimal("1550.00"),
            ),
            accountBalances = AccountBalanceCalculator.AccountBalancesResult(
                assets = listOf(
                    AccountBalanceCalculator.AccountBalance(
                        "Assets:Checking",
                        BigDecimal("3000.00"),
                        "$"
                    ),
                    AccountBalanceCalculator.AccountBalance(
                        "Assets:Savings",
                        BigDecimal("7000.00"),
                        "$"
                    ),
                ),
                liabilities = listOf(
                    AccountBalanceCalculator.AccountBalance(
                        "Liabilities:Credit Card",
                        BigDecimal("1550.00"),
                        "$"
                    ),
                ),
                equity = emptyList(),
                income = emptyList(),
                expenses = emptyList(),
            ),
            cashFlow = MonthlyCashFlowCalculator.CashFlowResult(
                totalIncome = BigDecimal("5000.00"),
                totalExpenses = BigDecimal("1635.00"),
                netFlow = BigDecimal("3365.00"),
                period = "2026-06",
            ),
            decimalSeparator = ".",
            onBackClick = {},
        )
    }
}
