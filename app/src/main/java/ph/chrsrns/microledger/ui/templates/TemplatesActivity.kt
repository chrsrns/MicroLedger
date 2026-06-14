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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ph.chrsrns.microledger.R
import ph.chrsrns.microledger.data.Posting
import ph.chrsrns.microledger.data.TransactionTemplate
import ph.chrsrns.microledger.ui.add.AddActivity
import ph.chrsrns.microledger.ui.common.NoFileState
import ph.chrsrns.microledger.ui.theme.MicroLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

const val TEMPLATE_ID_KEY = "template_id"

@AndroidEntryPoint
class TemplatesActivity : ComponentActivity() {
    private val templatesViewModel: TemplatesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        templatesViewModel.refreshTemplates()

        setContent {
            val context = LocalContext.current

            val errorWritingFile = stringResource(R.string.error_writing_file)
            val latestError by templatesViewModel.latestError.observeAsState()
            LaunchedEffect(latestError) {
                if (latestError?.get() != null) {
                    Toast.makeText(context, errorWritingFile, Toast.LENGTH_LONG).show()
                }
            }

            val errorReadingFile = stringResource(R.string.error_reading_file)
            val latestReadError by templatesViewModel.latestReadError.observeAsState()
            LaunchedEffect(latestReadError) {
                if (latestReadError?.get() != null) {
                    Toast.makeText(context, errorReadingFile, Toast.LENGTH_LONG).show()
                }
            }

            val mismatchMessage = stringResource(R.string.mismatch_no_delete)
            val latestMismatch by templatesViewModel.latestMismatch.observeAsState()
            LaunchedEffect(latestMismatch) {
                if (latestMismatch?.get() != null) {
                    Toast.makeText(context, mismatchMessage, Toast.LENGTH_LONG).show()
                }
            }

            MicroLedgerTheme {
                TemplatesScreen(
                    onBackClick = {
                        finish()
                    },
                    onAddClick = {
                        val intent = Intent(context, TemplateFormActivity::class.java)
                        startActivity(intent)
                    },
                    onEditClick = { template ->
                        val intent = Intent(context, TemplateFormActivity::class.java)
                        intent.putExtra(EDIT_TEMPLATE_ID_KEY, template.id)
                        startActivity(intent)
                    },
                    onTemplateClick = { template ->
                        val intent = Intent(context, AddActivity::class.java)
                        intent.putExtra(TEMPLATE_ID_KEY, template.id)
                        startActivity(intent)
                    },
                    onDeleteClick = { templateId ->
                        templatesViewModel.deleteTemplate(templateId) {}
                    },
                )
            }
        }
    }
}

@Composable
fun TemplatesScreen(
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onTemplateClick: (TransactionTemplate) -> Unit,
    onEditClick: (TransactionTemplate) -> Unit,
    onDeleteClick: (String) -> Unit,
    templatesViewModel: TemplatesViewModel = viewModel(),
    showTopBar: Boolean = true,
    showFab: Boolean = true,
) {
    val templates by templatesViewModel.templates.observeAsState(emptyList())
    val saving by templatesViewModel.saving.observeAsState(false)
    val fileUri by templatesViewModel.fileUri.observeAsState()

    TemplatesScreenContent(
        templates = templates,
        saving = saving,
        hasFile = fileUri != null,
        onBackClick = onBackClick,
        onAddClick = onAddClick,
        onTemplateClick = onTemplateClick,
        onEditClick = onEditClick,
        onDeleteClick = onDeleteClick,
        showTopBar = showTopBar,
        showFab = showFab,
    )
}

@Composable
fun TemplatesScreenContent(
    templates: List<TransactionTemplate>,
    saving: Boolean,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onTemplateClick: (TransactionTemplate) -> Unit,
    onEditClick: (TransactionTemplate) -> Unit,
    onDeleteClick: (String) -> Unit,
    showTopBar: Boolean = true,
    showFab: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
    hasFile: Boolean = true,
    onGoToSettings: (() -> Unit)? = null,
) {
    var templateToDelete by remember { mutableStateOf<TransactionTemplate?>(null) }

    val content: @Composable (PaddingValues) -> Unit = { contentPadding ->
        if (!hasFile) {
            NoFileState(
                contentPadding = contentPadding,
                onGoToSettings = onGoToSettings ?: {},
            )
        } else if (saving) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (templates.isEmpty()) {
            EmptyTemplatesState(contentPadding)
        } else {
            TemplatesGrid(
                templates = templates,
                onTemplateClick = onTemplateClick,
                onLongPress = { template ->
                    templateToDelete = template
                },
                contentPadding = contentPadding,
            )
        }

        templateToDelete?.let { template ->
            TemplateActionsDialog(
                templateName = template.name,
                onEdit = {
                    onEditClick(template)
                    templateToDelete = null
                },
                onDelete = {
                    onDeleteClick(template.id)
                    templateToDelete = null
                },
                onDismiss = {
                    templateToDelete = null
                },
            )
        }
    }

    if (showTopBar || showFab) {
        var fabHeight by remember { mutableIntStateOf(0) }
        val onFabPositioned =
            rememberUpdatedState { size: Int ->
                fabHeight = size
            }

        Scaffold(
            topBar = { if (showTopBar) TemplatesBar(onBackClick) else {} },
            floatingActionButton = {
                if (showFab) {
                    FloatingActionButton(
                        onClick = onAddClick,
                        modifier = Modifier.onGloballyPositioned { onFabPositioned.value(it.size.height) },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
                    }
                }
            },
            modifier = Modifier.imePadding(),
        ) { contentPadding ->
            content(contentPadding)
        }
    } else {
        content(contentPadding)
    }
}

@Composable
fun TemplatesBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.templates)) },
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
fun TemplatesGrid(
    templates: List<TransactionTemplate>,
    onTemplateClick: (TransactionTemplate) -> Unit,
    onLongPress: (TransactionTemplate) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(templates) { template ->
            TemplateCard(
                template = template,
                onClick = { onTemplateClick(template) },
                onLongClick = { onLongPress(template) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
            )
        }
    }
}

@Composable
fun EmptyTemplatesState(contentPadding: PaddingValues) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.no_templates_yet),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            stringResource(R.string.create_template),
            style =
                MaterialTheme.typography.headlineLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun TemplateActionsDialog(
    templateName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(templateName) },
        text = { Text(stringResource(R.string.template_actions_message)) },
        confirmButton = {
            TextButton(onClick = onEdit) {
                Text(stringResource(R.string.edit))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors =
                    androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text(stringResource(R.string.delete))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
fun TemplatesScreenPreview() {
    MicroLedgerTheme {
        TemplatesScreenContent(
            templates =
                listOf(
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
                ),
            saving = false,
            onBackClick = {},
            onAddClick = {},
            onTemplateClick = {},
            onEditClick = {},
            onDeleteClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TemplatesScreenNoFilePreview() {
    MicroLedgerTheme {
        TemplatesScreenContent(
            templates = emptyList(),
            saving = false,
            hasFile = false,
            onBackClick = {},
            onAddClick = {},
            onTemplateClick = {},
            onEditClick = {},
            onDeleteClick = {},
            onGoToSettings = {},
        )
    }
}
