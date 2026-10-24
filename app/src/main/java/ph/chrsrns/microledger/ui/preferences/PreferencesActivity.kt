package ph.chrsrns.microledger.ui.preferences

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import ph.chrsrns.microledger.R
import ph.chrsrns.microledger.data.SettingsState
import ph.chrsrns.microledger.ui.theme.MicroLedgerTheme

@AndroidEntryPoint
class PreferencesActivity : ComponentActivity() {
    private val preferencesViewModel: PreferencesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openFile =
            registerForActivityResult(OpenDocument()) { uri: Uri? ->
                if (uri != null) {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                    preferencesViewModel.storeFileUri(uri)
                }
            }
        setContent {
            MicroLedgerTheme {
                PreferencesScreen(
                    preferencesViewModel = preferencesViewModel,
                    onOpenFile = { openFile.launch(arrayOf("*/*")) },
                )
            }
        }
    }
}

@Composable
fun PreferencesScreen(
    preferencesViewModel: PreferencesViewModel,
    onOpenFile: () -> Unit,
    showTopBar: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val fileUri by preferencesViewModel.fileUri.observeAsState()
    val state by preferencesViewModel.settingsState.observeAsState()
    state?.let {
        PreferencesScreen(
            state = it,
            callbacks = preferencesViewModel.settingsCallbacks,
            fileUri = fileUri,
            onOpenFile = onOpenFile,
            showTopBar = showTopBar,
            contentPadding = contentPadding,
        )
    }
}

