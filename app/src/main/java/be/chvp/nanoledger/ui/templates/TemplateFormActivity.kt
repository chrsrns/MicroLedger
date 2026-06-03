package be.chvp.nanoledger.ui.templates

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.chvp.nanoledger.R
import be.chvp.nanoledger.ui.common.CodeField
import be.chvp.nanoledger.ui.common.NoteSelector
import be.chvp.nanoledger.ui.common.PayeeSelector
import be.chvp.nanoledger.ui.common.PostingRow
import be.chvp.nanoledger.ui.common.StatusSelector
import be.chvp.nanoledger.ui.theme.NanoLedgerTheme
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
            NanoLedgerTheme {
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
                    StatusSelector(viewModel, Modifier.width((3 * 16).sp.toDp()))
                    CodeField(
                        viewModel,
                        Modifier
                            .weight(0.5f)
                            .width((16 * 16).sp.toDp()),
                    )
                    PayeeSelector(
                        viewModel,
                        Modifier
                            .weight(0.5f)
                            .width((16 * 16).sp.toDp()),
                    )
                    NoteSelector(
                        viewModel,
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
