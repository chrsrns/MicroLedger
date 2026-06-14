package ph.chrsrns.microledger.ui.templates

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ph.chrsrns.microledger.R
import ph.chrsrns.microledger.data.Amount
import ph.chrsrns.microledger.data.CostType
import ph.chrsrns.microledger.data.Posting
import ph.chrsrns.microledger.ui.common.CodeField
import ph.chrsrns.microledger.ui.common.NoteSelector
import ph.chrsrns.microledger.ui.common.PayeeSelector
import ph.chrsrns.microledger.ui.common.PostingRow
import ph.chrsrns.microledger.ui.common.StatusSelector
import ph.chrsrns.microledger.ui.theme.MicroLedgerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

const val EDIT_TEMPLATE_ID_KEY = "edit_template_id"

@AndroidEntryPoint
class TemplateFormActivity : ComponentActivity() {
    private val templateFormViewModel: TemplateFormViewModel by viewModels()

    private fun navigateBackToTemplates() {
        finish()
        startActivity(
            Intent(this, TemplatesActivity::class.java).setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP,
            ),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isEditing = intent.hasExtra(EDIT_TEMPLATE_ID_KEY)
        val templateId = intent.getStringExtra(EDIT_TEMPLATE_ID_KEY)
        if (isEditing && templateId != null) {
            templateFormViewModel.loadTemplate(templateId)
        }

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }

            var templateName by rememberSaveable { mutableStateOf("") }
            // Set template name from loaded template if editing
            if (isEditing && templateId != null) {
                val templates by templateFormViewModel.templates.observeAsState()
                val template = templates?.find { it.id == templateId }
                LaunchedEffect(template) {
                    if (template != null && templateName.isEmpty()) {
                        templateName = template.name
                    }
                }
            }

