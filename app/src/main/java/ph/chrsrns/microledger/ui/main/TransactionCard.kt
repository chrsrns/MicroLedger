package ph.chrsrns.microledger.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ph.chrsrns.microledger.data.Amount
import ph.chrsrns.microledger.data.Posting
import ph.chrsrns.microledger.data.Transaction
import ph.chrsrns.microledger.ui.theme.MicroLedgerTheme
import ph.chrsrns.microledger.ui.util.accountTypeColor
import ph.chrsrns.microledger.ui.util.postingAmountColor
import ph.chrsrns.microledger.ui.util.statusChipLabel

@Composable
fun TransactionCard(
    transaction: Transaction,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    assetsPrefixes: List<String> = emptyList(),
    liabilitiesPrefixes: List<String> = emptyList(),
    equityPrefixes: List<String> = emptyList(),
    incomePrefixes: List<String> = emptyList(),
    expensesPrefixes: List<String> = emptyList(),
    decimalSeparator: String = ".",
) {
    Card(
        colors =
            if (selected) {
                CardDefaults.outlinedCardColors()
            } else {
                CardDefaults.cardColors()
            },
        elevation =
            if (selected) {
                CardDefaults.outlinedCardElevation()
            } else {
                CardDefaults.cardElevation()
            },
        border =
            if (selected) {
                CardDefaults.outlinedCardBorder(true)
            } else {
                null
            },
        modifier =
            modifier.testTag(
                if (selected) {
                    "Selected"
                } else {
                    "Unselected"
                },
            ),
    ) {
        Box(modifier = Modifier.clickable { onClick() }) {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .width(3.dp)
                            .background(
                                if (selected) {
                                    Color.Transparent
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            ),
                )
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                ) {
                    TransactionHeader(transaction)
                    for (posting in transaction.postings) {
                        TransactionPosting(
                            posting = posting,
                            assetsPrefixes = assetsPrefixes,
                            liabilitiesPrefixes = liabilitiesPrefixes,
                            equityPrefixes = equityPrefixes,
                            incomePrefixes = incomePrefixes,
                            expensesPrefixes = expensesPrefixes,
                            decimalSeparator = decimalSeparator,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionHeader(transaction: Transaction) {
    val statusLabel = statusChipLabel(transaction.status)
    val mutedColor = LocalContentColor.current.copy(alpha = 0.6f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = transaction.date,
            style =
                MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = mutedColor,
                ),
            maxLines = 1,
        )
        Text(
            text = transactionTitle(transaction),
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
        )
        if (statusLabel != null) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("StatusChip"),
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

private fun transactionTitle(transaction: Transaction): String {
    val parts =
        listOfNotNull(transaction.payee, transaction.note)
            .filter { it.isNotBlank() }
            .joinToString(" | ")
    val code = transaction.code?.trim()
    return if (code.isNullOrBlank()) {
        parts
    } else {
        "($code) $parts"
    }
}

@Composable
private fun TransactionPosting(
    posting: Posting,
    assetsPrefixes: List<String>,
    liabilitiesPrefixes: List<String>,
    equityPrefixes: List<String>,
    incomePrefixes: List<String>,
    expensesPrefixes: List<String>,
    decimalSeparator: String,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
    ) {
        if (posting.isComment()) {
            Text(
                text = posting.fullAmountDisplayString(),
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        color = LocalContentColor.current.copy(alpha = 0.6f),
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        } else {
            val dotColor =
                accountTypeColor(
                    posting.account,
                    assets = assetsPrefixes,
                    liabilities = liabilitiesPrefixes,
                    equity = equityPrefixes,
                    income = incomePrefixes,
                    expenses = expensesPrefixes,
                )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                if (dotColor != null) {
                    Box(
                        modifier =
                            Modifier
                                .padding(end = 6.dp)
                                .size(8.dp)
                                .background(dotColor, CircleShape)
                                .testTag("AccountDot"),
                    )
                }
                Text(
                    text = posting.account ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val amountColor =
                postingAmountColor(posting.amount, decimalSeparator)
                    ?: LocalContentColor.current
            Text(
                text = posting.fullAmountDisplayString(),
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = amountColor,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier =
                    Modifier
                        .padding(start = 2.dp)
                        .testTag("Amount"),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionCardPreview() {
    MicroLedgerTheme {
        TransactionCard(
            transaction =
                Transaction(
                    firstLine = 0,
                    lastLine = 0,
                    date = "2023-10-27",
                    status = "*",
                    code = null,
                    payee = "Supermarket",
                    note = "Weekly groceries",
                    postings =
                        listOf(
                            Posting(
                                account = "expenses:groceries",
                                amount = Amount("50.00", "EUR", "50.00 EUR"),
                                cost = null,
                                assertion = null,
                                assertionCost = null,
                                comment = null,
                            ),
                            Posting(
                                account = "assets:checking",
                                amount = Amount("-50.00", "EUR", "-50.00 EUR"),
                                cost = null,
                                assertion = null,
                                assertionCost = null,
                                comment = null,
                            ),
                        ),
                ),
            selected = false,
            onClick = {},
            modifier = Modifier.padding(8.dp),
            assetsPrefixes = listOf("assets"),
            expensesPrefixes = listOf("expenses"),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionCardSelectedPreview() {
    MicroLedgerTheme {
        TransactionCard(
            transaction =
                Transaction(
                    firstLine = 0,
                    lastLine = 0,
                    date = "2023-10-28",
                    status = "!",
                    code = "123",
                    payee = "Employer",
                    note = "Monthly salary",
                    postings =
                        listOf(
                            Posting(
                                account = "assets:checking",
                                amount = Amount("3000.00", "EUR", "3000.00 EUR"),
                                cost = null,
                                assertion = null,
                                assertionCost = null,
                                comment = null,
                            ),
                            Posting(
                                account = "income:salary",
                                amount = Amount("-3000.00", "EUR", "-3000.00 EUR"),
                                cost = null,
                                assertion = null,
                                assertionCost = null,
                                comment = null,
                            ),
                        ),
                ),
            selected = true,
            onClick = {},
            modifier = Modifier.padding(8.dp),
            assetsPrefixes = listOf("assets"),
            incomePrefixes = listOf("income"),
        )
    }
}
