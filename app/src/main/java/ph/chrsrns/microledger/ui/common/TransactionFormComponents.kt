package ph.chrsrns.microledger.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ph.chrsrns.microledger.R
import ph.chrsrns.microledger.data.Amount
import ph.chrsrns.microledger.data.CostType
import ph.chrsrns.microledger.data.Posting
import ph.chrsrns.microledger.ui.theme.MicroLedgerTheme

const val TRANSACTION_INDEX_KEY = "transaction_index"

@Composable
fun TransactionForm(
    viewModel: TransactionFormViewModel,
    contentPadding: PaddingValues,
    bottomOffset: Dp,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val latestError by viewModel.latestError.observeAsState()
    val showMessage = stringResource(R.string.show)
    var openErrorDialog by rememberSaveable { mutableStateOf(false) }

    val errorMessage = stringResource(R.string.error_writing_file)
    var errorDialogMessage by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(latestError) {
        val error = latestError?.get()
        if (error != null) {
            Log.e("ph.chrsrns.microledger", "Exception while writing file", error)
            scope.launch {
                val result =
                    snackbarHostState.showSnackbar(
                        message = errorMessage,
                        actionLabel = showMessage,
                        duration = SnackbarDuration.Long,
                    )
                if (result == SnackbarResult.ActionPerformed) {
                    openErrorDialog = true
                    errorDialogMessage = error.stackTraceToString()
                }
            }
        }
    }

    val latestMismatch by viewModel.latestMismatch.observeAsState()
    val mismatchMessage = stringResource(R.string.mismatch_no_write)
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

    Box(
        modifier =
            Modifier
                .padding(contentPadding)
                .fillMaxSize(),
    ) {
        if (openErrorDialog) {
            AlertDialog(
                onDismissRequest = { openErrorDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip: ClipData =
                            ClipData.newPlainText("simple text", errorDialogMessage)
                        clipboard.setPrimaryClip(clip)
                    }) { Text(stringResource(R.string.copy)) }
                },
                title = { Text(stringResource(R.string.error)) },
                text = { Text(errorDialogMessage) },
                dismissButton = {
                    TextButton(onClick = {
                        openErrorDialog = false
                    }) { Text(stringResource(R.string.dismiss)) }
                },
            )
        }
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 2.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            with(LocalDensity.current) {
                FlowRow(
                    modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.Bottom,
                ) {
                    DateSelector(
                        viewModel,
                        Modifier
                            .weight(0.25f)
                            .width((8 * 16).sp.toDp()),
                    )
                    val status by viewModel.status.observeAsState()
                    StatusSelector(
                        status,
                        { viewModel.setStatus(it) },
                        Modifier.width((3 * 16).sp.toDp())
                    )
                    val code by viewModel.code.observeAsState()
                    CodeField(
                        code,
                        { viewModel.setCode(it) },
                        Modifier
                            .weight(0.5f)
                            .width((16 * 16).sp.toDp()),
                    )
                    val payee by viewModel.payee.observeAsState()
                    val options by viewModel.possiblePayees.observeAsState()
                    PayeeSelector(
                        payee,
                        options ?: emptyList(),
                        { viewModel.setPayee(it) },
                        Modifier
                            .weight(0.5f)
                            .width((16 * 16).sp.toDp()),
                    )
                    val note by viewModel.note.observeAsState()
                    val possibleNotes by viewModel.possibleNotes.observeAsState()
                    NoteSelector(
                        note,
                        possibleNotes ?: emptyList(),
                        { viewModel.setNote(it) },
                        Modifier
                            .weight(0.75f)
                            .width((16 * 16).sp.toDp()),
                    )
                }
                val postings by viewModel.postings.observeAsState()
                postings?.forEachIndexed { i, posting ->
                    HorizontalDivider(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    )
                    PostingRow(i, posting, posting.isEmpty(), viewModel)
                }
            }
            Box(
                Modifier
                    .height(bottomOffset)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
fun FieldSelector(
    viewModel: TransactionFormViewModel,
    onTemplateCreated: ((String) -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    val status by viewModel.status.observeAsState()
    val code by viewModel.code.observeAsState()
    val payee by viewModel.payee.observeAsState()
    val note by viewModel.note.observeAsState()
    val currencyEnabled by viewModel.currencyEnabled.observeAsState(true)

    if (showSaveTemplateDialog) {
        SaveAsTemplateDialog(
            viewModel = viewModel,
            initialName = payee ?: "",
            onDismiss = { showSaveTemplateDialog = false },
            onTemplateCreated = onTemplateCreated,
        )
    }

    Box {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(
                Icons.Default.EditNote,
                contentDescription = stringResource(R.string.change_transaction_fields),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (status != null) R.string.remove_status else R.string.add_status,
                        ),
                    )
                },
                onClick = {
                    viewModel.toggleStatus()
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (code != null) R.string.remove_code else R.string.add_code,
                        ),
                    )
                },
                onClick = {
                    viewModel.toggleCode()
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (payee != null) R.string.remove_payee else R.string.add_payee,
                        ),
                    )
                },
                onClick = {
                    viewModel.togglePayee()
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (note != null) R.string.remove_note else R.string.add_note,
                        ),
                    )
                },
                onClick = {
                    viewModel.toggleNote()
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (currencyEnabled) R.string.remove_currency else R.string.add_currency,
                        ),
                    )
                },
                onClick = {
                    viewModel.toggleCurrency()
                    expanded = false
                },
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = { Text(stringResource(R.string.save_as_template)) },
                onClick = {
                    showSaveTemplateDialog = true
                    expanded = false
                },
            )
        }
    }
}

