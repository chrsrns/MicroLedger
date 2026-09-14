package ph.chrsrns.microledger.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ph.chrsrns.microledger.R
import ph.chrsrns.microledger.data.Amount
import ph.chrsrns.microledger.data.Cost
import ph.chrsrns.microledger.data.CostType
import ph.chrsrns.microledger.data.Posting
import ph.chrsrns.microledger.ui.util.accountTypeColor
import ph.chrsrns.microledger.ui.util.postingAmountColor

@Composable
fun TransactionHeaderCard(
    formattedDate: String,
    status: String?,
    code: String?,
    payee: String?,
    note: String?,
    possiblePayees: List<String>,
    possibleNotes: List<String>,
    onDateChange: (Long) -> Unit,
    onStatusChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onPayeeChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                DateField(
                    formattedDate = formattedDate,
                    onDateChange = onDateChange,
                    modifier = Modifier.weight(0.35f),
                )
                if (status != null) {
                    StatusSelector(
                        status = status,
                        onStatusChange = onStatusChange,
                        modifier = Modifier.weight(0.15f),
                    )
                }
                if (code != null) {
                    CodeField(
                        code = code,
                        onCodeChange = onCodeChange,
                        modifier = Modifier.weight(0.5f),
                    )
                }
            }
            if (payee != null) {
                PayeeSelector(
                    payee = payee,
                    options = possiblePayees,
                    onPayeeChange = onPayeeChange,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (note != null) {
                NoteSelector(
                    note = note,
                    options = possibleNotes,
                    onNoteChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun DateField(
    formattedDate: String,
    onDateChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var dateDialogOpen by remember { mutableStateOf(false) }
    val initialMillis = remember(formattedDate) { dateFormat.parse(formattedDate)?.time }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    OutlinedTextField(
        value = formattedDate,
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        label = {
            Text(
                stringResource(R.string.date),
                maxLines = 1,
            )
        },
        textStyle = LocalTextStyle.current,
        modifier =
            modifier
                .onFocusChanged {
                    if (it.isFocused) {
                        dateDialogOpen = true
                    }
                },
    )

    if (dateDialogOpen) {
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { dateDialogOpen = false },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDateChange(it) }
                        dateDialogOpen = false
                        focusManager.clearFocus()
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
        ) {
            androidx.compose.material3.DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun PostingList(
    postings: List<Posting>,
    accounts: List<String>,
    selectedIndex: Int,
    currencyEnabled: Boolean,
    currencyBeforeAmount: Boolean,
    currencyAmountSpacing: Boolean,
    decimalSeparator: String,
    defaultCurrency: String,
    unbalancedAmount: String?,
    assetsPrefixes: List<String>,
    liabilitiesPrefixes: List<String>,
    equityPrefixes: List<String>,
    incomePrefixes: List<String>,
    expensesPrefixes: List<String>,
    onPostingClick: (Int) -> Unit,
    onRemoveClick: (Int) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                stringResource(R.string.postings),
                style = MaterialTheme.typography.titleMedium,
            )
            postings.forEachIndexed { index, posting ->
                if (index > 0) {
                    HorizontalDivider()
                }
                val isBalance = index == postings.lastIndex && posting.account.isNullOrBlank()
                PostingRowCompact(
                    posting = posting,
                    isBalance = isBalance,
                    currencyEnabled = currencyEnabled,
                    currencyBeforeAmount = currencyBeforeAmount,
                    currencyAmountSpacing = currencyAmountSpacing,
                    decimalSeparator = decimalSeparator,
                    defaultCurrency = defaultCurrency,
                    unbalancedAmount = unbalancedAmount,
                    assetsPrefixes = assetsPrefixes,
                    liabilitiesPrefixes = liabilitiesPrefixes,
                    equityPrefixes = equityPrefixes,
                    incomePrefixes = incomePrefixes,
                    expensesPrefixes = expensesPrefixes,
                    onClick = { onPostingClick(index) },
                    onRemove = { onRemoveClick(index) },
                    showRemove = !isBalance,
                )
            }
            androidx.compose.material3.TextButton(
                onClick = onAddClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.add_posting))
            }
        }
    }
}

