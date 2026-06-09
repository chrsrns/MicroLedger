package be.chvp.nanoledger.ui.preferences

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import be.chvp.nanoledger.R
import be.chvp.nanoledger.ui.theme.NanoLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

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
            NanoLedgerTheme {
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
    val transactionDefaultElements by preferencesViewModel.transactionDefaultElements.observeAsState(
        emptyList()
    )
    val transactionStatusPresentByDefault by preferencesViewModel.transactionStatusPresentByDefault.observeAsState(
        true
    )
    val transactionCodePresentByDefault by preferencesViewModel.transactionCodePresentByDefault.observeAsState(
        false
    )
    val transactionPayeePresentByDefault by preferencesViewModel.transactionPayeePresentByDefault.observeAsState(
        true
    )
    val transactionNotePresentByDefault by preferencesViewModel.transactionNotePresentByDefault.observeAsState(
        true
    )
    val transactionCurrenciesPresentByDefault by preferencesViewModel.transactionCurrenciesPresentByDefault.observeAsState(
        true
    )
    val postingDefaultElements by preferencesViewModel.postingDefaultElements.observeAsState(
        emptyList()
    )
    val postingAmountPresentByDefault by preferencesViewModel.postingAmountPresentByDefault.observeAsState(
        true
    )
    val postingCostPresentByDefault by preferencesViewModel.postingCostPresentByDefault.observeAsState(
        false
    )
    val postingAssertionPresentByDefault by preferencesViewModel.postingAssertionPresentByDefault.observeAsState(
        false
    )
    val postingAssertionCostPresentByDefault by preferencesViewModel.postingAssertionCostPresentByDefault.observeAsState(
        false
    )
    val postingCommentPresentByDefault by preferencesViewModel.postingCommentPresentByDefault.observeAsState(
        false
    )
    val defaultCurrency by preferencesViewModel.defaultCurrency.observeAsState("€")
    val postingWidth by preferencesViewModel.postingWidth.observeAsState(72)
    val defaultStatus by preferencesViewModel.defaultStatus.observeAsState(" ")
    val decimalSeparator by preferencesViewModel.decimalSeparator.observeAsState(".")
    val currencyBeforeAmount by preferencesViewModel.currencyBeforeAmount.observeAsState(true)
    val currencyAmountSpacing by preferencesViewModel.spacingBetweenCurrencyAndAmount.observeAsState(
        true
    )

    PreferencesScreen(
        fileUri = fileUri,
        onOpenFile = onOpenFile,
        transactionDefaultElements = transactionDefaultElements,
        transactionStatusPresentByDefault = transactionStatusPresentByDefault,
        onTransactionStatusPresentByDefaultChange = {
            preferencesViewModel.storeTransactionStatusPresentByDefault(it)
        },
        transactionCodePresentByDefault = transactionCodePresentByDefault,
        onTransactionCodePresentByDefaultChange = {
            preferencesViewModel.storeTransactionCodePresentByDefault(it)
        },
        transactionPayeePresentByDefault = transactionPayeePresentByDefault,
        onTransactionPayeePresentByDefaultChange = {
            preferencesViewModel.storeTransactionPayeePresentByDefault(it)
        },
        transactionNotePresentByDefault = transactionNotePresentByDefault,
        onTransactionNotePresentByDefaultChange = {
            preferencesViewModel.storeTransactionNotePresentByDefault(it)
        },
        transactionCurrenciesPresentByDefault = transactionCurrenciesPresentByDefault,
        onTransactionCurrenciesPresentByDefaultChange = {
            preferencesViewModel.storeTransactionCurrenciesPresentByDefault(it)
        },
        postingDefaultElements = postingDefaultElements,
        postingAmountPresentByDefault = postingAmountPresentByDefault,
        onPostingAmountPresentByDefaultChange = {
            preferencesViewModel.storePostingAmountPresentByDefault(it)
        },
        postingCostPresentByDefault = postingCostPresentByDefault,
        onPostingCostPresentByDefaultChange = {
            preferencesViewModel.storePostingCostPresentByDefault(it)
        },
        postingAssertionPresentByDefault = postingAssertionPresentByDefault,
        onPostingAssertionPresentByDefaultChange = {
            preferencesViewModel.storePostingAssertionPresentByDefault(it)
        },
        postingAssertionCostPresentByDefault = postingAssertionCostPresentByDefault,
        onPostingAssertionCostPresentByDefaultChange = {
            preferencesViewModel.storePostingAssertionCostPresentByDefault(it)
        },
        postingCommentPresentByDefault = postingCommentPresentByDefault,
        onPostingCommentPresentByDefaultChange = {
            preferencesViewModel.storePostingCommentPresentByDefault(it)
        },
        defaultCurrency = defaultCurrency,
        onDefaultCurrencyChange = { preferencesViewModel.storeDefaultCurrency(it) },
        postingWidth = postingWidth,
        onPostingWidthChange = { preferencesViewModel.storePostingWidth(it) },
        defaultStatus = defaultStatus,
        onDefaultStatusChange = { preferencesViewModel.storeDefaultStatus(it) },
        decimalSeparator = decimalSeparator,
        onDecimalSeparatorChange = { preferencesViewModel.storeDecimalSeparator(it) },
        currencyBeforeAmount = currencyBeforeAmount,
        onCurrencyBeforeAmountChange = { preferencesViewModel.storeCurrencyBeforeAmount(it) },
        currencyAmountSpacing = currencyAmountSpacing,
        onCurrencyAmountSpacingChange = { preferencesViewModel.storeCurrencyAmountSpacing(it) },
        showTopBar = showTopBar,
        contentPadding = contentPadding,
    )
}

