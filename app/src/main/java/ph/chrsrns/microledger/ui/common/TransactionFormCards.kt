package ph.chrsrns.microledger.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ph.chrsrns.microledger.R
import ph.chrsrns.microledger.data.Amount
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