@Composable
fun PostingRowCompact(
    posting: Posting,
    isBalance: Boolean,
    currencyEnabled: Boolean,
    currencyBeforeAmount: Boolean,
    currencyAmountSpacing: Boolean,
    decimalSeparator: String,
    defaultCurrency: String,
    unbalancedAmount: String?,
    assetsPrefixes: List<String>,
    liabilitiesPrefixes: List<String>,
    equityPrefixes: List<String>,
    incomePrefixes: List<String>,
    expensesPrefixes: List<String>,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    showRemove: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            posting.isComment() -> {
                CommentCompactRow(posting, onClick, onRemove, showRemove)
            }
            isBalance -> {
                BalanceRow(
                    unbalancedAmount = unbalancedAmount,
                    currencyEnabled = currencyEnabled,
                    currencyBeforeAmount = currencyBeforeAmount,
                    currencyAmountSpacing = currencyAmountSpacing,
                    decimalSeparator = decimalSeparator,
                    defaultCurrency = defaultCurrency,
                )
            }
            else -> {
                AccountDot(
                    account = posting.account,
                    assetsPrefixes = assetsPrefixes,
                    liabilitiesPrefixes = liabilitiesPrefixes,
                    equityPrefixes = equityPrefixes,
                    incomePrefixes = incomePrefixes,
                    expensesPrefixes = expensesPrefixes,
                )
                Text(
                    text = posting.account ?: "",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                posting.amount?.let { amount ->
                    val amountText = amount.format(currencyBeforeAmount, currencyAmountSpacing, currencyEnabled)
                    Text(
                        text = amountText,
                        color = postingAmountColor(amount, decimalSeparator) ?: LocalTextStyle.current.color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (showRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Default.RemoveCircleOutline,
                            contentDescription = stringResource(R.string.remove_posting),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentCompactRow(
    posting: Posting,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    showRemove: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "; ${posting.comment ?: ""}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        androidx.compose.material3.AssistChip(
            onClick = onClick,
            label = { Text(stringResource(R.string.add_account)) },
        )
        if (showRemove) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.RemoveCircleOutline,
                    contentDescription = stringResource(R.string.remove_posting),
                )
            }
        }
    }
}

@Composable
fun BalanceRow(
    unbalancedAmount: String?,
    currencyEnabled: Boolean,
    currencyBeforeAmount: Boolean,
    currencyAmountSpacing: Boolean,
    decimalSeparator: String,
    defaultCurrency: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.balance),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        val amountText =
            if (!unbalancedAmount.isNullOrBlank() && defaultCurrency.isNotBlank()) {
                Amount(unbalancedAmount, defaultCurrency, "")
                    .format(currencyBeforeAmount, currencyAmountSpacing, currencyEnabled)
            } else {
                ""
            }
        if (amountText.isNotBlank()) {
            val amountColor =
                postingAmountColor(
                    Amount(unbalancedAmount!!, defaultCurrency, ""),
                    decimalSeparator,
                ) ?: LocalTextStyle.current.color
            Text(
                text = amountText,
                color = amountColor,
            )
        }
    }
}

@Composable
fun AccountDot(
    account: String?,
    assetsPrefixes: List<String>,
    liabilitiesPrefixes: List<String>,
    equityPrefixes: List<String>,
    incomePrefixes: List<String>,
    expensesPrefixes: List<String>,
) {
    val color =
        accountTypeColor(
            account,
            assetsPrefixes,
            liabilitiesPrefixes,
            equityPrefixes,
            incomePrefixes,
            expensesPrefixes,
        )
    if (color != null) {
        Box(
            modifier =
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color),
        )
    } else {
        Box(modifier = Modifier.size(12.dp))
    }
}

