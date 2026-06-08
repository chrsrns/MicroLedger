package be.chvp.nanoledger.ui.accounttransactions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import be.chvp.nanoledger.R
import be.chvp.nanoledger.data.Amount
import be.chvp.nanoledger.data.Posting
import be.chvp.nanoledger.data.Transaction
import be.chvp.nanoledger.data.reporting.AccountBalanceCalculator
import be.chvp.nanoledger.ui.main.TransactionCard
import be.chvp.nanoledger.ui.theme.NanoLedgerTheme
import be.chvp.nanoledger.ui.util.amountColor
import be.chvp.nanoledger.ui.util.formatAmount
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal

@AndroidEntryPoint
class AccountTransactionsActivity : ComponentActivity() {
    private val accountTransactionsViewModel: AccountTransactionsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NanoLedgerTheme {
                AccountTransactionsScreen(
                    onBackClick = { finish() },
                )
            }
        }
    }
}

@Composable
fun AccountTransactionsScreen(
    accountTransactionsViewModel: AccountTransactionsViewModel = viewModel(),
    onBackClick: () -> Unit,
    expandedGroups: Set<String> = emptySet(),
) {
    val accountBalances by accountTransactionsViewModel.accountBalances.observeAsState()
    val accountTransactions by accountTransactionsViewModel.accountTransactions.observeAsState()
    val selectedAccount by accountTransactionsViewModel.selectedAccount.observeAsState()
    val decimalSeparator by accountTransactionsViewModel.decimalSeparator.observeAsState(".")

    AccountTransactionsScreenContent(
        accountBalances = accountBalances,
        accountTransactions = accountTransactions,
        selectedAccount = selectedAccount,
        decimalSeparator = decimalSeparator,
        onBackClick = onBackClick,
        onAccountClick = accountTransactionsViewModel::selectAccount,
        onClearSelection = accountTransactionsViewModel::clearSelectedAccount,
        expandedGroups = expandedGroups,
    )
}

@Composable
fun AccountTransactionsScreenContent(
    accountBalances: AccountBalanceCalculator.AccountBalancesResult?,
    accountTransactions: List<Transaction>?,
    selectedAccount: String?,
    decimalSeparator: String,
    onBackClick: () -> Unit,
    onAccountClick: (String, String) -> Unit,
    onClearSelection: () -> Unit,
    expandedGroups: Set<String> = emptySet(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = if (selectedAccount != null) onClearSelection else onBackClick,
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(
                                if (selectedAccount != null) R.string.back else R.string.back
                            ),
                        )
                    }
                },
                title = {
                    Text(
                        selectedAccount ?: stringResource(R.string.account_balances)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        modifier = Modifier.imePadding(),
    ) { contentPadding ->
        if (selectedAccount != null) {
            // Show transactions for selected account
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                if (accountTransactions.isNullOrEmpty()) {
                    Text(
                        stringResource(R.string.no_transactions_for_account),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalContentColor.current.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        accountTransactions.forEach { transaction ->
                            TransactionCard(
                                transaction = transaction,
                                selected = false,
                                onClick = {},
                            )
                        }
                    }
                }
            }
        } else {
            // Show account accordion list
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (accountBalances == null) {
                    Text(
                        stringResource(R.string.no_data),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalContentColor.current.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    val assetsTitle = stringResource(R.string.assets)
                    AccountAccordionGroup(
                        title = assetsTitle,
                        accounts = accountBalances.assets,
                        onAccountClick = onAccountClick,
                        decimalSeparator = decimalSeparator,
                        initiallyExpanded = assetsTitle in expandedGroups,
                    )
                    AccountAccordionGroup(
                        title = stringResource(R.string.liabilities),
                        accounts = accountBalances.liabilities,
                        onAccountClick = onAccountClick,
                        decimalSeparator = decimalSeparator,
                        negate = true,
                    )
                    AccountAccordionGroup(
                        title = stringResource(R.string.equity),
                        accounts = accountBalances.equity,
                        onAccountClick = onAccountClick,
                        decimalSeparator = decimalSeparator,
                        negate = true,
                    )
                    AccountAccordionGroup(
                        title = stringResource(R.string.income),
                        accounts = accountBalances.income,
                        onAccountClick = onAccountClick,
                        decimalSeparator = decimalSeparator,
                        negate = true,
                    )
                    AccountAccordionGroup(
                        title = stringResource(R.string.expenses),
                        accounts = accountBalances.expenses,
                        onAccountClick = onAccountClick,
                        decimalSeparator = decimalSeparator,
                    )
                }
            }
        }
    }
}

