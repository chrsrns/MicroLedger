package be.chvp.nanoledger.ui.templates

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import be.chvp.nanoledger.R
import be.chvp.nanoledger.data.Posting
import be.chvp.nanoledger.data.TransactionTemplate
import be.chvp.nanoledger.ui.theme.NanoLedgerTheme

@Composable
fun TemplateCard(
    template: TransactionTemplate,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (template.payee != null) {
                Text(
                    text = template.payee,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            val accounts = template.postings.mapNotNull { it.account }
            if (accounts.isNotEmpty()) {
                val accountSummary =
                    when (accounts.size) {
                        1 -> accounts[0]
                        2 -> "${accounts[0]} → ${accounts[1]}"
                        else -> "${accounts[0]} → ${accounts[1]} " +
                                stringResource(
                                    R.string.template_accounts_more,
                                    accounts.size - 2
                                )
                    }
                Text(
                    text = accountSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TemplateCardPreview() {
    NanoLedgerTheme {
        TemplateCard(
            template =
                TransactionTemplate(
                    firstLine = 0,
                    lastLine = 0,
                    id = "1",
                    name = "Simple Template",
                    payee = "Some Payee",
                    note = null,
                    status = null,
                    code = null,
                    postings =
                        listOf(
                            Posting("Assets:Checking", null, null, null, null, null),
                            Posting("Expenses:Groceries", null, null, null, null, null),
                        ),
                ),
            onClick = {},
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TemplateCardComplexPreview() {
    NanoLedgerTheme {
        TemplateCard(
            template =
                TransactionTemplate(
                    firstLine = 0,
                    lastLine = 0,
                    id = "2",
                    name = "Complex Template",
                    payee = "Another Payee",
                    note = "A note",
                    status = "*",
                    code = "123",
                    postings =
                        listOf(
                            Posting("Assets:Checking", null, null, null, null, null),
                            Posting("Expenses:Food", null, null, null, null, null),
                            Posting("Expenses:Drink", null, null, null, null, null),
                        ),
                ),
            onClick = {},
            modifier = Modifier.padding(8.dp),
        )
    }
}