            val templateNotFound by templateFormViewModel.templateNotFound.observeAsState()
            LaunchedEffect(templateNotFound) {
                if (templateNotFound?.get() == true) {
                    Toast.makeText(context, R.string.template_not_found, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            BackHandler(enabled = true) {
                finish()
            }

            val saving by templateFormViewModel.saving.observeAsState()
            val nameValid = templateName.isNotBlank()
            val enabled = !(saving ?: true) && nameValid

            var fabHeight by remember { mutableIntStateOf(0) }
            val fabOffsetDp = with(LocalDensity.current) { fabHeight.toDp() + 16.dp }
            MicroLedgerTheme {
                Scaffold(
                    topBar = {
                        TemplateFormBar(
                            isEditing = isEditing,
                            onBackClick = {
                                finish()
                            },
                        )
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                if (enabled) {
                                    if (isEditing && templateId != null) {
                                        templateFormViewModel.updateTemplate(
                                            templateId,
                                            templateName
                                        ) {
                                            scope.launch(Main) { navigateBackToTemplates() }
                                        }
                                    } else {
                                        templateFormViewModel.saveAsTemplate(templateName) {
                                            scope.launch(Main) { navigateBackToTemplates() }
                                        }
                                    }
                                }
                            },
                            containerColor =
                                if (enabled) {
                                    FloatingActionButtonDefaults.containerColor
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            modifier = Modifier.onGloballyPositioned { fabHeight = it.size.height },
                        ) {
                            if (saving ?: true) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            } else {
                                Icon(
                                    Icons.Default.Done,
                                    contentDescription = stringResource(R.string.save),
                                )
                            }
                        }
                    },
                    modifier = Modifier.imePadding(),
                ) { contentPadding ->
                    TemplateForm(
                        viewModel = templateFormViewModel,
                        templateName = templateName,
                        onTemplateNameChange = { templateName = it },
                        contentPadding = contentPadding,
                        bottomOffset = fabOffsetDp,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateFormBar(
    isEditing: Boolean,
    onBackClick: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(if (isEditing) R.string.edit_template else R.string.add_template)) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
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
    BackHandler {
        onBackClick()
    }
}

@Composable
fun TemplateForm(
    viewModel: TemplateFormViewModel,
    templateName: String,
    onTemplateNameChange: (String) -> Unit,
    contentPadding: PaddingValues,
    bottomOffset: Dp,
) {
    val status by viewModel.status.observeAsState()
    val code by viewModel.code.observeAsState()
    val payee by viewModel.payee.observeAsState()
    val possiblePayees by viewModel.possiblePayees.observeAsState()
    val note by viewModel.note.observeAsState()
    val possibleNotes by viewModel.possibleNotes.observeAsState()
    val postings by viewModel.postings.observeAsState()
    val accounts by viewModel.accounts.observeAsState()
    val currencyEnabled by viewModel.currencyEnabled.observeAsState(true)
    val currencyBeforeAmount by viewModel.currencyBeforeAmount.observeAsState(true)
    val unbalancedAmount by viewModel.unbalancedAmount.observeAsState()

    TemplateForm(
        status = status,
        onStatusChange = { viewModel.setStatus(it) },
        code = code,
        onCodeChange = { viewModel.setCode(it) },
        payee = payee,
        possiblePayees = possiblePayees ?: emptyList(),
        onPayeeChange = { viewModel.setPayee(it) },
        note = note,
        possibleNotes = possibleNotes ?: emptyList(),
        onNoteChange = { viewModel.setNote(it) },
        postings = postings,
        accounts = accounts ?: emptyList(),
        currencyEnabled = currencyEnabled,
        currencyBeforeAmount = currencyBeforeAmount,
        unbalancedAmount = unbalancedAmount,
        onPostingCommentChange = { i, it -> viewModel.setComment(i, it) },
        onPostingAccountChange = { i, it -> viewModel.setAccount(i, it) },
        onRemovePosting = { viewModel.removePosting(it) },
        onPostingCurrencyChange = { i, it -> viewModel.setCurrency(i, it) },
        onPostingAmountChange = { i, it -> viewModel.setAmount(i, it) },
        onPostingCostTypeChange = { i, it -> viewModel.setCostType(i, it) },
        onPostingCostCurrencyChange = { i, it -> viewModel.setCostCurrency(i, it) },
        onPostingCostAmountChange = { i, it -> viewModel.setCostAmount(i, it) },
        onPostingAssertionCurrencyChange = { i, it -> viewModel.setAssertionCurrency(i, it) },
        onPostingAssertionAmountChange = { i, it -> viewModel.setAssertionAmount(i, it) },
        onPostingAssertionCostTypeChange = { i, it -> viewModel.setAssertionCostType(i, it) },
        onPostingAssertionCostCurrencyChange = { i, it ->
            viewModel.setAssertionCostCurrency(
                i,
                it
            )
        },
        onPostingAssertionCostAmountChange = { i, it -> viewModel.setAssertionCostAmount(i, it) },
        onPostingToggleAccount = { i, it -> viewModel.toggleAccount(i, it) },
        onPostingToggleAmount = { i, it -> viewModel.toggleAmount(i, it) },
        onPostingToggleCost = { i, it -> viewModel.toggleCost(i, it) },
        onPostingToggleAssertion = { i, it -> viewModel.toggleAssertion(i, it) },
        onPostingToggleAssertionCost = { i, it -> viewModel.toggleAssertionCost(i, it) },
        onPostingToggleComment = { i, it -> viewModel.toggleComment(i, it) },
        templateName = templateName,
        onTemplateNameChange = onTemplateNameChange,
        contentPadding = contentPadding,
        bottomOffset = bottomOffset,
    )
}

@Composable
fun TemplateForm(
    status: String?,
    onStatusChange: (String) -> Unit,
    code: String?,
    onCodeChange: (String) -> Unit,
    payee: String?,
    possiblePayees: List<String>,
    onPayeeChange: (String) -> Unit,
    note: String?,
    possibleNotes: List<String>,
    onNoteChange: (String) -> Unit,
    postings: List<Posting>?,
    accounts: List<String>,
    currencyEnabled: Boolean,
    currencyBeforeAmount: Boolean,
    unbalancedAmount: String?,
    onPostingCommentChange: (Int, String) -> Unit,
    onPostingAccountChange: (Int, String) -> Unit,
    onRemovePosting: (Int) -> Unit,
    onPostingCurrencyChange: (Int, String) -> Unit,
    onPostingAmountChange: (Int, String) -> Unit,
    onPostingCostTypeChange: (Int, CostType) -> Unit,
    onPostingCostCurrencyChange: (Int, String) -> Unit,
    onPostingCostAmountChange: (Int, String) -> Unit,
    onPostingAssertionCurrencyChange: (Int, String) -> Unit,
    onPostingAssertionAmountChange: (Int, String) -> Unit,
    onPostingAssertionCostTypeChange: (Int, CostType) -> Unit,
    onPostingAssertionCostCurrencyChange: (Int, String) -> Unit,
    onPostingAssertionCostAmountChange: (Int, String) -> Unit,
    onPostingToggleAccount: (Int, Boolean) -> Unit,
    onPostingToggleAmount: (Int, Boolean) -> Unit,
    onPostingToggleCost: (Int, Boolean) -> Unit,
    onPostingToggleAssertion: (Int, Boolean) -> Unit,
    onPostingToggleAssertionCost: (Int, Boolean) -> Unit,
    onPostingToggleComment: (Int, Boolean) -> Unit,
    templateName: String,
    onTemplateNameChange: (String) -> Unit,
    contentPadding: PaddingValues,
    bottomOffset: Dp,
) {
    Box(
        modifier =
            Modifier
                .padding(contentPadding)
                .fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 2.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            // Template name field
            TextField(
                value = templateName,
                onValueChange = onTemplateNameChange,
                label = { Text(stringResource(R.string.template_name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                singleLine = true,
            )

            with(LocalDensity.current) {
                FlowRow(
                    modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.Bottom,
                ) {
                    StatusSelector(status, onStatusChange, Modifier.width((3 * 16).sp.toDp()))
                    CodeField(
                        code, onCodeChange, Modifier
                            .weight(0.5f)
                            .width((16 * 16).sp.toDp())
                    )
                    PayeeSelector(
                        payee,
                        possiblePayees,
                        onPayeeChange,
                        Modifier
                            .weight(0.5f)
                            .width((16 * 16).sp.toDp())
                    )
                    NoteSelector(
                        note,
                        possibleNotes,
                        onNoteChange,
                        Modifier
                            .weight(0.75f)
                            .width((16 * 16).sp.toDp())
                    )
                }
                postings?.forEachIndexed { i, posting ->
                    HorizontalDivider(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    )
                    PostingRow(
                        posting = posting,
                        showAmountHint = posting.isEmpty(),
                        accounts = accounts,
                        currencyEnabled = currencyEnabled,
                        currencyBeforeAmount = currencyBeforeAmount,
                        unbalancedAmount = unbalancedAmount,
                        onCommentChange = { onPostingCommentChange(i, it) },
                        onAccountChange = { onPostingAccountChange(i, it) },
                        onRemovePosting = { onRemovePosting(i) },
                        onCurrencyChange = { onPostingCurrencyChange(i, it) },
                        onAmountChange = { onPostingAmountChange(i, it) },
                        onCostTypeChange = { onPostingCostTypeChange(i, it) },
                        onCostCurrencyChange = { onPostingCostCurrencyChange(i, it) },
                        onCostAmountChange = { onPostingCostAmountChange(i, it) },
                        onAssertionCurrencyChange = { onPostingAssertionCurrencyChange(i, it) },
                        onAssertionAmountChange = { onPostingAssertionAmountChange(i, it) },
                        onAssertionCostTypeChange = { onPostingAssertionCostTypeChange(i, it) },
                        onAssertionCostCurrencyChange = {
                            onPostingAssertionCostCurrencyChange(
                                i,
                                it
                            )
                        },
                        onAssertionCostAmountChange = { onPostingAssertionCostAmountChange(i, it) },
                        onToggleAccount = { onPostingToggleAccount(i, it) },
                        onToggleAmount = { onPostingToggleAmount(i, it) },
                        onToggleCost = { onPostingToggleCost(i, it) },
                        onToggleAssertion = { onPostingToggleAssertion(i, it) },
                        onToggleAssertionCost = { onPostingToggleAssertionCost(i, it) },
                        onToggleComment = { onPostingToggleComment(i, it) },
                    )
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

@Preview(showBackground = true)
@Composable
fun TemplateFormPreview() {
    MicroLedgerTheme {
        TemplateForm(
            status = "*",
            onStatusChange = {},
            code = "123",
            onCodeChange = {},
            payee = "Sample Payee",
            possiblePayees = listOf("Sample Payee", "Other Payee"),
            onPayeeChange = {},
            note = "Sample Note",
            possibleNotes = listOf("Sample Note", "Other Note"),
            onNoteChange = {},
            postings =
                listOf(
                    Posting(
                        "assets:checking",
                        Amount("-10.00", "EUR", "EUR -10.00"),
                        null,
                        null,
                        null,
                        null,
                    ),
                    Posting(
                        "expenses:food",
                        Amount("10.00", "EUR", "EUR 10.00"),
                        null,
                        null,
                        null,
                        null,
                    ),
                ),
            accounts = listOf("assets:checking", "expenses:food", "income:salary"),
            currencyEnabled = true,
            currencyBeforeAmount = true,
            unbalancedAmount = "0.00 EUR",
            onPostingCommentChange = { _, _ -> },
            onPostingAccountChange = { _, _ -> },
            onRemovePosting = { _ -> },
            onPostingCurrencyChange = { _, _ -> },
            onPostingAmountChange = { _, _ -> },
            onPostingCostTypeChange = { _, _ -> },
            onPostingCostCurrencyChange = { _, _ -> },
            onPostingCostAmountChange = { _, _ -> },
            onPostingAssertionCurrencyChange = { _, _ -> },
            onPostingAssertionAmountChange = { _, _ -> },
            onPostingAssertionCostTypeChange = { _, _ -> },
            onPostingAssertionCostCurrencyChange = { _, _ -> },
            onPostingAssertionCostAmountChange = { _, _ -> },
            onPostingToggleAccount = { _, _ -> },
            onPostingToggleAmount = { _, _ -> },
            onPostingToggleCost = { _, _ -> },
            onPostingToggleAssertion = { _, _ -> },
            onPostingToggleAssertionCost = { _, _ -> },
            onPostingToggleComment = { _, _ -> },
            templateName = "My Template",
            onTemplateNameChange = {},
            contentPadding = PaddingValues(16.dp),
            bottomOffset = 0.dp,
        )
    }
}