@Composable
fun AccountAccordionGroup(
    title: String,
    accounts: List<AccountBalanceCalculator.AccountBalance>,
    onAccountClick: (String, String) -> Unit,
    decimalSeparator: String,
    negate: Boolean = false,
    initiallyExpanded: Boolean = false,
) {
    if (accounts.isEmpty()) return

    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron_rotation",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotationAngle),
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    HorizontalDivider()
                    accounts.forEach { account ->
                        AccountRow(
                            account = account,
                            onAccountClick = onAccountClick,
                            decimalSeparator = decimalSeparator,
                            negate = negate,
                        )
                        if (account != accounts.last()) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountRow(
    account: AccountBalanceCalculator.AccountBalance,
    onAccountClick: (String, String) -> Unit,
    decimalSeparator: String,
    negate: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAccountClick(account.account, account.currency) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            account.account,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (account.currency.isNotBlank()) {
                Text(
                    account.currency,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.6f),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                formatAmount(
                    if (negate) account.balance.negate() else account.balance,
                    decimalSeparator
                ),
                modifier = Modifier.widthIn(min = 80.dp),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = amountColor(if (negate) account.balance.negate() else account.balance),
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun AccountTransactionsScreenPreview() {
    NanoLedgerTheme {
        AccountTransactionsScreenContent(
            accountBalances = AccountBalanceCalculator.AccountBalancesResult(
                assets = listOf(
                    AccountBalanceCalculator.AccountBalance(
                        "Assets:Checking",
                        BigDecimal("3000.00"),
                        "$",
                        emptyList(),
                    ),
                    AccountBalanceCalculator.AccountBalance(
                        "Assets:Savings",
                        BigDecimal("7000.00"),
                        "$",
                        emptyList(),
                    ),
                ),
                liabilities = listOf(
                    AccountBalanceCalculator.AccountBalance(
                        "Liabilities:Credit Card",
                        BigDecimal("1550.00"),
                        "$",
                        emptyList(),
                    ),
                ),
                equity = emptyList(),
                income = emptyList(),
                expenses = emptyList(),
            ),
            accountTransactions = null,
            selectedAccount = null,
            decimalSeparator = ".",
            onBackClick = {},
            onAccountClick = { _, _ -> },
            onClearSelection = {},
            expandedGroups = setOf("Assets"),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AccountAccordionGroupPreview() {
    NanoLedgerTheme {
        AccountAccordionGroup(
            title = "Assets",
            accounts = listOf(
                AccountBalanceCalculator.AccountBalance(
                    "Assets:Checking",
                    BigDecimal("3000.00"),
                    "$",
                    emptyList(),
                ),
                AccountBalanceCalculator.AccountBalance(
                    "Assets:Savings",
                    BigDecimal("7000.00"),
                    "$",
                    emptyList(),
                ),
            ),
            onAccountClick = { _, _ -> },
            decimalSeparator = ".",
            initiallyExpanded = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AccountTransactionsScreenSelectedPreview() {
    NanoLedgerTheme {
        AccountTransactionsScreenContent(
            accountBalances = null,
            accountTransactions = listOf(
                Transaction(
                    firstLine = 0,
                    lastLine = 2,
                    date = "2023-09-08",
                    status = "*",
                    code = null,
                    payee = "Shop",
                    note = "Groceries",
                    postings = listOf(
                        Posting(
                            account = "Assets:Checking",
                            amount = Amount("-2.19", "EUR", "-2.19 EUR"),
                            cost = null,
                            assertion = null,
                            assertionCost = null,
                            comment = null
                        ),
                        Posting(
                            account = "Expenses:Groceries",
                            amount = Amount("2.19", "EUR", "2.19 EUR"),
                            cost = null,
                            assertion = null,
                            assertionCost = null,
                            comment = null
                        )
                    )
                ),
                Transaction(
                    firstLine = 3,
                    lastLine = 5,
                    date = "2023-09-09",
                    status = "*",
                    code = null,
                    payee = "Bakery",
                    note = "Bread",
                    postings = listOf(
                        Posting(
                            account = "Assets:Checking",
                            amount = Amount("-3.50", "EUR", "-3.50 EUR"),
                            cost = null,
                            assertion = null,
                            assertionCost = null,
                            comment = null
                        ),
                        Posting(
                            account = "Expenses:Groceries",
                            amount = Amount("3.50", "EUR", "3.50 EUR"),
                            cost = null,
                            assertion = null,
                            assertionCost = null,
                            comment = null
                        )
                    )
                ),
                Transaction(
                    firstLine = 6,
                    lastLine = 8,
                    date = "2023-09-10",
                    status = "*",
                    code = null,
                    payee = "Supermarket",
                    note = "Weekly shop",
                    postings = listOf(
                        Posting(
                            account = "Assets:Checking",
                            amount = Amount("-45.20", "EUR", "-45.20 EUR"),
                            cost = null,
                            assertion = null,
                            assertionCost = null,
                            comment = null
                        ),
                        Posting(
                            account = "Expenses:Groceries",
                            amount = Amount("45.20", "EUR", "45.20 EUR"),
                            cost = null,
                            assertion = null,
                            assertionCost = null,
                            comment = null
                        )
                    )
                ),
                Transaction(
                    firstLine = 9,
                    lastLine = 11,
                    date = "2023-09-11",
                    status = "*",
                    code = null,
                    payee = "Farmer's Market",
                    note = "Vegetables",
                    postings = listOf(
                        Posting(
                            account = "Assets:Checking",
                            amount = Amount("-12.00", "EUR", "-12.00 EUR"),
                            cost = null,
                            assertion = null,
                            assertionCost = null,
                            comment = null
                        ),
                        Posting(
                            account = "Expenses:Groceries",
                            amount = Amount("12.00", "EUR", "12.00 EUR"),
                            cost = null,
                            assertion = null,
                            assertionCost = null,
                            comment = null
                        )
                    )
                )
            ),
            selectedAccount = "Expenses:Groceries",
            decimalSeparator = ".",
            onBackClick = {},
            onAccountClick = { _, _ -> },
            onClearSelection = {},
        )
    }
}