@Composable
fun PostingEditBottomSheet(
    posting: Posting,
    accounts: List<String>,
    isBalance: Boolean,
    currencyEnabled: Boolean,
    currencyBeforeAmount: Boolean,
    currencyAmountSpacing: Boolean,
    decimalSeparator: String,
    defaultCurrency: String,
    unbalancedAmount: String?,
    assetsPrefixes: List<String>,
    liabilitiesPrefixes: List<String>,
    equityPrefixes: List<String>,
    incomePrefixes: List<String>,
    expensesPrefixes: List<String>,
    onDismiss: () -> Unit,
    onSave: (Posting) -> Unit,
    onRemove: () -> Unit,
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState()
    var edited by androidx.compose.runtime.remember(posting) { mutableStateOf(posting) }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetHeader(isBalance, onRemove, onDismiss)

            edited.account?.let { account ->
                AccountSelector(
                    value = account,
                    options = accounts,
                    onAccountChange = { edited = edited.withAccount(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            edited.amount?.let { amount ->
                AmountEditor(
                    amount = amount,
                    showHint = isBalance && !unbalancedAmount.isNullOrBlank(),
                    hint = unbalancedAmount,
                    currencyEnabled = currencyEnabled,
                    currencyBeforeAmount = currencyBeforeAmount,
                    currencyAmountSpacing = currencyAmountSpacing,
                    onAmountChange = { edited = edited.withAmount(it) },
                )
            }

            edited.cost?.let { cost ->
                CostEditor(
                    cost = cost,
                    currencyEnabled = currencyEnabled,
                    currencyBeforeAmount = currencyBeforeAmount,
                    currencyAmountSpacing = currencyAmountSpacing,
                    onCostChange = { edited = edited.withCost(it) },
                )
            }

            edited.assertion?.let { assertion ->
                AssertionEditor(
                    assertion = assertion,
                    currencyEnabled = currencyEnabled,
                    currencyBeforeAmount = currencyBeforeAmount,
                    currencyAmountSpacing = currencyAmountSpacing,
                    onAssertionChange = { edited = edited.withAssertion(it) },
                )
            }

            edited.assertionCost?.let { assertionCost ->
                CostEditor(
                    cost = assertionCost,
                    prefix = "= ",
                    currencyEnabled = currencyEnabled,
                    currencyBeforeAmount = currencyBeforeAmount,
                    currencyAmountSpacing = currencyAmountSpacing,
                    onCostChange = { edited = edited.withAssertionCost(it) },
                )
            }

            edited.comment?.let { comment ->
                CommentField(
                    comment = comment,
                    onCommentChange = { edited = edited.withComment(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OptionalChips(
                posting = edited,
                defaultCurrency = defaultCurrency,
                onToggleAccount = { edited = edited.withAccount(if (it) "" else null) },
                onToggleAmount = {
                    edited =
                        edited.withAmount(
                            if (it) Amount("", defaultCurrency, "") else null,
                        )
                },
                onToggleCost = {
                    edited =
                        edited.withCost(
                            if (it) Cost(Amount("", defaultCurrency, ""), CostType.UNIT) else null,
                        )
                },
                onToggleAssertion = {
                    edited =
                        edited.withAssertion(
                            if (it) Amount("", defaultCurrency, "") else null,
                        )
                },
                onToggleAssertionCost = {
                    edited =
                        edited.withAssertionCost(
                            if (it) Cost(Amount("", defaultCurrency, ""), CostType.UNIT) else null,
                        )
                },
                onToggleComment = { edited = edited.withComment(if (it) "" else null) },
            )

            SheetFooter(
                canSave = edited.isComment() || !edited.account.isNullOrBlank(),
                onCancel = onDismiss,
                onSave = {
                    onSave(edited)
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun SheetHeader(
    isBalance: Boolean,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.edit_posting),
            style = MaterialTheme.typography.titleLarge,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isBalance) {
                TextButton(
                    onClick = {
                        onRemove()
                        onDismiss()
                    },
                ) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cancel),
                )
            }
        }
    }
}

@Composable
private fun AmountEditor(
    amount: Amount,
    showHint: Boolean,
    hint: String?,
    currencyEnabled: Boolean,
    currencyBeforeAmount: Boolean,
    currencyAmountSpacing: Boolean,
    onAmountChange: (Amount) -> Unit,
) {
    fun update(quantity: String = amount.quantity, currency: String = amount.currency) {
        onAmountChange(Amount(quantity, currency, ""))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (currencyEnabled && currencyBeforeAmount) {
            CurrencyField(
                currency = amount.currency,
                onCurrencyChange = { update(currency = it) },
                modifier = Modifier.padding(end = 4.dp),
            )
        }

        AmountField(
            quantity = amount.quantity,
            showAmountHint = showHint,
            unbalancedAmount = hint,
            onAmountChange = { update(quantity = it) },
            modifier = Modifier.weight(1f).testTag("posting_sheet_amount"),
        )

        if (currencyEnabled && !currencyBeforeAmount) {
            CurrencyField(
                currency = amount.currency,
                onCurrencyChange = { update(currency = it) },
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun CostEditor(
    cost: Cost,
    prefix: String = "",
    currencyEnabled: Boolean,
    currencyBeforeAmount: Boolean,
    currencyAmountSpacing: Boolean,
    onCostChange: (Cost) -> Unit,
) {
    fun update(
        type: CostType = cost.type,
        quantity: String = cost.amount.quantity,
        currency: String = cost.amount.currency,
    ) {
        onCostChange(Cost(Amount(quantity, currency, ""), type))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        if (prefix.isNotBlank()) {
            Text(prefix, modifier = Modifier.padding(horizontal = 4.dp))
        }
        CostTypeSelector(cost.type) { update(type = it) }
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (currencyEnabled && currencyBeforeAmount) {
                CurrencyField(
                    currency = cost.amount.currency,
                    onCurrencyChange = { update(currency = it) },
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
            AmountField(
                quantity = cost.amount.quantity,
                showAmountHint = false,
                unbalancedAmount = null,
                onAmountChange = { update(quantity = it) },
                modifier = Modifier.weight(1f),
            )
            if (currencyEnabled && !currencyBeforeAmount) {
                CurrencyField(
                    currency = cost.amount.currency,
                    onCurrencyChange = { update(currency = it) },
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun AssertionEditor(
    assertion: Amount,
    currencyEnabled: Boolean,
    currencyBeforeAmount: Boolean,
    currencyAmountSpacing: Boolean,
    onAssertionChange: (Amount) -> Unit,
) {
    AmountEditor(
        amount = assertion,
        showHint = false,
        hint = null,
        currencyEnabled = currencyEnabled,
        currencyBeforeAmount = currencyBeforeAmount,
        currencyAmountSpacing = currencyAmountSpacing,
        onAmountChange = { onAssertionChange(it) },
    )
}

@Composable
private fun OptionalChips(
    posting: Posting,
    defaultCurrency: String,
    onToggleAccount: (Boolean) -> Unit,
    onToggleAmount: (Boolean) -> Unit,
    onToggleCost: (Boolean) -> Unit,
    onToggleAssertion: (Boolean) -> Unit,
    onToggleAssertionCost: (Boolean) -> Unit,
    onToggleComment: (Boolean) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ToggleChip(
            active = posting.account != null,
            addLabel = stringResource(R.string.add_account),
            removeLabel = stringResource(R.string.remove_account),
            onClick = { onToggleAccount(posting.account == null) },
        )
        ToggleChip(
            active = posting.amount != null,
            addLabel = stringResource(R.string.add_amount),
            removeLabel = stringResource(R.string.remove_amount),
            onClick = { onToggleAmount(posting.amount == null) },
        )
        ToggleChip(
            active = posting.cost != null,
            addLabel = stringResource(R.string.add_cost),
            removeLabel = stringResource(R.string.remove_cost),
            onClick = { onToggleCost(posting.cost == null) },
        )
        ToggleChip(
            active = posting.assertion != null,
            addLabel = stringResource(R.string.add_assertion),
            removeLabel = stringResource(R.string.remove_assertion),
            onClick = { onToggleAssertion(posting.assertion == null) },
        )
        ToggleChip(
            active = posting.assertionCost != null,
            addLabel = stringResource(R.string.add_assertion_cost),
            removeLabel = stringResource(R.string.remove_assertion_cost),
            onClick = { onToggleAssertionCost(posting.assertionCost == null) },
        )
        ToggleChip(
            active = posting.comment != null,
            addLabel = stringResource(R.string.add_comment),
            removeLabel = stringResource(R.string.remove_comment),
            onClick = { onToggleComment(posting.comment == null) },
        )
    }
}

@Composable
private fun ToggleChip(
    active: Boolean,
    addLabel: String,
    removeLabel: String,
    onClick: () -> Unit,
) {
    androidx.compose.material3.FilterChip(
        selected = active,
        onClick = onClick,
        label = { Text(if (active) removeLabel else addLabel) },
    )
}

@Composable
private fun SheetFooter(
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onCancel) {
            Text(stringResource(R.string.cancel))
        }
        TextButton(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.testTag("posting_sheet_save"),
        ) {
            Text(stringResource(R.string.save))
        }
    }
}