@Composable
fun PreferencesScreen(
    fileUri: Uri?,
    onOpenFile: () -> Unit,
    transactionDefaultElements: List<Int>,
    transactionStatusPresentByDefault: Boolean,
    onTransactionStatusPresentByDefaultChange: (Boolean) -> Unit,
    transactionCodePresentByDefault: Boolean,
    onTransactionCodePresentByDefaultChange: (Boolean) -> Unit,
    transactionPayeePresentByDefault: Boolean,
    onTransactionPayeePresentByDefaultChange: (Boolean) -> Unit,
    transactionNotePresentByDefault: Boolean,
    onTransactionNotePresentByDefaultChange: (Boolean) -> Unit,
    transactionCurrenciesPresentByDefault: Boolean,
    onTransactionCurrenciesPresentByDefaultChange: (Boolean) -> Unit,
    postingDefaultElements: List<Int>,
    postingAmountPresentByDefault: Boolean,
    onPostingAmountPresentByDefaultChange: (Boolean) -> Unit,
    postingCostPresentByDefault: Boolean,
    onPostingCostPresentByDefaultChange: (Boolean) -> Unit,
    postingAssertionPresentByDefault: Boolean,
    onPostingAssertionPresentByDefaultChange: (Boolean) -> Unit,
    postingAssertionCostPresentByDefault: Boolean,
    onPostingAssertionCostPresentByDefaultChange: (Boolean) -> Unit,
    postingCommentPresentByDefault: Boolean,
    onPostingCommentPresentByDefaultChange: (Boolean) -> Unit,
    defaultCurrency: String,
    onDefaultCurrencyChange: (String) -> Unit,
    postingWidth: Int,
    onPostingWidthChange: (Int) -> Unit,
    defaultStatus: String,
    onDefaultStatusChange: (String) -> Unit,
    decimalSeparator: String,
    onDecimalSeparatorChange: (String) -> Unit,
    currencyBeforeAmount: Boolean,
    onCurrencyBeforeAmountChange: (Boolean) -> Unit,
    currencyAmountSpacing: Boolean,
    onCurrencyAmountSpacingChange: (Boolean) -> Unit,
    showTopBar: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
) {
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
                                    if (transactionStatusPresentByDefault) {
                                        R.string.remove_status
                                    } else {
                                        R.string.add_status
                                    },
                                ),
                            )
                        },
                        onClick = {
                            onTransactionStatusPresentByDefaultChange(!transactionStatusPresentByDefault)
                            transactionDefaultElementsOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (transactionCodePresentByDefault) {
                                        R.string.remove_code
                                    } else {
                                        R.string.add_code
                                    },
                                ),
                            )
                        },
                        onClick = {
                            onTransactionCodePresentByDefaultChange(!transactionCodePresentByDefault)
                            transactionDefaultElementsOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (transactionPayeePresentByDefault) {
                                        R.string.remove_payee
                                    } else {
                                        R.string.add_payee
                                    },
                                ),
                            )
                        },
                        onClick = {
                            onTransactionPayeePresentByDefaultChange(!transactionPayeePresentByDefault)
                            transactionDefaultElementsOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (transactionNotePresentByDefault) {
                                        R.string.remove_note
                                    } else {
                                        R.string.add_note
                                    },
                                ),
                            )
                        },
                        onClick = {
                            onTransactionNotePresentByDefaultChange(!transactionNotePresentByDefault)
                            transactionDefaultElementsOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (transactionCurrenciesPresentByDefault) {
                                        R.string.remove_currency
                                    } else {
                                        R.string.add_currency
                                    },
                                ),
                            )
                        },
                        onClick = {
                            onTransactionCurrenciesPresentByDefaultChange(
                                !transactionCurrenciesPresentByDefault,
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
                                    if (postingAmountPresentByDefault) {
                                        R.string.remove_amount
                                    } else {
                                        R.string.add_amount
                                    },
                                ),
                            )
                        },
                        onClick = {
                            onPostingAmountPresentByDefaultChange(!postingAmountPresentByDefault)
                            postingDefaultElementsOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (postingCostPresentByDefault) {
                                        R.string.remove_cost
                                    } else {
                                        R.string.add_cost
                                    },
                                ),
                            )
                        },
                        onClick = {
                            onPostingCostPresentByDefaultChange(!postingCostPresentByDefault)
                            postingDefaultElementsOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (postingAssertionPresentByDefault) {
                                        R.string.remove_assertion
                                    } else {
                                        R.string.add_assertion
                                    },
                                ),
                            )
                        },
                        onClick = {
                            onPostingAssertionPresentByDefaultChange(!postingAssertionPresentByDefault)
                            postingDefaultElementsOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (postingAssertionCostPresentByDefault) {
                                        R.string.remove_assertion_cost
                                    } else {
                                        R.string.add_assertion_cost
                                    },
                                ),
                            )
                        },
                        onClick = {
                            onPostingAssertionCostPresentByDefaultChange(
                                !postingAssertionCostPresentByDefault,
                            )
                            postingDefaultElementsOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (postingCommentPresentByDefault) {
                                        R.string.remove_comment
                                    } else {
                                        R.string.add_comment
                                    },
                                ),
                            )
                        },
                        onClick = {
                            onPostingCommentPresentByDefaultChange(!postingCommentPresentByDefault)
                            postingDefaultElementsOpen = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
            HorizontalDivider()
            var newDefaultCurrency by remember { mutableStateOf(defaultCurrency) }
            var defaultCurrencyOpen by remember { mutableStateOf(false) }
            Setting(
                stringResource(R.string.default_currency),
                defaultCurrency,
            ) {
                defaultCurrencyOpen = true
            }
            SettingDialog(
                defaultCurrencyOpen,
                stringResource(R.string.change_default_currency),
                true,
                { onDefaultCurrencyChange(newDefaultCurrency) },
                { defaultCurrencyOpen = false },
            ) {
                OutlinedTextField(newDefaultCurrency, { newDefaultCurrency = it })
            }
            HorizontalDivider()
            var newPostingWidth by remember { mutableStateOf("$postingWidth") }
            var postingWidthOpen by remember { mutableStateOf(false) }
            Setting(stringResource(R.string.posting_width), "$postingWidth") {
                postingWidthOpen = true
            }
            SettingDialog(postingWidthOpen, stringResource(R.string.change_posting_width), true, {
                onPostingWidthChange(Integer.parseInt(newPostingWidth))
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
                    statusMap[defaultStatus] ?: stringResource(
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
                                onDefaultStatusChange(it.key)
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
                    separatorMap[decimalSeparator] ?: stringResource(
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
                                onDecimalSeparatorChange(it.key)
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
                    currencyOrderMap[currencyBeforeAmount] ?: stringResource(
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
                                onCurrencyBeforeAmountChange(it.key)
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
                    currencySpacingMap[currencyAmountSpacing] ?: stringResource(
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
                                onCurrencyAmountSpacingChange(it.key)
                                expandedCurrencySpacing = false
                            },
                            contentPadding =
                                ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }
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
    title: String,
    canSave: Boolean,
    save: (() -> Unit),
    dismiss: (() -> Unit),
    content: @Composable () -> Unit,
) {
    if (opened) {
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(title, style = MaterialTheme.typography.titleLarge) },
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

@Preview(showBackground = true)
@Composable
fun PreferencesScreenPreview() {
    NanoLedgerTheme {
        PreferencesScreen(
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
        )
    }
}
