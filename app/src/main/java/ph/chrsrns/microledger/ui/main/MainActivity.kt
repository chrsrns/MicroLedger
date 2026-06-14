package ph.chrsrns.microledger.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import ph.chrsrns.microledger.R
import ph.chrsrns.microledger.data.Amount
import ph.chrsrns.microledger.data.Posting
import ph.chrsrns.microledger.data.Transaction
import ph.chrsrns.microledger.data.TransactionTemplate
import ph.chrsrns.microledger.data.reporting.AccountBalanceCalculator
import ph.chrsrns.microledger.data.reporting.MonthlyCashFlowCalculator
import ph.chrsrns.microledger.data.reporting.NetWorthCalculator
import ph.chrsrns.microledger.ui.accounttransactions.AccountTransactionsActivity
import ph.chrsrns.microledger.ui.add.AddActivity
import ph.chrsrns.microledger.ui.cashflowtransactions.CashFlowTransactionsActivity
import ph.chrsrns.microledger.ui.common.NoFileState
import ph.chrsrns.microledger.ui.common.TRANSACTION_INDEX_KEY
import ph.chrsrns.microledger.ui.dashboard.DashboardScreenContent
import ph.chrsrns.microledger.ui.dashboard.DashboardViewModel
import ph.chrsrns.microledger.ui.edit.EditActivity
import ph.chrsrns.microledger.ui.preferences.PreferencesScreen
import ph.chrsrns.microledger.ui.preferences.PreferencesViewModel
import ph.chrsrns.microledger.ui.templates.EDIT_TEMPLATE_ID_KEY
import ph.chrsrns.microledger.ui.templates.TEMPLATE_ID_KEY
import ph.chrsrns.microledger.ui.templates.TemplateFormActivity
import ph.chrsrns.microledger.ui.templates.TemplatesScreenContent
import ph.chrsrns.microledger.ui.templates.TemplatesViewModel
import ph.chrsrns.microledger.ui.theme.MicroLedgerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal

sealed class TabConfiguration {
    data class Dashboard(
        val netWorth: NetWorthCalculator.NetWorthResult?,
        val accountBalances: AccountBalanceCalculator.AccountBalancesResult?,
        val cashFlow: MonthlyCashFlowCalculator.CashFlowResult?,
        val decimalSeparator: String,
        val hasFile: Boolean,
        val onAccountClick: () -> Unit,
        val onCashFlowClick: () -> Unit,
    ) : TabConfiguration()

    data class Templates(
        val templates: List<TransactionTemplate>,
        val saving: Boolean,
        val hasFile: Boolean,
        val onAddClick: () -> Unit,
        val onTemplateClick: (TransactionTemplate) -> Unit,
        val onEditClick: (TransactionTemplate) -> Unit,
        val onDeleteClick: (String) -> Unit,
        val onGoToSettings: () -> Unit,
    ) : TabConfiguration()

    data class Settings(
        val fileUri: Uri?,
        val onOpenFile: () -> Unit,
        val transactionDefaultElements: List<Int>,
        val transactionStatusPresentByDefault: Boolean,
        val onTransactionStatusPresentByDefaultChange: (Boolean) -> Unit,
        val transactionCodePresentByDefault: Boolean,
        val onTransactionCodePresentByDefaultChange: (Boolean) -> Unit,
        val transactionPayeePresentByDefault: Boolean,
        val onTransactionPayeePresentByDefaultChange: (Boolean) -> Unit,
        val transactionNotePresentByDefault: Boolean,
        val onTransactionNotePresentByDefaultChange: (Boolean) -> Unit,
        val transactionCurrenciesPresentByDefault: Boolean,
        val onTransactionCurrenciesPresentByDefaultChange: (Boolean) -> Unit,
        val postingDefaultElements: List<Int>,
        val postingAmountPresentByDefault: Boolean,
        val onPostingAmountPresentByDefaultChange: (Boolean) -> Unit,
        val postingCostPresentByDefault: Boolean,
        val onPostingCostPresentByDefaultChange: (Boolean) -> Unit,
        val postingAssertionPresentByDefault: Boolean,
        val onPostingAssertionPresentByDefaultChange: (Boolean) -> Unit,
        val postingAssertionCostPresentByDefault: Boolean,
        val onPostingAssertionCostPresentByDefaultChange: (Boolean) -> Unit,
        val postingCommentPresentByDefault: Boolean,
        val onPostingCommentPresentByDefaultChange: (Boolean) -> Unit,
        val defaultCurrency: String,
        val onDefaultCurrencyChange: (String) -> Unit,
        val postingWidth: Int,
        val onPostingWidthChange: (Int) -> Unit,
        val defaultStatus: String,
        val onDefaultStatusChange: (String) -> Unit,
        val decimalSeparator: String,
        val onDecimalSeparatorChange: (String) -> Unit,
        val currencyBeforeAmount: Boolean,
        val onCurrencyBeforeAmountChange: (Boolean) -> Unit,
        val currencyAmountSpacing: Boolean,
        val onCurrencyAmountSpacingChange: (Boolean) -> Unit,
        val assetsPrefixes: List<String>,
        val onAssetsPrefixesChange: (List<String>) -> Unit,
        val liabilitiesPrefixes: List<String>,
        val onLiabilitiesPrefixesChange: (List<String>) -> Unit,
        val equityPrefixes: List<String>,
        val onEquityPrefixesChange: (List<String>) -> Unit,
        val incomePrefixes: List<String>,
        val onIncomePrefixesChange: (List<String>) -> Unit,
        val expensesPrefixes: List<String>,
        val onExpensesPrefixesChange: (List<String>) -> Unit,
    ) : TabConfiguration()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val templatesViewModel: TemplatesViewModel by viewModels()
    private val preferencesViewModel: PreferencesViewModel by viewModels()

    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            mainViewModel.setFileUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current