@Composable
fun SaveAsTemplateDialog(
    viewModel: TransactionFormViewModel,
    initialName: String,
    onDismiss: () -> Unit,
    onTemplateCreated: ((String) -> Unit)? = null,
) {
    var templateName by remember { mutableStateOf(initialName) }
    val scope = rememberCoroutineScope()
    val saving by viewModel.saving.observeAsState(false)

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(R.string.save_as_template)) },
        text = {
            OutlinedTextField(
                value = templateName,
                onValueChange = { templateName = it },
                label = { Text(stringResource(R.string.template_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (templateName.isNotBlank()) {
                        scope.launch {
                            viewModel.saveAsTemplate(templateName) { templateId ->
                                onDismiss()
                                if (templateId != null) {
                                    onTemplateCreated?.invoke(templateId)
                                }
                            }
                        }
                    }
                },
                enabled = templateName.isNotBlank() && !saving,
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() },
                enabled = !saving,
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun DateSelector(
    viewModel: TransactionFormViewModel,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val date by viewModel.date.observeAsState()
    val formattedDate by viewModel.formattedDate.observeAsState()
    var dateDialogOpen by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = formattedDate ?: "",
        readOnly = true,
        singleLine = true,
        onValueChange = {},
        label = {
            Text(
                stringResource(R.string.date),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors =
            ExposedDropdownMenuDefaults.textFieldColors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        modifier =
            modifier.onFocusChanged {
                if (it.isFocused) {
                    dateDialogOpen = true
                }
            },
    )
    if (dateDialogOpen) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date?.time)
        DatePickerDialog(
            onDismissRequest = { dateDialogOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.setDate(it) }
                    dateDialogOpen = false
                    focusManager.clearFocus()
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun StatusSelector(
    status: String?,
    onStatusChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(" ", "!", "*")
    var expanded by rememberSaveable { mutableStateOf(false) }
    if (status != null) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = modifier,
        ) {
            OutlinedTextField(
                value = status,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                colors =
                    ExposedDropdownMenuDefaults.textFieldColors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.exposedDropdownSize(true),
            ) {
                options.forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            onStatusChange(it)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

@Composable
fun CodeField(
    code: String?,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (code != null) {
        OutlinedTextField(
            code,
            onValueChange = onCodeChange,
            modifier = modifier,
            label = {
                Text(
                    stringResource(R.string.code),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
        )
    }
}

@Composable
fun PayeeSelector(
    payee: String?,
    options: List<String>,
    onPayeeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (payee != null) {
        OutlinedLooseDropdown(
            options,
            payee,
            onPayeeChange,
            modifier,
        ) { Text(stringResource(R.string.payee), maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
fun NoteSelector(
    note: String?,
    options: List<String>,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (note != null) {
        OutlinedLooseDropdown(
            options,
            note,
            onNoteChange,
            modifier,
        ) { Text(stringResource(R.string.note), maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
fun PostingRow(
    posting: Posting,
    showAmountHint: Boolean,
    accounts: List<String>,
    currencyEnabled: Boolean,
    currencyBeforeAmount: Boolean,
    unbalancedAmount: String?,
    onCommentChange: (String) -> Unit,
    onAccountChange: (String) -> Unit,
    onRemovePosting: () -> Unit,
    onCurrencyChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCostTypeChange: (CostType) -> Unit,
    onCostCurrencyChange: (String) -> Unit,
    onCostAmountChange: (String) -> Unit,
    onAssertionCurrencyChange: (String) -> Unit,
    onAssertionAmountChange: (String) -> Unit,
    onAssertionCostTypeChange: (CostType) -> Unit,
    onAssertionCostCurrencyChange: (String) -> Unit,
    onAssertionCostAmountChange: (String) -> Unit,
    onToggleAccount: (Boolean) -> Unit,
    onToggleAmount: (Boolean) -> Unit,
    onToggleCost: (Boolean) -> Unit,
    onToggleAssertion: (Boolean) -> Unit,
    onToggleAssertionCost: (Boolean) -> Unit,
    onToggleComment: (Boolean) -> Unit,
) {
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(4.dp),
        itemVerticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        with(LocalDensity.current) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (posting.isComment()) {
                    CommentField(posting.comment ?: "", onCommentChange, Modifier.weight(1.0f))
                } else {
                    AccountSelector(
                        posting.account ?: "",
                        accounts,
                        onAccountChange,
                        Modifier.weight(1.0f)
                    )
                }
                PostingFieldSelector(
                    posting,
                    onToggleAccount,
                    onToggleAmount,
                    onToggleCost,
                    onToggleAssertion,
                    onToggleAssertionCost,
                    onToggleComment,
                )
                IconButton(onClick = onRemovePosting) {
                    Icon(
                        Icons.Default.RemoveCircleOutline,
                        contentDescription = stringResource(R.string.remove_posting)
                    )
                }
            }
            if (posting.amount != null) {
                CurrencyAndAmountFields(
                    posting.amount.currency,
                    posting.amount.quantity,
                    currencyEnabled,
                    currencyBeforeAmount,
                    showAmountHint,
                    unbalancedAmount,
                    onCurrencyChange,
                    onAmountChange,
                    Modifier.weight(1.0f),
                )
                if (posting.cost != null) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier =
                            Modifier
                                .width((20 * 16).sp.toDp())
                                .weight(1.0f),
                    ) {
                        CostTypeSelector(posting.cost.type, onCostTypeChange)
                        CurrencyAndAmountFields(
                            posting.cost.amount.currency,
                            posting.cost.amount.quantity,
                            currencyEnabled,
                            currencyBeforeAmount,
                            false,
                            null,
                            onCostCurrencyChange,
                            onCostAmountChange,
                            Modifier
                                .weight(1.0f)
                                .padding(start = 4.dp),
                        )
                    }
                }
            }
            if (posting.assertion != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .width((20 * 16).sp.toDp())
                            .weight(1.0f),
                ) {
                    Text("=", modifier = Modifier.padding(horizontal = 4.dp))
                    CurrencyAndAmountFields(
                        posting.assertion.currency,
                        posting.assertion.quantity,
                        currencyEnabled,
                        currencyBeforeAmount,
                        false,
                        null,
                        onAssertionCurrencyChange,
                        onAssertionAmountChange,
                        Modifier
                            .weight(1.0f)
                            .padding(start = 4.dp),
                    )
                }
                if (posting.assertionCost != null) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier =
                            Modifier
                                .width((20 * 16).sp.toDp())
                                .weight(1.0f),
                    ) {
                        CostTypeSelector(posting.assertionCost.type, onAssertionCostTypeChange)
                        CurrencyAndAmountFields(
                            posting.assertionCost.amount.currency,
                            posting.assertionCost.amount.quantity,
                            currencyEnabled,
                            currencyBeforeAmount,
                            false,
                            null,
                            onAssertionCostCurrencyChange,
                            onAssertionCostAmountChange,
                            Modifier
                                .weight(1.0f)
                                .padding(start = 4.dp),
                        )
                    }
                }
            }
            if (!posting.isComment() && posting.comment != null) {
                CommentField(posting.comment, onCommentChange, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun PostingRow(
    index: Int,
    posting: Posting,
    showAmountHint: Boolean,
    viewModel: TransactionFormViewModel,
) {
    val accounts by viewModel.accounts.observeAsState()
    val currencyEnabled by viewModel.currencyEnabled.observeAsState(true)
    val currencyBeforeAmount by viewModel.currencyBeforeAmount.observeAsState(true)
    val unbalancedAmount by viewModel.unbalancedAmount.observeAsState()

    PostingRow(
        posting,
        showAmountHint,
        accounts ?: emptyList(),
        currencyEnabled,
        currencyBeforeAmount,
        unbalancedAmount,
        { viewModel.setComment(index, it) },
        { viewModel.setAccount(index, it) },
        { viewModel.removePosting(index) },
        { viewModel.setCurrency(index, it) },
        { viewModel.setAmount(index, it) },
        { viewModel.setCostType(index, it) },
        { viewModel.setCostCurrency(index, it) },
        { viewModel.setCostAmount(index, it) },
        { viewModel.setAssertionCurrency(index, it) },
        { viewModel.setAssertionAmount(index, it) },
        { viewModel.setAssertionCostType(index, it) },
        { viewModel.setAssertionCostCurrency(index, it) },
        { viewModel.setAssertionCostAmount(index, it) },
        { viewModel.toggleAccount(index, it) },
        { viewModel.toggleAmount(index, it) },
        { viewModel.toggleCost(index, it) },
        { viewModel.toggleAssertion(index, it) },
        { viewModel.toggleAssertionCost(index, it) },
        { viewModel.toggleComment(index, it) },
    )
}

@Composable
fun PostingFieldSelector(
    posting: Posting,
    onToggleAccount: (Boolean) -> Unit,
    onToggleAmount: (Boolean) -> Unit,
    onToggleCost: (Boolean) -> Unit,
    onToggleAssertion: (Boolean) -> Unit,
    onToggleAssertionCost: (Boolean) -> Unit,
    onToggleComment: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(
                Icons.Default.EditNote,
                contentDescription = stringResource(R.string.change_posting_fields),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (!posting.isComment()) R.string.remove_account else R.string.add_account,
                        ),
                    )
                },
                onClick = {
                    onToggleAccount(posting.account == null)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (posting.amount != null) R.string.remove_amount else R.string.add_amount,
                        ),
                    )
                },
                onClick = {
                    onToggleAmount(posting.amount == null)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (posting.cost != null) R.string.remove_cost else R.string.add_cost,
                        ),
                    )
                },
                onClick = {
                    onToggleCost(posting.cost == null)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (posting.assertion != null) R.string.remove_assertion else R.string.add_assertion,
                        ),
                    )
                },
                onClick = {
                    onToggleAssertion(posting.assertion == null)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (posting.assertionCost != null) R.string.remove_assertion_cost else R.string.add_assertion_cost,
                        ),
                    )
                },
                onClick = {
                    onToggleAssertionCost(posting.assertionCost == null)
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (posting.comment != null) R.string.remove_comment else R.string.add_comment,
                        ),
                    )
                },
                onClick = {
                    onToggleComment(posting.comment == null)
                    expanded = false
                },
            )
        }
    }
}

