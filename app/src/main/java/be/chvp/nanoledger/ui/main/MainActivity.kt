package be.chvp.nanoledger.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import be.chvp.nanoledger.R
import be.chvp.nanoledger.data.Amount
import be.chvp.nanoledger.data.Posting
import be.chvp.nanoledger.data.Transaction
import be.chvp.nanoledger.ui.add.AddActivity
import be.chvp.nanoledger.ui.common.TRANSACTION_INDEX_KEY
import be.chvp.nanoledger.ui.edit.EditActivity
import be.chvp.nanoledger.ui.preferences.PreferencesActivity
import be.chvp.nanoledger.ui.templates.TemplatesActivity
import be.chvp.nanoledger.ui.theme.NanoLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current

            val latestReadError by mainViewModel.latestReadError.observeAsState()
            val readErrorMessage = stringResource(R.string.error_reading_file)
            LaunchedEffect(latestReadError) {
                val error = latestReadError?.get()
                if (error != null) {
                    Log.e("be.chvp.nanoledger", "Exception while reading file", error)
                    Toast
                        .makeText(
                            context,
                            readErrorMessage,
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }

            val latestWriteError by mainViewModel.latestWriteError.observeAsState()
            val writeErrorMessage = stringResource(R.string.error_writing_file)
            LaunchedEffect(latestWriteError) {
                val error = latestWriteError?.get()
                if (error != null) {
                    Log.e("be.chvp.nanoledger", "Exception while writing file", error)
                    Toast
                        .makeText(
                            context,
                            writeErrorMessage,
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }

            val latestMismatch by mainViewModel.latestMismatch.observeAsState()
            val mismatchMessage = stringResource(R.string.mismatch_no_delete)
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

            val fileUri by mainViewModel.fileUri.observeAsState()
            LaunchedEffect(fileUri) {
                mainViewModel.refresh()
            }

            NanoLedgerTheme {
                MainScreen(
                    onAddClick = {
                        startActivity(Intent(this, AddActivity::class.java))
                    },
                    onSettingsClick = {
                        startActivity(Intent(this, PreferencesActivity::class.java))
                    },
                    onTemplatesClick = {
                        startActivity(Intent(this, TemplatesActivity::class.java))
                    },
                    onCopyClick = { index ->
                        val intent = Intent(this, AddActivity::class.java)
                        intent.putExtra(TRANSACTION_INDEX_KEY, index)
                        mainViewModel.toggleSelect(index)
                        startActivity(intent)
                    },
                    onEditClick = { index ->
                        val intent = Intent(this, EditActivity::class.java)
                        intent.putExtra(TRANSACTION_INDEX_KEY, index)
                        mainViewModel.toggleSelect(index)
                        startActivity(intent)
                    },
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    mainViewModel: MainViewModel = viewModel(),
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onCopyClick: (Int) -> Unit,
    onEditClick: (Int) -> Unit,
) {
    val fileUri by mainViewModel.fileUri.observeAsState()
    val transactions by mainViewModel.filteredTransactions.observeAsState()
    val searching by mainViewModel.searching.observeAsState()
    val query by mainViewModel.query.observeAsState()
    val isRefreshing by mainViewModel.isRefreshing.observeAsState()
    val selected by mainViewModel.selectedIndex.observeAsState()

    MainScreen(
        fileUri = fileUri,
        transactions = transactions,
        searching = searching ?: false,
        query = query ?: "",
        isRefreshing = isRefreshing ?: false,
        selected = selected,
        onRefresh = { mainViewModel.refresh() },
        onToggleSelect = { mainViewModel.toggleSelect(it) },
        onSearchClick = { mainViewModel.setSearching(true) },
        onSettingsClick = onSettingsClick,
        onTemplatesClick = onTemplatesClick,
        onStopSearching = {
            mainViewModel.setSearching(false)
            mainViewModel.setQuery("")
        },
        onQueryChange = { mainViewModel.setQuery(it) },
        onStopSelection = { selected?.let { mainViewModel.toggleSelect(it) } },
        onCopyClick = { selected?.let { onCopyClick(it) } },
        onEditClick = { selected?.let { onEditClick(it) } },
        onDeleteClick = { mainViewModel.deleteSelected() },
        onAddClick = onAddClick,
    )
}

@Composable
fun MainScreen(
    fileUri: Uri?,
    transactions: List<Pair<Int, Transaction>>?,
    searching: Boolean,
    query: String,
    isRefreshing: Boolean,
    selected: Int?,
    onRefresh: () -> Unit,
    onToggleSelect: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onStopSearching: () -> Unit,
    onQueryChange: (String) -> Unit,
    onStopSelection: () -> Unit,
    onCopyClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddClick: () -> Unit,
) {
    var fabHeight by remember { mutableIntStateOf(0) }
    val fabOffsetDp = with(LocalDensity.current) { fabHeight.toDp() + 16.dp }

    Scaffold(
        topBar = {
            if (selected != null) {
                SelectionBar(onStopSelection, onCopyClick, onEditClick, onDeleteClick)
            } else if (searching) {
                SearchBar(query, onQueryChange, onStopSearching)
            } else {
                MainBar(onSearchClick, onSettingsClick, onTemplatesClick)
            }
        },
        floatingActionButton = {
            if (fileUri != null) {
                FloatingActionButton(
                    onClick = onAddClick,
                    modifier = Modifier.onGloballyPositioned { fabHeight = it.size.height },
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.add),
                    )
                }
            }
        },
        modifier = Modifier.imePadding(),
    ) { contentPadding ->
        if (fileUri != null) {
            MainContent(
                transactions = transactions,
                query = query,
                isRefreshing = isRefreshing,
                selected = selected,
                onRefresh = onRefresh,
                onToggleSelect = onToggleSelect,
                contentPadding = contentPadding,
                bottomOffset = fabOffsetDp,
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(R.string.no_file_yet),
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier.align(Alignment.CenterHorizontally).padding(
                            horizontal = 16.dp,
                        ),
                )
                Text(
                    stringResource(R.string.go_to_settings),
                    style =
                        MaterialTheme.typography.headlineLarge.copy(
                            textDecoration = TextDecoration.Underline,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(
                                horizontal = 16.dp,
                            ).clickable { onSettingsClick() },
                )
            }
        }
    }
}

@Composable
fun MainContent(
    transactions: List<Pair<Int, Transaction>>?,
    query: String,
    isRefreshing: Boolean,
    selected: Int?,
    onRefresh: () -> Unit,
    onToggleSelect: (Int) -> Unit,
    contentPadding: PaddingValues,
    bottomOffset: Dp,
) {
    val context = LocalContext.current
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier.padding(contentPadding),
    ) {
        if (transactions != null && (transactions.isNotEmpty() || isRefreshing)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(transactions.size) {
                    val index = transactions.size - it - 1
                    val (originalIndex, transaction) = transactions[index]
                    TransactionCard(
                        transaction,
                        originalIndex == selected,
                        { onToggleSelect(originalIndex) },
                        Modifier.fillMaxWidth().padding(
                            8.dp,
                            if (it == 0) 8.dp else 4.dp,
                            8.dp,
                            if (it == transactions.size - 1) 8.dp else 4.dp,
                        ),
                    )
                }
                item {
                    Box(Modifier.height(bottomOffset).fillMaxWidth())
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    if (query == "") {
                        Text(
                            stringResource(R.string.no_transactions_yet),
                            style = MaterialTheme.typography.headlineLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Text(
                            stringResource(R.string.create_transaction),
                            style =
                                MaterialTheme.typography.headlineLarge.copy(
                                    textDecoration = TextDecoration.Underline,
                                    color = MaterialTheme.colorScheme.primary,
                                ),
                            textAlign = TextAlign.Center,
                            modifier =
                                Modifier.padding(horizontal = 16.dp).clickable {
                                    context.startActivity(
                                        Intent(context, AddActivity::class.java),
                                    )
                                },
                        )
                    } else {
                        Text(
                            stringResource(R.string.no_search_results),
                            style = MaterialTheme.typography.headlineLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainBar(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTemplatesClick: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
            }
            IconButton(onClick = onTemplatesClick) {
                Icon(
                    painterResource(R.drawable.baseline_bookmark_24),
                    contentDescription = stringResource(R.string.templates),
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings),
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    )
}

@Composable
fun SelectionBar(
    onStopSelection: () -> Unit,
    onCopyClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = onStopSelection,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.stop_selection))
            }
        },
        title = { },
        actions = {
            IconButton(onClick = onCopyClick) {
                Icon(painterResource(R.drawable.baseline_difference_24), contentDescription = stringResource(R.string.copy_and_edit))
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    )

    BackHandler {
        onStopSelection()
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onStopSearching: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = onStopSearching,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.stop_searching))
            }
        },
        title = {
            TextField(
                query,
                onQueryChange,
                singleLine = true,
                placeholder = {
                    Text(
                        stringResource(R.string.search),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Normal,
                    )
                },
                colors =
                    TextFieldDefaults.colors(
                        focusedTextColor = LocalContentColor.current,
                        unfocusedTextColor = LocalContentColor.current,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        cursorColor = LocalContentColor.current,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).testTag("search-field"),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusRequester.freeFocus()
                        },
                    ),
            )
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    )
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
    BackHandler {
        onStopSearching()
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    NanoLedgerTheme {
        MainScreen(
            fileUri = "content://test".toUri(),
            transactions =
                listOf(
                    0 to
                        Transaction(
                            firstLine = 0,
                            lastLine = 2,
                            date = "2023-08-31",
                            status = "*",
                            code = null,
                            payee = "Payee",
                            note = "Note",
                            postings =
                                listOf(
                                    Posting("assets", Amount("-5.00", "€", "€ -5.00"), null, null, null, null),
                                    Posting("expenses", Amount("5.00", "€", "€ 5.00"), null, null, null, null),
                                ),
                        ),
                    1 to
                        Transaction(
                            firstLine = 3,
                            lastLine = 5,
                            date = "2023-09-01",
                            status = "!",
                            code = "123",
                            payee = "Another Payee",
                            note = null,
                            postings =
                                listOf(
                                    Posting("assets", Amount("-10.00", "€", "€ -10.00"), null, null, null, null),
                                    Posting("expenses", Amount("10.00", "€", "€ 10.00"), null, null, null, null),
                                ),
                        ),
                ),
            searching = false,
            query = "",
            isRefreshing = false,
            selected = null,
            onRefresh = {},
            onToggleSelect = {},
            onSearchClick = {},
            onSettingsClick = {},
            onTemplatesClick = {},
            onStopSearching = {},
            onQueryChange = {},
            onStopSelection = {},
            onCopyClick = {},
            onEditClick = {},
            onDeleteClick = {},
            onAddClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainContentPreview() {
    NanoLedgerTheme {
        MainContent(
            transactions =
                listOf(
                    0 to
                        Transaction(
                            firstLine = 0,
                            lastLine = 2,
                            date = "2023-08-31",
                            status = "*",
                            code = null,
                            payee = "Payee",
                            note = "Note",
                            postings =
                                listOf(
                                    Posting("assets", Amount("-5.00", "€", "€ -5.00"), null, null, null, null),
                                    Posting("expenses", Amount("5.00", "€", "€ 5.00"), null, null, null, null),
                                ),
                        ),
                    1 to
                        Transaction(
                            firstLine = 3,
                            lastLine = 5,
                            date = "2023-09-01",
                            status = "!",
                            code = "123",
                            payee = "Another Payee",
                            note = null,
                            postings =
                                listOf(
                                    Posting("assets", Amount("-10.00", "€", "€ -10.00"), null, null, null, null),
                                    Posting("expenses", Amount("10.00", "€", "€ 10.00"), null, null, null, null),
                                ),
                        ),
                ),
            query = "",
            isRefreshing = false,
            selected = null,
            onRefresh = {},
            onToggleSelect = {},
            contentPadding = PaddingValues(0.dp),
            bottomOffset = 0.dp,
        )
    }
}