@Composable
fun PreferencesScreen(
    state: SettingsState,
    callbacks: SettingsCallbacks,
    fileUri: Uri?,
    onOpenFile: () -> Unit,
    showTopBar: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val transactionDefaultElements =
        listOfNotNull(
            if (state.transactionStatusPresentByDefault) R.string.status else null,
            if (state.transactionCodePresentByDefault) R.string.code else null,
            if (state.transactionPayeePresentByDefault) R.string.payee else null,
            if (state.transactionNotePresentByDefault) R.string.note else null,
            if (state.transactionCurrenciesPresentByDefault) R.string.currencies else null,
        )
    val postingDefaultElements =
        listOfNotNull(
            if (state.postingAmountPresentByDefault) R.string.amount else null,
            if (state.postingCostPresentByDefault) R.string.cost else null,
            if (state.postingAssertionPresentByDefault) R.string.assertion else null,
            if (state.postingAssertionCostPresentByDefault) R.string.assertion_cost else null,
            if (state.postingCommentPresentByDefault) R.string.comment else null,
        )
    val statusMap =
        mapOf(
            " " to stringResource(R.string.status_unmarked),
            "!" to stringResource(R.string.status_pending),
            "*" to stringResource(R.string.status_cleared),
        )
    val currencyOrderMap =
        mapOf(
            true to stringResource(R.string.currency_order_before),
            false to stringResource(R.string.currency_order_after),
        )
    val currencySpacingMap =
        mapOf(
            true to stringResource(R.string.currency_amount_spacing_on),
            false to stringResource(R.string.currency_amount_spacing_off),
        )
    val separatorMap =
        mapOf(
            "." to stringResource(R.string.separator_point),
            "," to stringResource(R.string.separator_comma),
        )
    val content: @Composable (PaddingValues) -> Unit = { contentPadding ->
        Column(
            modifier =
                Modifier
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState()),
        ) {
            Setting(
                stringResource(R.string.file),
                fileUri?.toString() ?: stringResource(R.string.select_file),
            ) {
                onOpenFile()
            }
            HorizontalDivider()
            var transactionDefaultElementsOpen by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = transactionDefaultElementsOpen,
                onExpandedChange = {
                    transactionDefaultElementsOpen = !transactionDefaultElementsOpen
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Setting(
                    stringResource(R.string.default_transaction_fields),
                    transactionDefaultElements.map { stringResource(it) }.joinToString(", "),
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                ) { transactionDefaultElementsOpen = true }
                ExposedDropdownMenu(
                    expanded = transactionDefaultElementsOpen,
                    onDismissRequest = { transactionDefaultElementsOpen = false },
                    modifier = Modifier.exposedDropdownSize(true),
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (state.transactionStatusPresentByDefault) {
                                        R.string.remove_status
                                    } else {
                                        R.string.add_status
                                    },
                                ),
                            )
                        },
                        onClick = {
                            callbacks.onTransactionStatusPresentByDefaultChange(!state.transactionStatusPresentByDefault)
                            transactionDefaultElementsOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (state.transactionCodePresentByDefault) {
                                        R.string.remove_code
                                    } else {
                                        R.string.add_code
                                    },
                                ),
                            )
                        },
                        onClick = {
                            callbacks.onTransactionCodePresentByDefaultChange(!state.transactionCodePresentByDefault)
                            transactionDefaultElementsOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (state.transactionPayeePresentByDefault) {
                                        R.string.remove_payee
                                    } else {
                                        R.string.add_payee
                                    },
                                ),
                            )
                        },
                        onClick = {
                            callbacks.onTransactionPayeePresentByDefaultChange(!state.transactionPayeePresentByDefault)
                            transactionDefaultElementsOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (state.transactionNotePresentByDefault) {
                                        R.string.remove_note
                                    } else {
                                        R.string.add_note
                                    },
                                ),
                            )
                        },
                        onClick = {
                            callbacks.onTransactionNotePresentByDefaultChange(!state.transactionNotePresentByDefault)
                            transactionDefaultElementsOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (state.transactionCurrenciesPresentByDefault) {
                                        R.string.remove_currency
                                    } else {
                                        R.string.add_currency
                                    },
                                ),
                            )
                        },
                        onClick = {
                            callbacks.onTransactionCurrenciesPresentByDefaultChange(
                                !state.transactionCurrenciesPresentByDefault,
                            )
                            transactionDefaultElementsOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
            HorizontalDivider()
            var postingDefaultElementsOpen by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = postingDefaultElementsOpen,
                onExpandedChange = { postingDefaultElementsOpen = !postingDefaultElementsOpen },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Setting(
                    stringResource(R.string.default_posting_fields),
                    postingDefaultElements.map { stringResource(it) }.joinToString(", "),
                ) { postingDefaultElementsOpen = true }
                ExposedDropdownMenu(
                    expanded = postingDefaultElementsOpen,
                    onDismissRequest = { postingDefaultElementsOpen = false },
                    modifier = Modifier.exposedDropdownSize(true),
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (state.postingAmountPresentByDefault) {
                                        R.string.remove_amount
                                    } else {
                                        R.string.add_amount
                                    },
                                ),
                            )
                        },
                        onClick = {
                            callbacks.onPostingAmountPresentByDefaultChange(!state.postingAmountPresentByDefault)
                            postingDefaultElementsOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (state.postingCostPresentByDefault) {
                                        R.string.remove_cost
                                    } else {
                                        R.string.add_cost
                                    },
                                ),
                            )
                        },
                        onClick = {
                            callbacks.onPostingCostPresentByDefaultChange(!state.postingCostPresentByDefault)
                            postingDefaultElementsOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (state.postingAssertionPresentByDefault) {
                                        R.string.remove_assertion
                                    } else {
                                        R.string.add_assertion
                                    },
                                ),
                            )
                        },
                        onClick = {
                            callbacks.onPostingAssertionPresentByDefaultChange(!state.postingAssertionPresentByDefault)
                            postingDefaultElementsOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (state.postingAssertionCostPresentByDefault) {
                                        R.string.remove_assertion_cost
                                    } else {
                                        R.string.add_assertion_cost
                                    },
                                ),
                            )
                        },
                        onClick = {
                            callbacks.onPostingAssertionCostPresentByDefaultChange(
                                !state.postingAssertionCostPresentByDefault,
                            )
                            postingDefaultElementsOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (state.postingCommentPresentByDefault) {
                                        R.string.remove_comment
                                    } else {
                                        R.string.add_comment
                                    },
                                ),
                            )
                        },
                        onClick = {
                            callbacks.onPostingCommentPresentByDefaultChange(!state.postingCommentPresentByDefault)
                            postingDefaultElementsOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
            HorizontalDivider()
            var newDefaultCurrency by remember { mutableStateOf(state.defaultCurrency) }
            var defaultCurrencyOpen by remember { mutableStateOf(false) }
            Setting(
                stringResource(R.string.default_currency),
                state.defaultCurrency,
            ) {
                defaultCurrencyOpen = true
            }
            SettingDialog(
                defaultCurrencyOpen,
                R.string.change_default_currency,
                true,
                { callbacks.onDefaultCurrencyChange(newDefaultCurrency) },
                { defaultCurrencyOpen = false },
            ) {
                OutlinedTextField(newDefaultCurrency, { newDefaultCurrency = it })
            }
            HorizontalDivider()
            var newPostingWidth by remember { mutableStateOf("${state.postingWidth}") }
            var postingWidthOpen by remember { mutableStateOf(false) }
            Setting(stringResource(R.string.posting_width), "${state.postingWidth}") {
                postingWidthOpen = true
            }
            SettingDialog(postingWidthOpen, R.string.change_posting_width, true, {
                callbacks.onPostingWidthChange(Integer.parseInt(newPostingWidth))
            }, { postingWidthOpen = false }) {
                OutlinedTextField(
                    newPostingWidth,
                    {
                        if (it.isEmpty() || it.matches(Regex("^\\d+$"))) {
                            newPostingWidth = it
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            HorizontalDivider()
            var expandedStatus by rememberSaveable { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedStatus,
                onExpandedChange = { expandedStatus = !expandedStatus },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Setting(
                    stringResource(R.string.default_status),
                    statusMap[state.defaultStatus] ?: stringResource(
                        R.string.status_unmarked,
                    ),
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                ) { expandedStatus = true }
                ExposedDropdownMenu(
                    expanded = expandedStatus,
                    onDismissRequest = { expandedStatus = false },
                    modifier = Modifier.exposedDropdownSize(true),
                ) {
                    statusMap.forEach {
                        DropdownMenuItem(
                            text = { Text(it.value) },
                            onClick = {
                                callbacks.onDefaultStatusChange(it.key)
                                expandedStatus = false
                            },
                            contentPadding =
                                ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }
            HorizontalDivider()
            var expandedSeparator by rememberSaveable { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedSeparator,
                onExpandedChange = { expandedSeparator = !expandedSeparator },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Setting(
                    stringResource(R.string.decimal_separator),
                    separatorMap[state.decimalSeparator] ?: stringResource(
                        R.string.separator_point,
                    ),
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                ) { expandedSeparator = true }
                ExposedDropdownMenu(
                    expanded = expandedSeparator,
                    onDismissRequest = { expandedSeparator = false },
                    modifier = Modifier.exposedDropdownSize(true),
                ) {
                    separatorMap.forEach {
                        DropdownMenuItem(
                            text = { Text(it.value) },
                            onClick = {
                                callbacks.onDecimalSeparatorChange(it.key)
                                expandedSeparator = false
                            },
                            contentPadding =
                                ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }
            HorizontalDivider()
            var expandedCurrency by rememberSaveable { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedCurrency,
                onExpandedChange = { expandedCurrency = !expandedCurrency },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Setting(
                    stringResource(R.string.currency_amount_order),
                    currencyOrderMap[state.currencyBeforeAmount] ?: stringResource(
                        R.string.currency_order_before,
                    ),
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                ) { expandedCurrency = true }
                ExposedDropdownMenu(
                    expanded = expandedCurrency,
                    onDismissRequest = { expandedCurrency = false },
                    modifier = Modifier.exposedDropdownSize(true),
                ) {
                    currencyOrderMap.forEach {
                        DropdownMenuItem(
                            text = { Text(it.value) },
                            onClick = {
                                callbacks.onCurrencyBeforeAmountChange(it.key)
                                expandedCurrency = false
                            },
                            contentPadding =
                                ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }
            HorizontalDivider()
            var expandedCurrencySpacing by rememberSaveable { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedCurrencySpacing,
                onExpandedChange = { expandedCurrencySpacing = !expandedCurrencySpacing },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Setting(
                    stringResource(R.string.currency_amount_spacing),
                    currencySpacingMap[state.currencyAmountSpacing] ?: stringResource(
                        R.string.currency_amount_spacing_on,
                    ),
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                ) { expandedCurrencySpacing = true }
                ExposedDropdownMenu(
                    expanded = expandedCurrencySpacing,
                    onDismissRequest = { expandedCurrencySpacing = false },
                    modifier = Modifier.exposedDropdownSize(true),
                ) {
                    currencySpacingMap.forEach {
                        DropdownMenuItem(
                            text = { Text(it.value) },
                            onClick = {
                                callbacks.onCurrencyAmountSpacingChange(it.key)
                                expandedCurrencySpacing = false
                            },
                            contentPadding =
                                ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }
            HorizontalDivider()
            var assetsPrefixesOpen by remember { mutableStateOf(false) }
            Setting(
                stringResource(R.string.assets_prefixes),
                state.prefixes.assets.joinToString(", "),
            ) { assetsPrefixesOpen = true }
            PrefixListDialog(
                assetsPrefixesOpen,
                R.string.change_assets_prefixes,
                state.prefixes.assets,
                { callbacks.onAssetsPrefixesChange(it) },
                { assetsPrefixesOpen = false },
            )
            HorizontalDivider()
            var liabilitiesPrefixesOpen by remember { mutableStateOf(false) }
            Setting(
                stringResource(R.string.liabilities_prefixes),
                state.prefixes.liabilities.joinToString(", "),
            ) { liabilitiesPrefixesOpen = true }
            PrefixListDialog(
                liabilitiesPrefixesOpen,
                R.string.change_liabilities_prefixes,
                state.prefixes.liabilities,
                { callbacks.onLiabilitiesPrefixesChange(it) },
                { liabilitiesPrefixesOpen = false },
            )
            HorizontalDivider()
            var equityPrefixesOpen by remember { mutableStateOf(false) }
            Setting(
                stringResource(R.string.equity_prefixes),
                state.prefixes.equity.joinToString(", "),
            ) { equityPrefixesOpen = true }
            PrefixListDialog(
                equityPrefixesOpen,
                R.string.change_equity_prefixes,
                state.prefixes.equity,
                { callbacks.onEquityPrefixesChange(it) },
                { equityPrefixesOpen = false },
            )
            HorizontalDivider()
            var incomePrefixesOpen by remember { mutableStateOf(false) }
            Setting(
                stringResource(R.string.income_prefixes),
                state.prefixes.income.joinToString(", "),
            ) { incomePrefixesOpen = true }
            PrefixListDialog(
                incomePrefixesOpen,
                R.string.change_income_prefixes,
                state.prefixes.income,
                { callbacks.onIncomePrefixesChange(it) },
                { incomePrefixesOpen = false },
            )
            HorizontalDivider()
            var expensesPrefixesOpen by remember { mutableStateOf(false) }
            Setting(
                stringResource(R.string.expenses_prefixes),
                state.prefixes.expenses.joinToString(", "),
            ) { expensesPrefixesOpen = true }
            PrefixListDialog(
                expensesPrefixesOpen,
                R.string.change_expenses_prefixes,
                state.prefixes.expenses,
                { callbacks.onExpensesPrefixesChange(it) },
                { expensesPrefixesOpen = false },
            )
        }
    }
    if (showTopBar) {
        Scaffold(topBar = { Bar() }, modifier = Modifier.imePadding()) { scaffoldPadding ->
            content(scaffoldPadding)
        }
    } else {
        content(contentPadding)
    }
}

@Composable
fun Setting(
    text: String,
    subtext: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    var localModifier = modifier.fillMaxWidth()
    if (onClick != null) {
        localModifier = localModifier.clickable(onClick = onClick)
    }
    Column(modifier = localModifier) {
        Text(
            text,
            modifier =
                Modifier.padding(
                    top = 8.dp,
                    start = 8.dp,
                    end = 8.dp,
                    bottom = 0.dp,
                ),
        )
        Text(
            subtext,
            modifier = Modifier.padding(bottom = 8.dp, start = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
        )
    }
}

@Composable
fun Bar() {
    val context = LocalContext.current
    TopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        navigationIcon = {
            IconButton(onClick = { (context as Activity).finish() }) {
                Icon(
                    Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    )
}

@Composable
fun SettingDialog(
    opened: Boolean,
    titleRes: Int,
    canSave: Boolean,
    save: (() -> Unit),
    dismiss: (() -> Unit),
    content: @Composable () -> Unit,
) {
    if (opened) {
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(stringResource(titleRes), style = MaterialTheme.typography.titleLarge) },
            text = content,
            dismissButton = {
                TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                TextButton(onClick = {
                    save()
                    dismiss()
                }, enabled = canSave) {
                    Text(stringResource(R.string.save))
                }
            },
        )
    }
}

@Composable
fun PrefixListDialog(
    opened: Boolean,
    titleRes: Int,
    prefixes: List<String>,
    save: (List<String>) -> Unit,
    dismiss: () -> Unit,
) {
    if (opened) {
        val title = stringResource(titleRes)
        val currentPrefixes =
            remember(prefixes) { mutableStateListOf<String>().apply { addAll(prefixes) } }
        var newPrefix by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(title, style = MaterialTheme.typography.titleLarge) },
            text = {
                PrefixListDialogContent(
                    currentPrefixes = currentPrefixes,
                    newPrefix = newPrefix,
                    onNewPrefixChange = { newPrefix = it },
                    onAddPrefix = {
                        if (newPrefix.isNotBlank()) {
                            currentPrefixes.add(newPrefix.trim())
                            newPrefix = ""
                        }
                    },
                    onRemovePrefix = { currentPrefixes.remove(it) },
                )
            },
            dismissButton = {
                TextButton(onClick = dismiss) { Text(stringResource(R.string.cancel)) }
            },
            confirmButton = {
                TextButton(onClick = {
                    save(currentPrefixes.toList())
                    dismiss()
                }) {
                    Text(stringResource(R.string.save))
                }
            },
        )
    }
}

@Composable
fun PrefixListDialogContent(
    currentPrefixes: List<String>,
    newPrefix: String,
    onNewPrefixChange: (String) -> Unit,
    onAddPrefix: () -> Unit,
    onRemovePrefix: (String) -> Unit,
) {
    Column {
        LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
            items(currentPrefixes) { prefix ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(prefix, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onRemovePrefix(prefix) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.remove_account),
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                newPrefix,
                onNewPrefixChange,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = onAddPrefix,
                enabled = newPrefix.isNotBlank(),
            ) {
                Text(stringResource(R.string.add_prefix))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreferencesScreenPreview() {
    MicroLedgerTheme {
        PreferencesScreen(
            state = SettingsState(),
            callbacks = SettingsCallbacks(),
            fileUri = null,
            onOpenFile = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PrefixListDialogPreview() {
    MicroLedgerTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            PrefixListDialogContent(
                currentPrefixes = listOf("Assets", "Cash", "Bank"),
                newPrefix = "",
                onNewPrefixChange = {},
                onAddPrefix = {},
                onRemovePrefix = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PrefixListDialogSingleItemPreview() {
    MicroLedgerTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            PrefixListDialogContent(
                currentPrefixes = listOf("Assets"),
                newPrefix = "",
                onNewPrefixChange = {},
                onAddPrefix = {},
                onRemovePrefix = {},
            )
        }
    }
}