@Composable
fun PostingFieldSelector(
    viewModel: TransactionFormViewModel,
    index: Int,
    posting: Posting,
) {
    PostingFieldSelector(
        posting,
        { viewModel.toggleAccount(index, it) },
        { viewModel.toggleAmount(index, it) },
        { viewModel.toggleCost(index, it) },
        { viewModel.toggleAssertion(index, it) },
        { viewModel.toggleAssertionCost(index, it) },
        { viewModel.toggleComment(index, it) },
    )
}

@Composable
fun CommentField(
    comment: String,
    onCommentChange: (String) -> Unit,
    modifier: Modifier,
) {
    OutlinedTextField(
        comment,
        onValueChange = onCommentChange,
        singleLine = true,
        label = {
            Text(
                stringResource(R.string.comment),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier,
    )
}

@Composable
fun CostTypeSelector(
    costType: CostType,
    save: (newCostType: CostType) -> Unit,
) {
    val options = listOf(CostType.UNIT, CostType.TOTAL)
    var expanded by rememberSaveable { mutableStateOf(false) }
    with(LocalDensity.current) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.width((5 * 16).sp.toDp()),
        ) {
            OutlinedTextField(
                value = costType.repr,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                colors =
                    ExposedDropdownMenuDefaults.textFieldColors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.exposedDropdownSize(true),
            ) {
                options.forEach {
                    DropdownMenuItem(
                        text = { Text(it.repr) },
                        onClick = {
                            save(it)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

@Composable
fun CurrencyAndAmountFields(
    currency: String,
    quantity: String,
    currencyEnabled: Boolean,
    currencyBeforeAmount: Boolean,
    showAmountHint: Boolean,
    unbalancedAmount: String?,
    onCurrencyChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    modifier: Modifier,
) {
    with(LocalDensity.current) {
        Row(modifier = modifier.width((15 * 16).sp.toDp()), verticalAlignment = Alignment.Bottom) {
            if (currencyEnabled && currencyBeforeAmount) {
                CurrencyField(currency, onCurrencyChange, Modifier.padding(end = 4.dp))
            }

            AmountField(
                quantity,
                showAmountHint,
                unbalancedAmount,
                onAmountChange,
                Modifier.weight(1f)
            )

            if (currencyEnabled && !currencyBeforeAmount) {
                CurrencyField(currency, onCurrencyChange, Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
fun CurrencyField(
    currency: String,
    onCurrencyChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    with(LocalDensity.current) {
        OutlinedTextField(
            value = currency,
            onValueChange = onCurrencyChange,
            singleLine = true,
            modifier = modifier.width((6 * 16).sp.toDp()),
            colors =
                ExposedDropdownMenuDefaults.textFieldColors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
        )
    }
}

@Composable
fun AmountField(
    quantity: String,
    showAmountHint: Boolean,
    unbalancedAmount: String?,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = quantity,
        onValueChange = onAmountChange,
        singleLine = true,
        colors =
            ExposedDropdownMenuDefaults.textFieldColors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        label = {
            if (showAmountHint && (unbalancedAmount ?: "") != "") {
                Text(
                    unbalancedAmount!!,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    stringResource(R.string.amount),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        modifier = modifier,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
    )
}

@Composable
fun AccountSelector(
    value: String,
    options: List<String>,
    onAccountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filteredOptions = options.filter { it.contains(value, ignoreCase = true) }
    OutlinedLooseDropdown(filteredOptions, value, onAccountChange, modifier) {
        Text(stringResource(R.string.account), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun OutlinedLooseDropdown(
    options: List<String>,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                if (it.length > value.length) {
                    expanded = true
                }
                onValueChange(it)
            },
            singleLine = true,
            label = content,
            modifier =
                Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (!it.hasFocus) {
                            expanded = false
                        }
                    },
            colors =
                ExposedDropdownMenuDefaults.textFieldColors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
        )
        if (shouldShowDropdown(options, value)) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.exposedDropdownSize(true),
            ) {
                options.forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            onValueChange(it)
                            focusManager.clearFocus()
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }
    }
}

fun shouldShowDropdown(
    options: List<String>,
    currentValue: String,
): Boolean = options.size > 1 || (options.size == 1 && options[0] != currentValue)

@Preview(showBackground = true)
@Composable
fun StatusSelectorPreview() {
    MicroLedgerTheme {
        StatusSelector(status = "*", onStatusChange = {})
    }
}

@Preview(showBackground = true)
@Composable
fun CodeFieldPreview() {
    MicroLedgerTheme {
        CodeField(code = "123", onCodeChange = {})
    }
}

@Preview(showBackground = true)
@Composable
fun PayeeSelectorPreview() {
    MicroLedgerTheme {
        PayeeSelector(
            payee = "Sample Payee",
            options = listOf("Sample Payee", "Another Payee"),
            onPayeeChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NoteSelectorPreview() {
    MicroLedgerTheme {
        NoteSelector(
            note = "Sample Note",
            options = listOf("Sample Note", "Another Note"),
            onNoteChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PostingRowPreview() {
    MicroLedgerTheme {
        PostingRow(
            posting =
                Posting(
                    account = "assets:checking",
                    amount = Amount("-10.00", "EUR", "EUR -10.00"),
                    cost = null,
                    assertion = null,
                    assertionCost = null,
                    comment = "Sample comment",
                ),
            showAmountHint = false,
            accounts = listOf("assets:checking", "expenses:food"),
            currencyEnabled = true,
            currencyBeforeAmount = true,
            unbalancedAmount = "0.00 EUR",
            onCommentChange = {},
            onAccountChange = {},
            onRemovePosting = {},
            onCurrencyChange = {},
            onAmountChange = {},
            onCostTypeChange = {},
            onCostCurrencyChange = {},
            onCostAmountChange = {},
            onAssertionCurrencyChange = {},
            onAssertionAmountChange = {},
            onAssertionCostTypeChange = {},
            onAssertionCostCurrencyChange = {},
            onAssertionCostAmountChange = {},
            onToggleAccount = {},
            onToggleAmount = {},
            onToggleCost = {},
            onToggleAssertion = {},
            onToggleAssertionCost = {},
            onToggleComment = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CostTypeSelectorPreview() {
    MicroLedgerTheme {
        CostTypeSelector(costType = CostType.UNIT, save = {})
    }
}

@Preview(showBackground = true)
@Composable
fun CurrencyAndAmountFieldsPreview() {
    MicroLedgerTheme {
        CurrencyAndAmountFields(
            currency = "EUR",
            quantity = "10.00",
            currencyEnabled = true,
            currencyBeforeAmount = true,
            showAmountHint = false,
            unbalancedAmount = null,
            onCurrencyChange = {},
            onAmountChange = {},
            modifier = Modifier,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AccountSelectorPreview() {
    MicroLedgerTheme {
        AccountSelector(
            value = "assets",
            options = listOf("assets:checking", "assets:savings", "expenses:food"),
            onAccountChange = {},
        )
    }
}