            val latestReadError by mainViewModel.latestReadError.observeAsState()
            val readErrorMessage = stringResource(R.string.error_reading_file)
            LaunchedEffect(latestReadError) {
                val error = latestReadError?.get()
                if (error != null) {
                    Log.e("ph.chrsrns.microledger", "Exception while reading file", error)
                    Toast
                        .makeText(
                            context,
                            readErrorMessage,
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }

            val latestWriteError by mainViewModel.latestWriteError.observeAsState()
            val writeErrorMessage = stringResource(R.string.error_writing_file)
            LaunchedEffect(latestWriteError) {
                val error = latestWriteError?.get()
                if (error != null) {
                    Log.e("ph.chrsrns.microledger", "Exception while writing file", error)
                    Toast
                        .makeText(
                            context,
                            writeErrorMessage,
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }

            val latestMismatch by mainViewModel.latestMismatch.observeAsState()
            val mismatchMessage = stringResource(R.string.mismatch_no_delete)
            LaunchedEffect(latestMismatch) {
                val error = latestMismatch?.get()
                if (error != null) {
                    Toast
                        .makeText(
                            context,
                            mismatchMessage,
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }

            val fileUri by mainViewModel.fileUri.observeAsState()
            val isRefreshing by mainViewModel.isRefreshing.observeAsState(false)
            var hasRefreshed by remember { mutableStateOf(false) }
            LaunchedEffect(fileUri) {
                mainViewModel.refresh()
            }

            // Preload tab ViewModels after main ViewModel finishes loading
            LaunchedEffect(isRefreshing) {
                if (!isRefreshing && fileUri != null) {
                    hasRefreshed = true
                }
                if (!isRefreshing && hasRefreshed) {
                    // Access ViewModels to trigger their initialization
                    dashboardViewModel.netWorth
                    templatesViewModel.templates
                    preferencesViewModel.fileUri
                }
            }

            MicroLedgerTheme {
                MainScreen(
                    onAddClick = {
                        startActivity(Intent(this, AddActivity::class.java))
                    },
                    onCopyClick = { index ->
                        val intent = Intent(this, AddActivity::class.java)
                        intent.putExtra(TRANSACTION_INDEX_KEY, index)
                        mainViewModel.toggleSelect(index)
                        startActivity(intent)
                    },
                    onEditClick = { index ->
                        val intent = Intent(this, EditActivity::class.java)
                        intent.putExtra(TRANSACTION_INDEX_KEY, index)
                        mainViewModel.toggleSelect(index)
                        startActivity(intent)
                    },
                    onOpenFile = { openFileLauncher.launch(arrayOf("*/*")) },
                    mainViewModel = mainViewModel,
                    dashboardViewModel = dashboardViewModel,
                    templatesViewModel = templatesViewModel,
                    preferencesViewModel = preferencesViewModel,
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    mainViewModel: MainViewModel = viewModel(),
    dashboardViewModel: DashboardViewModel,
    templatesViewModel: TemplatesViewModel,
    preferencesViewModel: PreferencesViewModel,
    onAddClick: () -> Unit,
    onCopyClick: (Int) -> Unit,
    onEditClick: (Int) -> Unit,
    onOpenFile: () -> Unit,
) {
    val context = LocalContext.current
    val fileUri by mainViewModel.fileUri.observeAsState()
    val transactions by mainViewModel.filteredTransactions.observeAsState()
    val searching by mainViewModel.searching.observeAsState()
    val query by mainViewModel.query.observeAsState()
    val isRefreshing by mainViewModel.isRefreshing.observeAsState()
    val selected by mainViewModel.selectedIndex.observeAsState()
    val selectedTab by mainViewModel.selectedTab.observeAsState()

    // Handle back button when in search mode
    BackHandler(enabled = (searching ?: false) && selectedTab == MainTab.Home) {
        mainViewModel.setSearching(false)
        mainViewModel.setQuery("")
    }

    MainScreen(
        fileUri = fileUri,
        transactions = transactions,
        searching = searching ?: false,
        query = query ?: "",
        isRefreshing = isRefreshing ?: false,
        selected = selected,
        selectedTab = selectedTab ?: MainTab.Home,
        onRefresh = { mainViewModel.refresh() },
        onToggleSelect = { mainViewModel.toggleSelect(it) },
        onSearchClick = { mainViewModel.setSearching(true) },
        onSelectTab = { mainViewModel.selectTab(it) },
        onStopSearching = {
            mainViewModel.setSearching(false)
            mainViewModel.setQuery("")
        },
        onQueryChange = { mainViewModel.setQuery(it) },
        onStopSelection = { selected?.let { mainViewModel.toggleSelect(it) } },
        onCopyClick = { selected?.let { onCopyClick(it) } },
        onEditClick = { selected?.let { onEditClick(it) } },
        onDeleteClick = { mainViewModel.deleteSelected() },
        onAddClick = onAddClick,
        onDashboardAccountClick = {
            context.startActivity(Intent(context, AccountTransactionsActivity::class.java))
        },
        onCashFlowClick = {
            context.startActivity(Intent(context, CashFlowTransactionsActivity::class.java))
        },
        onOpenFile = onOpenFile,
        onTemplateAddClick = {
            context.startActivity(Intent(context, TemplateFormActivity::class.java))
        },
        dashboardViewModel = dashboardViewModel,
        templatesViewModel = templatesViewModel,
        preferencesViewModel = preferencesViewModel,
    )
}

@Composable
fun MainScreen(
    fileUri: Uri?,
    transactions: List<Pair<Int, Transaction>>?,
    searching: Boolean,
    query: String,
    isRefreshing: Boolean,
    selected: Int?,
    selectedTab: MainTab,
    onRefresh: () -> Unit,
    onToggleSelect: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onSelectTab: (MainTab) -> Unit,
    onStopSearching: () -> Unit,
    onQueryChange: (String) -> Unit,
    onStopSelection: () -> Unit,
    onCopyClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddClick: () -> Unit,
    onDashboardAccountClick: () -> Unit,
    onCashFlowClick: () -> Unit,
    onOpenFile: () -> Unit,
    onTemplateAddClick: (() -> Unit)? = null,
    dashboardViewModel: DashboardViewModel? = null,
    templatesViewModel: TemplatesViewModel? = null,
    preferencesViewModel: PreferencesViewModel? = null,
    tabContent: (@Composable (MainTab, PaddingValues) -> Unit)? = null,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            if (selected != null && selectedTab == MainTab.Home) {
                SelectionBar(onStopSelection, onCopyClick, onEditClick, onDeleteClick)
            } else if (searching && selectedTab == MainTab.Home) {
                SearchBar(query, onQueryChange, onStopSearching)
            } else {
                MainBar(selectedTab, onSearchClick)
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == MainTab.Home,
                    onClick = { onSelectTab(MainTab.Home) },
                    icon = {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = stringResource(R.string.home),
                        )
                    },
                    label = { Text(stringResource(R.string.home)) },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Dashboard,
                    onClick = { onSelectTab(MainTab.Dashboard) },
                    icon = {
                        Icon(
                            Icons.Filled.Dashboard,
                            contentDescription = stringResource(R.string.dashboard),
                        )
                    },
                    label = { Text(stringResource(R.string.dashboard)) },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Templates,
                    onClick = { onSelectTab(MainTab.Templates) },
                    icon = {
                        Icon(
                            painterResource(R.drawable.baseline_bookmark_24),
                            contentDescription = stringResource(R.string.templates),
                        )
                    },
                    label = { Text(stringResource(R.string.templates)) },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.Settings,
                    onClick = { onSelectTab(MainTab.Settings) },
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    },
                    label = { Text(stringResource(R.string.settings)) },
                )
            }
        },
        floatingActionButton = {
            if (fileUri != null) {
                AnimatedVisibility(
                    visible = selectedTab != MainTab.Settings,
                    enter = slideInHorizontally(
                        animationSpec = tween(durationMillis = 300),
                        initialOffsetX = { it },
                    ),
                    exit = slideOutHorizontally(
                        animationSpec = tween(durationMillis = 300),
                        targetOffsetX = { it },
                    ),
                ) {
                    val density = LocalDensity.current
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Template FAB (secondary) - slides in/out from behind main FAB
                        AnimatedVisibility(
                            visible = selectedTab == MainTab.Templates && onTemplateAddClick != null,
                            enter = slideInVertically(
                                animationSpec = tween(durationMillis = 300),
                                initialOffsetY = { with(density) { 56.dp.roundToPx() } },
                            ),
                            exit = slideOutVertically(
                                animationSpec = tween(durationMillis = 300),
                                targetOffsetY = { with(density) { 56.dp.roundToPx() } },
                            ),
                        ) {
                            onTemplateAddClick?.let { onClick ->
                                SmallFloatingActionButton(
                                    onClick = onClick,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ) {
                                    Icon(
                                        painterResource(R.drawable.baseline_bookmark_24),
                                        contentDescription = stringResource(R.string.add_template),
                                    )
                                }
                            }
                        }
                        // Main FAB - always static
                        FloatingActionButton(
                            onClick = onAddClick,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = stringResource(R.string.add)
                            )
                        }
                    }
                }
            }
        },
        modifier = Modifier.imePadding(),
    ) { contentPadding ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                slideInHorizontally(
                    animationSpec = tween(300),
                    initialOffsetX = { if (targetState.ordinal > initialState.ordinal) it else -it },
                ) togetherWith slideOutHorizontally(
                    animationSpec = tween(300),
                    targetOffsetX = { if (targetState.ordinal > initialState.ordinal) -it else it },
                )
            },
            label = "tab_switch",
        ) { tab ->
            when (tab) {
                MainTab.Home -> {
                    if (fileUri != null) {
                        MainContent(
                            transactions = transactions,
                            query = query,
                            isRefreshing = isRefreshing,
                            selected = selected,
                            onRefresh = onRefresh,
                            onToggleSelect = onToggleSelect,
                            contentPadding = contentPadding,
                        )
                    } else {
                        NoFileState(
                            contentPadding = contentPadding,
                            onGoToSettings = { onSelectTab(MainTab.Settings) },
                        )
                    }
                }
                else -> {
                    if (tabContent != null) {
                        tabContent(tab, contentPadding)
                    } else if (dashboardViewModel != null &&
                        templatesViewModel != null &&
                        preferencesViewModel != null
                    ) {
                        MainTabContent(
                            tab = tab,
                            contentPadding = contentPadding,
                            onDashboardAccountClick = onDashboardAccountClick,
                            onCashFlowClick = onCashFlowClick,
                            onTemplateAddClick = {
                                context.startActivity(
                                    Intent(context, TemplateFormActivity::class.java)
                                )
                            },
                            onTemplateClick = { template ->
                                val intent = Intent(context, AddActivity::class.java)
                                intent.putExtra(TEMPLATE_ID_KEY, template.id)
                                context.startActivity(intent)
                            },
                            onTemplateEditClick = { template ->
                                val intent = Intent(context, TemplateFormActivity::class.java)
                                intent.putExtra(EDIT_TEMPLATE_ID_KEY, template.id)
                                context.startActivity(intent)
                            },
                            onOpenFile = onOpenFile,
                            onGoToSettings = { onSelectTab(MainTab.Settings) },
                            dashboardViewModel = dashboardViewModel,
                            templatesViewModel = templatesViewModel,
                            preferencesViewModel = preferencesViewModel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainTabContent(
    tab: MainTab,
    contentPadding: PaddingValues,
    onDashboardAccountClick: () -> Unit,
    onCashFlowClick: () -> Unit,
    onTemplateAddClick: () -> Unit,
    onTemplateClick: (TransactionTemplate) -> Unit,
    onTemplateEditClick: (TransactionTemplate) -> Unit,
    onOpenFile: () -> Unit,
    onGoToSettings: () -> Unit,
    dashboardViewModel: DashboardViewModel,
    templatesViewModel: TemplatesViewModel,
    preferencesViewModel: PreferencesViewModel,
) {
    val context = LocalContext.current

    val latestTemplateError by templatesViewModel.latestError.observeAsState()
    val errorWritingFile = stringResource(R.string.error_writing_file)
    LaunchedEffect(latestTemplateError) {
        if (latestTemplateError?.get() != null) {
            Toast.makeText(context, errorWritingFile, Toast.LENGTH_LONG).show()
        }
    }

    val latestTemplateReadError by templatesViewModel.latestReadError.observeAsState()
    val errorReadingFile = stringResource(R.string.error_reading_file)
    LaunchedEffect(latestTemplateReadError) {
        if (latestTemplateReadError?.get() != null) {
            Toast.makeText(context, errorReadingFile, Toast.LENGTH_LONG).show()
        }
    }

    val latestTemplateMismatch by templatesViewModel.latestMismatch.observeAsState()
    val mismatchMessage = stringResource(R.string.mismatch_no_delete)
    LaunchedEffect(latestTemplateMismatch) {
        if (latestTemplateMismatch?.get() != null) {
            Toast.makeText(context, mismatchMessage, Toast.LENGTH_LONG).show()
        }
    }

    val netWorth by dashboardViewModel.netWorth.observeAsState()
    val accountBalances by dashboardViewModel.accountBalances.observeAsState()
    val cashFlow by dashboardViewModel.currentMonthCashFlow.observeAsState()
    val decimalSeparator by dashboardViewModel.decimalSeparator.observeAsState(".")
    val templates by templatesViewModel.templates.observeAsState(emptyList())
    val saving by templatesViewModel.saving.observeAsState(false)
    val fileUri by preferencesViewModel.fileUri.observeAsState()
    val transactionDefaultElements by preferencesViewModel.transactionDefaultElements.observeAsState(emptyList())
    val transactionStatusPresentByDefault by preferencesViewModel.transactionStatusPresentByDefault.observeAsState(true)
    val transactionCodePresentByDefault by preferencesViewModel.transactionCodePresentByDefault.observeAsState(false)
    val transactionPayeePresentByDefault by preferencesViewModel.transactionPayeePresentByDefault.observeAsState(true)
    val transactionNotePresentByDefault by preferencesViewModel.transactionNotePresentByDefault.observeAsState(true)
    val transactionCurrenciesPresentByDefault by preferencesViewModel.transactionCurrenciesPresentByDefault.observeAsState(true)
    val postingDefaultElements by preferencesViewModel.postingDefaultElements.observeAsState(emptyList())
    val postingAmountPresentByDefault by preferencesViewModel.postingAmountPresentByDefault.observeAsState(true)
    val postingCostPresentByDefault by preferencesViewModel.postingCostPresentByDefault.observeAsState(false)
    val postingAssertionPresentByDefault by preferencesViewModel.postingAssertionPresentByDefault.observeAsState(false)
    val postingAssertionCostPresentByDefault by preferencesViewModel.postingAssertionCostPresentByDefault.observeAsState(false)
    val postingCommentPresentByDefault by preferencesViewModel.postingCommentPresentByDefault.observeAsState(false)
    val defaultCurrency by preferencesViewModel.defaultCurrency.observeAsState("€")
    val postingWidth by preferencesViewModel.postingWidth.observeAsState(72)
    val defaultStatus by preferencesViewModel.defaultStatus.observeAsState(" ")
    val prefDecimalSeparator by preferencesViewModel.decimalSeparator.observeAsState(".")
    val currencyBeforeAmount by preferencesViewModel.currencyBeforeAmount.observeAsState(true)
    val currencyAmountSpacing by preferencesViewModel.spacingBetweenCurrencyAndAmount.observeAsState(true)
    val assetsPrefixes by preferencesViewModel.assetsPrefixes.observeAsState(listOf("Assets"))
    val liabilitiesPrefixes by preferencesViewModel.liabilitiesPrefixes.observeAsState(listOf("Liabilities"))
    val equityPrefixes by preferencesViewModel.equityPrefixes.observeAsState(listOf("Equity"))
    val incomePrefixes by preferencesViewModel.incomePrefixes.observeAsState(listOf("Income"))
    val expensesPrefixes by preferencesViewModel.expensesPrefixes.observeAsState(listOf("Expenses"))

    val configuration = when (tab) {
        MainTab.Dashboard -> TabConfiguration.Dashboard(
            netWorth = netWorth,
            accountBalances = accountBalances,
            cashFlow = cashFlow,
            decimalSeparator = decimalSeparator,
            hasFile = fileUri != null,
            onAccountClick = onDashboardAccountClick,
            onCashFlowClick = onCashFlowClick,
        )

        MainTab.Templates -> TabConfiguration.Templates(
            templates = templates,
            saving = saving,
            hasFile = fileUri != null,
            onAddClick = onTemplateAddClick,
            onTemplateClick = onTemplateClick,
            onEditClick = onTemplateEditClick,
            onDeleteClick = { templatesViewModel.deleteTemplate(it, {}) },
            onGoToSettings = onGoToSettings,
        )

        MainTab.Settings -> TabConfiguration.Settings(
            fileUri = fileUri,
            onOpenFile = onOpenFile,
            transactionDefaultElements = transactionDefaultElements,
            transactionStatusPresentByDefault = transactionStatusPresentByDefault,
            onTransactionStatusPresentByDefaultChange = {
                preferencesViewModel.storeTransactionStatusPresentByDefault(
                    it
                )
            },
            transactionCodePresentByDefault = transactionCodePresentByDefault,
            onTransactionCodePresentByDefaultChange = {
                preferencesViewModel.storeTransactionCodePresentByDefault(
                    it
                )
            },
            transactionPayeePresentByDefault = transactionPayeePresentByDefault,
            onTransactionPayeePresentByDefaultChange = {
                preferencesViewModel.storeTransactionPayeePresentByDefault(
                    it
                )
            },
            transactionNotePresentByDefault = transactionNotePresentByDefault,
            onTransactionNotePresentByDefaultChange = {
                preferencesViewModel.storeTransactionNotePresentByDefault(
                    it
                )
            },
            transactionCurrenciesPresentByDefault = transactionCurrenciesPresentByDefault,
            onTransactionCurrenciesPresentByDefaultChange = {
                preferencesViewModel.storeTransactionCurrenciesPresentByDefault(
                    it
                )
            },
            postingDefaultElements = postingDefaultElements,
            postingAmountPresentByDefault = postingAmountPresentByDefault,
            onPostingAmountPresentByDefaultChange = {
                preferencesViewModel.storePostingAmountPresentByDefault(
                    it
                )
            },
            postingCostPresentByDefault = postingCostPresentByDefault,
            onPostingCostPresentByDefaultChange = {
                preferencesViewModel.storePostingCostPresentByDefault(
                    it
                )
            },
            postingAssertionPresentByDefault = postingAssertionPresentByDefault,
            onPostingAssertionPresentByDefaultChange = {
                preferencesViewModel.storePostingAssertionPresentByDefault(
                    it
                )
            },
            postingAssertionCostPresentByDefault = postingAssertionCostPresentByDefault,
            onPostingAssertionCostPresentByDefaultChange = {
                preferencesViewModel.storePostingAssertionCostPresentByDefault(
                    it
                )
            },
            postingCommentPresentByDefault = postingCommentPresentByDefault,
            onPostingCommentPresentByDefaultChange = {
                preferencesViewModel.storePostingCommentPresentByDefault(
                    it
                )
            },
            defaultCurrency = defaultCurrency,
            onDefaultCurrencyChange = { preferencesViewModel.storeDefaultCurrency(it) },
            postingWidth = postingWidth,
            onPostingWidthChange = { preferencesViewModel.storePostingWidth(it) },
            defaultStatus = defaultStatus,
            onDefaultStatusChange = { preferencesViewModel.storeDefaultStatus(it) },
            decimalSeparator = prefDecimalSeparator,
            onDecimalSeparatorChange = { preferencesViewModel.storeDecimalSeparator(it) },
            currencyBeforeAmount = currencyBeforeAmount,
            onCurrencyBeforeAmountChange = { preferencesViewModel.storeCurrencyBeforeAmount(it) },
            currencyAmountSpacing = currencyAmountSpacing,
            onCurrencyAmountSpacingChange = { preferencesViewModel.storeCurrencyAmountSpacing(it) },
            assetsPrefixes = assetsPrefixes,
            onAssetsPrefixesChange = { preferencesViewModel.storeAssetsPrefixes(it) },
            liabilitiesPrefixes = liabilitiesPrefixes,
            onLiabilitiesPrefixesChange = { preferencesViewModel.storeLiabilitiesPrefixes(it) },
            equityPrefixes = equityPrefixes,
            onEquityPrefixesChange = { preferencesViewModel.storeEquityPrefixes(it) },
            incomePrefixes = incomePrefixes,
            onIncomePrefixesChange = { preferencesViewModel.storeIncomePrefixes(it) },
            expensesPrefixes = expensesPrefixes,
            onExpensesPrefixesChange = { preferencesViewModel.storeExpensesPrefixes(it) },
        )

        MainTab.Home -> null
    }

    if (configuration != null) {
        MainTabContent(
            tab = tab,
            contentPadding = contentPadding,
            configuration = configuration,
        )
    }
}

@Composable
fun MainTabContent(
    tab: MainTab,
    contentPadding: PaddingValues,
    configuration: TabConfiguration,
) {
    when (configuration) {
        is TabConfiguration.Dashboard -> DashboardScreenContent(
            netWorth = configuration.netWorth,
            accountBalances = configuration.accountBalances,
            cashFlow = configuration.cashFlow,
            decimalSeparator = configuration.decimalSeparator,
            hasFile = configuration.hasFile,
            onBackClick = {},
            onAccountClick = configuration.onAccountClick,
            onCashFlowClick = configuration.onCashFlowClick,
            showTopBar = false,
            contentPadding = contentPadding,
        )

        is TabConfiguration.Templates -> TemplatesScreenContent(
            templates = configuration.templates,
            saving = configuration.saving,
            hasFile = configuration.hasFile,
            onBackClick = {},
            onAddClick = configuration.onAddClick,
            onTemplateClick = configuration.onTemplateClick,
            onEditClick = configuration.onEditClick,
            onDeleteClick = configuration.onDeleteClick,
            showTopBar = false,
            showFab = false,
            contentPadding = contentPadding,
            onGoToSettings = configuration.onGoToSettings,
        )

        is TabConfiguration.Settings -> PreferencesScreen(
            fileUri = configuration.fileUri,
            onOpenFile = configuration.onOpenFile,
            transactionDefaultElements = configuration.transactionDefaultElements,
            transactionStatusPresentByDefault = configuration.transactionStatusPresentByDefault,
            onTransactionStatusPresentByDefaultChange = configuration.onTransactionStatusPresentByDefaultChange,
            transactionCodePresentByDefault = configuration.transactionCodePresentByDefault,
            onTransactionCodePresentByDefaultChange = configuration.onTransactionCodePresentByDefaultChange,
            transactionPayeePresentByDefault = configuration.transactionPayeePresentByDefault,
            onTransactionPayeePresentByDefaultChange = configuration.onTransactionPayeePresentByDefaultChange,
            transactionNotePresentByDefault = configuration.transactionNotePresentByDefault,
            onTransactionNotePresentByDefaultChange = configuration.onTransactionNotePresentByDefaultChange,
            transactionCurrenciesPresentByDefault = configuration.transactionCurrenciesPresentByDefault,
            onTransactionCurrenciesPresentByDefaultChange = configuration.onTransactionCurrenciesPresentByDefaultChange,
            postingDefaultElements = configuration.postingDefaultElements,
            postingAmountPresentByDefault = configuration.postingAmountPresentByDefault,
            onPostingAmountPresentByDefaultChange = configuration.onPostingAmountPresentByDefaultChange,
            postingCostPresentByDefault = configuration.postingCostPresentByDefault,
            onPostingCostPresentByDefaultChange = configuration.onPostingCostPresentByDefaultChange,
            postingAssertionPresentByDefault = configuration.postingAssertionPresentByDefault,
            onPostingAssertionPresentByDefaultChange = configuration.onPostingAssertionPresentByDefaultChange,
            postingAssertionCostPresentByDefault = configuration.postingAssertionCostPresentByDefault,
            onPostingAssertionCostPresentByDefaultChange = configuration.onPostingAssertionCostPresentByDefaultChange,
            postingCommentPresentByDefault = configuration.postingCommentPresentByDefault,
            onPostingCommentPresentByDefaultChange = configuration.onPostingCommentPresentByDefaultChange,
            defaultCurrency = configuration.defaultCurrency,
            onDefaultCurrencyChange = configuration.onDefaultCurrencyChange,
            postingWidth = configuration.postingWidth,
            onPostingWidthChange = configuration.onPostingWidthChange,
            defaultStatus = configuration.defaultStatus,
            onDefaultStatusChange = configuration.onDefaultStatusChange,
            decimalSeparator = configuration.decimalSeparator,
            onDecimalSeparatorChange = configuration.onDecimalSeparatorChange,
            currencyBeforeAmount = configuration.currencyBeforeAmount,
            onCurrencyBeforeAmountChange = configuration.onCurrencyBeforeAmountChange,
            currencyAmountSpacing = configuration.currencyAmountSpacing,
            onCurrencyAmountSpacingChange = configuration.onCurrencyAmountSpacingChange,
            assetsPrefixes = configuration.assetsPrefixes,
            onAssetsPrefixesChange = configuration.onAssetsPrefixesChange,
            liabilitiesPrefixes = configuration.liabilitiesPrefixes,
            onLiabilitiesPrefixesChange = configuration.onLiabilitiesPrefixesChange,
            equityPrefixes = configuration.equityPrefixes,
            onEquityPrefixesChange = configuration.onEquityPrefixesChange,
            incomePrefixes = configuration.incomePrefixes,
            onIncomePrefixesChange = configuration.onIncomePrefixesChange,
            expensesPrefixes = configuration.expensesPrefixes,
            onExpensesPrefixesChange = configuration.onExpensesPrefixesChange,
            showTopBar = false,
            contentPadding = contentPadding,
        )
    }
}

@Composable
fun MainContent(
    transactions: List<Pair<Int, Transaction>>?,
    query: String,
    isRefreshing: Boolean,
    selected: Int?,
    onRefresh: () -> Unit,
    onToggleSelect: (Int) -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier.padding(contentPadding),
    ) {
        if (transactions != null && (transactions.isNotEmpty() || isRefreshing)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(transactions.size) {
                    val index = transactions.size - it - 1
                    val (originalIndex, transaction) = transactions[index]
                    TransactionCard(
                        transaction,
                        originalIndex == selected,
                        { onToggleSelect(originalIndex) },
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                8.dp,
                                if (it == 0) 8.dp else 4.dp,
                                8.dp,
                                if (it == transactions.size - 1) 8.dp else 4.dp,
                            ),
                    )
                }

            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    if (query == "") {
                        Text(
                            stringResource(R.string.no_transactions_yet),
                            style = MaterialTheme.typography.headlineLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Text(
                            stringResource(R.string.create_transaction),
                            style =
                                MaterialTheme.typography.headlineLarge.copy(
                                    textDecoration = TextDecoration.Underline,
                                    color = MaterialTheme.colorScheme.primary,
                                ),
                            textAlign = TextAlign.Center,
                            modifier =
                                Modifier
                                    .padding(horizontal = 16.dp)
                                    .clickable {
                                        context.startActivity(
                                            Intent(context, AddActivity::class.java),
                                        )
                                    },
                        )
                    } else {
                        Text(
                            stringResource(R.string.no_search_results),
                            style = MaterialTheme.typography.headlineLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainBar(selectedTab: MainTab, onSearchClick: () -> Unit) {
    val title = when (selectedTab) {
        MainTab.Home -> stringResource(R.string.app_name)
        MainTab.Dashboard -> stringResource(R.string.dashboard)
        MainTab.Templates -> stringResource(R.string.templates)
        MainTab.Settings -> stringResource(R.string.settings)
    }
    TopAppBar(
        title = { Text(title) },
        actions = {
            if (selectedTab == MainTab.Home) {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    )
}

@Composable
fun SelectionBar(
    onStopSelection: () -> Unit,
    onCopyClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = onStopSelection,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.stop_selection),
                )
            }
        },
        title = { },
        actions = {
            IconButton(onClick = onCopyClick) {
                Icon(
                    painterResource(R.drawable.baseline_difference_24),
                    contentDescription = stringResource(R.string.copy_and_edit),
                )
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    )

    BackHandler {
        onStopSelection()
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onStopSearching: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = onStopSearching,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.stop_searching),
                )
            }
        },
        title = {
            TextField(
                query,
                onQueryChange,
                singleLine = true,
                placeholder = {
                    Text(
                        stringResource(R.string.search),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Normal,
                    )
                },
                colors =
                    TextFieldDefaults.colors(
                        focusedTextColor = LocalContentColor.current,
                        unfocusedTextColor = LocalContentColor.current,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        cursorColor = LocalContentColor.current,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("search-field"),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusRequester.freeFocus()
                        },
                    ),
            )
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    )
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
    BackHandler {
        onStopSearching()
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MicroLedgerTheme {
        MainScreen(
            fileUri = "content://test".toUri(),
            transactions =
                listOf(
                    0 to
                            Transaction(
                                firstLine = 0,
                                lastLine = 2,
                                date = "2023-08-31",
                                status = "*",
                                code = null,
                                payee = "Payee",
                                note = "Note",
                                postings =
                                    listOf(
                                        Posting(
                                            "assets",
                                            Amount("-5.00", "€", "€ -5.00"),
                                            null,
                                            null,
                                            null,
                                            null,
                                        ),
                                        Posting(
                                            "expenses",
                                            Amount("5.00", "€", "€ 5.00"),
                                            null,
                                            null,
                                            null,
                                            null,
                                        ),
                                    ),
                            ),
                    1 to
                            Transaction(
                                firstLine = 3,
                                lastLine = 5,
                                date = "2023-09-01",
                                status = "!",
                                code = "123",
                                payee = "Another Payee",
                                note = null,
                                postings =
                                    listOf(
                                        Posting(
                                            "assets",
                                            Amount("-10.00", "€", "€ -10.00"),
                                            null,
                                            null,
                                            null,
                                            null,
                                        ),
                                        Posting(
                                            "expenses",
                                            Amount("10.00", "€", "€ 10.00"),
                                            null,
                                            null,
                                            null,
                                            null,
                                        ),
                                    ),
                            ),
                ),
            searching = false,
            query = "",
            isRefreshing = false,
            selected = null,
            selectedTab = MainTab.Home,
            onRefresh = {},
            onToggleSelect = {},
            onSearchClick = {},
            onSelectTab = {},
            onStopSearching = {},
            onQueryChange = {},
            onStopSelection = {},
            onCopyClick = {},
            onEditClick = {},
            onDeleteClick = {},
            onAddClick = {},
            onDashboardAccountClick = {},
            onCashFlowClick = {},
            onOpenFile = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainContentPreview() {
    MicroLedgerTheme {
        MainContent(
            transactions =
                listOf(
                    0 to
                            Transaction(
                                firstLine = 0,
                                lastLine = 2,
                                date = "2023-08-31",
                                status = "*",
                                code = null,
                                payee = "Payee",
                                note = "Note",
                                postings =
                                    listOf(
                                        Posting(
                                            "assets",
                                            Amount("-5.00", "€", "€ -5.00"),
                                            null,
                                            null,
                                            null,
                                            null,
                                        ),
                                        Posting(
                                            "expenses",
                                            Amount("5.00", "€", "€ 5.00"),
                                            null,
                                            null,
                                            null,
                                            null,
                                        ),
                                    ),
                            ),
                    1 to
                            Transaction(
                                firstLine = 3,
                                lastLine = 5,
                                date = "2023-09-01",
                                status = "!",
                                code = "123",
                                payee = "Another Payee",
                                note = null,
                                postings =
                                    listOf(
                                        Posting(
                                            "assets",
                                            Amount("-10.00", "€", "€ -10.00"),
                                            null,
                                            null,
                                            null,
                                            null,
                                        ),
                                        Posting(
                                            "expenses",
                                            Amount("10.00", "€", "€ 10.00"),
                                            null,
                                            null,
                                            null,
                                            null,
                                        ),
                                    ),
                            ),
                ),
            query = "",
            isRefreshing = false,
            selected = null,
            onRefresh = {},
            onToggleSelect = {},
            contentPadding = PaddingValues(0.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenNoFilePreview() {
    MicroLedgerTheme {
        MainScreen(
            fileUri = null,
            transactions = null,
            searching = false,
            query = "",
            isRefreshing = false,
            selected = null,
            selectedTab = MainTab.Home,
            onRefresh = {},
            onToggleSelect = {},
            onSearchClick = {},
            onSelectTab = {},
            onStopSearching = {},
            onQueryChange = {},
            onStopSelection = {},
            onCopyClick = {},
            onEditClick = {},
            onDeleteClick = {},
            onAddClick = {},
            onDashboardAccountClick = {},
            onCashFlowClick = {},
            onOpenFile = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenDashboardTabPreview() {
    MicroLedgerTheme {
        MainScreen(
            fileUri = "content://test".toUri(),
            transactions = null,
            searching = false,
            query = "",
            isRefreshing = false,
            selected = null,
            selectedTab = MainTab.Dashboard,
            onRefresh = {},
            onToggleSelect = {},
            onSearchClick = {},
            onSelectTab = {},
            onStopSearching = {},
            onQueryChange = {},
            onStopSelection = {},
            onCopyClick = {},
            onEditClick = {},
            onDeleteClick = {},
            onAddClick = {},
            onDashboardAccountClick = {},
            onCashFlowClick = {},
            onOpenFile = {},
            tabContent = { tab, contentPadding ->
                val configuration = when (tab) {
                    MainTab.Dashboard -> TabConfiguration.Dashboard(
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
                        cashFlow = MonthlyCashFlowCalculator.CashFlowResult(
                            totalIncome = BigDecimal("5000.00"),
                            totalExpenses = BigDecimal("1635.00"),
                            netFlow = BigDecimal("3365.00"),
                            period = "2023-09",
                            incomeTransactions = emptyList(),
                            expenseTransactions = emptyList(),
                        ),
                        decimalSeparator = ".",
                        hasFile = true,
                        onAccountClick = {},
                        onCashFlowClick = {},
                    )

                    MainTab.Templates -> TabConfiguration.Templates(
                        templates = emptyList(),
                        saving = false,
                        hasFile = true,
                        onAddClick = {},
                        onTemplateClick = {},
                        onEditClick = {},
                        onDeleteClick = {},
                        onGoToSettings = {},
                    )

                    MainTab.Settings -> TabConfiguration.Settings(
                        fileUri = null,
                        onOpenFile = {},
                        transactionDefaultElements = listOf(R.string.status, R.string.payee),
                        transactionStatusPresentByDefault = true,
                        onTransactionStatusPresentByDefaultChange = {},
                        transactionCodePresentByDefault = false,
                        onTransactionCodePresentByDefaultChange = {},
                        transactionPayeePresentByDefault = true,
                        onTransactionPayeePresentByDefaultChange = {},
                        transactionNotePresentByDefault = true,
                        onTransactionNotePresentByDefaultChange = {},
                        transactionCurrenciesPresentByDefault = true,
                        onTransactionCurrenciesPresentByDefaultChange = {},
                        postingDefaultElements = listOf(R.string.amount),
                        postingAmountPresentByDefault = true,
                        onPostingAmountPresentByDefaultChange = {},
                        postingCostPresentByDefault = false,
                        onPostingCostPresentByDefaultChange = {},
                        postingAssertionPresentByDefault = false,
                        onPostingAssertionPresentByDefaultChange = {},
                        postingAssertionCostPresentByDefault = false,
                        onPostingAssertionCostPresentByDefaultChange = {},
                        postingCommentPresentByDefault = false,
                        onPostingCommentPresentByDefaultChange = {},
                        defaultCurrency = "€",
                        onDefaultCurrencyChange = {},
                        postingWidth = 72,
                        onPostingWidthChange = {},
                        defaultStatus = " ",
                        onDefaultStatusChange = {},
                        decimalSeparator = ".",
                        onDecimalSeparatorChange = {},
                        currencyBeforeAmount = true,
                        onCurrencyBeforeAmountChange = {},
                        currencyAmountSpacing = true,
                        onCurrencyAmountSpacingChange = {},
                        assetsPrefixes = listOf("Assets"),
                        onAssetsPrefixesChange = {},
                        liabilitiesPrefixes = listOf("Liabilities"),
                        onLiabilitiesPrefixesChange = {},
                        equityPrefixes = listOf("Equity"),
                        onEquityPrefixesChange = {},
                        incomePrefixes = listOf("Income"),
                        onIncomePrefixesChange = {},
                        expensesPrefixes = listOf("Expenses"),
                        onExpensesPrefixesChange = {},
                    )

                    MainTab.Home -> null
                }
                if (configuration != null) {
                    MainTabContent(
                        tab = tab,
                        contentPadding = contentPadding,
                        configuration = configuration,
                    )
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenTemplatesTabPreview() {
    MicroLedgerTheme {
        MainScreen(
            fileUri = "content://test".toUri(),
            transactions = null,
            searching = false,
            query = "",
            isRefreshing = false,
            selected = null,
            selectedTab = MainTab.Templates,
            onRefresh = {},
            onToggleSelect = {},
            onSearchClick = {},
            onSelectTab = {},
            onStopSearching = {},
            onQueryChange = {},
            onStopSelection = {},
            onCopyClick = {},
            onEditClick = {},
            onDeleteClick = {},
            onAddClick = {},
            onDashboardAccountClick = {},
            onCashFlowClick = {},
            onOpenFile = {},
            onTemplateAddClick = {},
            tabContent = { tab, contentPadding ->
                val configuration = when (tab) {
                    MainTab.Dashboard -> TabConfiguration.Dashboard(
                        netWorth = null,
                        accountBalances = null,
                        cashFlow = null,
                        decimalSeparator = ".",
                        hasFile = true,
                        onAccountClick = {},
                        onCashFlowClick = {},
                    )

                    MainTab.Templates -> TabConfiguration.Templates(
                        templates = listOf(
                            TransactionTemplate(
                                firstLine = 0,
                                lastLine = 0,
                                id = "1",
                                name = "Simple Template",
                                payee = "Some Payee",
                                note = null,
                                status = null,
                                code = null,
                                postings = listOf(
                                    Posting("Assets:Checking", null, null, null, null, null),
                                    Posting("Expenses:Groceries", null, null, null, null, null),
                                ),
                            ),
                            TransactionTemplate(
                                firstLine = 0,
                                lastLine = 0,
                                id = "2",
                                name = "Complex Template",
                                payee = "Another Payee",
                                note = "A note",
                                status = "*",
                                code = "123",
                                postings = listOf(
                                    Posting("Assets:Checking", null, null, null, null, null),
                                    Posting("Expenses:Food", null, null, null, null, null),
                                    Posting("Expenses:Drink", null, null, null, null, null),
                                ),
                            ),
                        ),
                        saving = false,
                        hasFile = true,
                        onAddClick = {},
                        onTemplateClick = {},
                        onEditClick = {},
                        onDeleteClick = {},
                        onGoToSettings = {},
                    )

                    MainTab.Settings -> TabConfiguration.Settings(
                        fileUri = null,
                        onOpenFile = {},
                        transactionDefaultElements = listOf(R.string.status, R.string.payee),
                        transactionStatusPresentByDefault = true,
                        onTransactionStatusPresentByDefaultChange = {},
                        transactionCodePresentByDefault = false,
                        onTransactionCodePresentByDefaultChange = {},
                        transactionPayeePresentByDefault = true,
                        onTransactionPayeePresentByDefaultChange = {},
                        transactionNotePresentByDefault = true,
                        onTransactionNotePresentByDefaultChange = {},
                        transactionCurrenciesPresentByDefault = true,
                        onTransactionCurrenciesPresentByDefaultChange = {},
                        postingDefaultElements = listOf(R.string.amount),
                        postingAmountPresentByDefault = true,
                        onPostingAmountPresentByDefaultChange = {},
                        postingCostPresentByDefault = false,
                        onPostingCostPresentByDefaultChange = {},
                        postingAssertionPresentByDefault = false,
                        onPostingAssertionPresentByDefaultChange = {},
                        postingAssertionCostPresentByDefault = false,
                        onPostingAssertionCostPresentByDefaultChange = {},
                        postingCommentPresentByDefault = false,
                        onPostingCommentPresentByDefaultChange = {},
                        defaultCurrency = "€",
                        onDefaultCurrencyChange = {},
                        postingWidth = 72,
                        onPostingWidthChange = {},
                        defaultStatus = " ",
                        onDefaultStatusChange = {},
                        decimalSeparator = ".",
                        onDecimalSeparatorChange = {},
                        currencyBeforeAmount = true,
                        onCurrencyBeforeAmountChange = {},
                        currencyAmountSpacing = true,
                        onCurrencyAmountSpacingChange = {},
                        assetsPrefixes = listOf("Assets"),
                        onAssetsPrefixesChange = {},
                        liabilitiesPrefixes = listOf("Liabilities"),
                        onLiabilitiesPrefixesChange = {},
                        equityPrefixes = listOf("Equity"),
                        onEquityPrefixesChange = {},
                        incomePrefixes = listOf("Income"),
                        onIncomePrefixesChange = {},
                        expensesPrefixes = listOf("Expenses"),
                        onExpensesPrefixesChange = {},
                    )

                    MainTab.Home -> null
                }
                if (configuration != null) {
                    MainTabContent(
                        tab = tab,
                        contentPadding = contentPadding,
                        configuration = configuration,
                    )
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenSettingsTabPreview() {
    MicroLedgerTheme {
        MainScreen(
            fileUri = "content://test".toUri(),
            transactions = null,
            searching = false,
            query = "",
            isRefreshing = false,
            selected = null,
            selectedTab = MainTab.Settings,
            onRefresh = {},
            onToggleSelect = {},
            onSearchClick = {},
            onSelectTab = {},
            onStopSearching = {},
            onQueryChange = {},
            onStopSelection = {},
            onCopyClick = {},
            onEditClick = {},
            onDeleteClick = {},
            onAddClick = {},
            onDashboardAccountClick = {},
            onCashFlowClick = {},
            onOpenFile = {},
            tabContent = { tab, contentPadding ->
                val configuration = when (tab) {
                    MainTab.Dashboard -> TabConfiguration.Dashboard(
                        netWorth = null,
                        accountBalances = null,
                        cashFlow = null,
                        decimalSeparator = ".",
                        hasFile = true,
                        onAccountClick = {},
                        onCashFlowClick = {},
                    )

                    MainTab.Templates -> TabConfiguration.Templates(
                        templates = emptyList(),
                        saving = false,
                        hasFile = true,
                        onAddClick = {},
                        onTemplateClick = {},
                        onEditClick = {},
                        onDeleteClick = {},
                        onGoToSettings = {},
                    )

                    MainTab.Settings -> TabConfiguration.Settings(
                        fileUri = null,
                        onOpenFile = {},
                        transactionDefaultElements = listOf(R.string.status, R.string.payee),
                        transactionStatusPresentByDefault = true,
                        onTransactionStatusPresentByDefaultChange = {},
                        transactionCodePresentByDefault = false,
                        onTransactionCodePresentByDefaultChange = {},
                        transactionPayeePresentByDefault = true,
                        onTransactionPayeePresentByDefaultChange = {},
                        transactionNotePresentByDefault = true,
                        onTransactionNotePresentByDefaultChange = {},
                        transactionCurrenciesPresentByDefault = true,
                        onTransactionCurrenciesPresentByDefaultChange = {},
                        postingDefaultElements = listOf(R.string.amount),
                        postingAmountPresentByDefault = true,
                        onPostingAmountPresentByDefaultChange = {},
                        postingCostPresentByDefault = false,
                        onPostingCostPresentByDefaultChange = {},
                        postingAssertionPresentByDefault = false,
                        onPostingAssertionPresentByDefaultChange = {},
                        postingAssertionCostPresentByDefault = false,
                        onPostingAssertionCostPresentByDefaultChange = {},
                        postingCommentPresentByDefault = false,
                        onPostingCommentPresentByDefaultChange = {},
                        defaultCurrency = "€",
                        onDefaultCurrencyChange = {},
                        postingWidth = 72,
                        onPostingWidthChange = {},
                        defaultStatus = " ",
                        onDefaultStatusChange = {},
                        decimalSeparator = ".",
                        onDecimalSeparatorChange = {},
                        currencyBeforeAmount = true,
                        onCurrencyBeforeAmountChange = {},
                        currencyAmountSpacing = true,
                        onCurrencyAmountSpacingChange = {},
                        assetsPrefixes = listOf("Assets"),
                        onAssetsPrefixesChange = {},
                        liabilitiesPrefixes = listOf("Liabilities"),
                        onLiabilitiesPrefixesChange = {},
                        equityPrefixes = listOf("Equity"),
                        onEquityPrefixesChange = {},
                        incomePrefixes = listOf("Income"),
                        onIncomePrefixesChange = {},
                        expensesPrefixes = listOf("Expenses"),
                        onExpensesPrefixesChange = {},
                    )

                    MainTab.Home -> null
                }
                if (configuration != null) {
                    MainTabContent(
                        tab = tab,
                        contentPadding = contentPadding,
                        configuration = configuration,
                    )
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenTemplatesTabWithFabPreview() {
    MicroLedgerTheme {
        MainScreen(
            fileUri = "content://test".toUri(),
            transactions = null,
            searching = false,
            query = "",
            isRefreshing = false,
            selected = null,
            selectedTab = MainTab.Templates,
            onRefresh = {},
            onToggleSelect = {},
            onSearchClick = {},
            onSelectTab = {},
            onStopSearching = {},
            onQueryChange = {},
            onStopSelection = {},
            onCopyClick = {},
            onEditClick = {},
            onDeleteClick = {},
            onAddClick = {},
            onDashboardAccountClick = {},
            onCashFlowClick = {},
            onOpenFile = {},
            onTemplateAddClick = {},
        )
    }
}
