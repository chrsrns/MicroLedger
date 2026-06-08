package be.chvp.nanoledger.ui.cashflowtransactions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import be.chvp.nanoledger.R
import be.chvp.nanoledger.data.Amount
import be.chvp.nanoledger.data.Posting
import be.chvp.nanoledger.data.Transaction
import be.chvp.nanoledger.data.reporting.MonthlyCashFlowCalculator
import be.chvp.nanoledger.ui.main.TransactionCard
import be.chvp.nanoledger.ui.theme.NanoLedgerTheme
import be.chvp.nanoledger.ui.util.amountColor
import be.chvp.nanoledger.ui.util.formatAmount
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import java.text.DateFormatSymbols
import java.util.Locale

@AndroidEntryPoint
class CashFlowTransactionsActivity : ComponentActivity() {
    private val cashFlowTransactionsViewModel: CashFlowTransactionsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NanoLedgerTheme {
                CashFlowTransactionsScreen(
                    onBackClick = { finish() },
                )
            }
        }
    }
}

@Composable
fun CashFlowTransactionsScreen(
    cashFlowTransactionsViewModel: CashFlowTransactionsViewModel = viewModel(),
    onBackClick: () -> Unit,
) {
    val cashFlow by cashFlowTransactionsViewModel.currentMonthCashFlow.observeAsState()
    val monthlyHistory by cashFlowTransactionsViewModel.monthlyHistory.observeAsState()
    val selectedYear by cashFlowTransactionsViewModel.selectedYear.observeAsState(
        java.util.Calendar
            .getInstance()
            .get(java.util.Calendar.YEAR),
    )
    val selectedMonth by cashFlowTransactionsViewModel.selectedMonth.observeAsState(
        java.util.Calendar
            .getInstance()
            .get(java.util.Calendar.MONTH) + 1,
    )
    val decimalSeparator by cashFlowTransactionsViewModel.decimalSeparator.observeAsState(".")

    CashFlowTransactionsScreenContent(
        cashFlow = cashFlow,
        monthlyHistory = monthlyHistory,
        selectedYear = selectedYear,
        selectedMonth = selectedMonth,
        decimalSeparator = decimalSeparator,
        onBackClick = onBackClick,
        onPreviousMonth = cashFlowTransactionsViewModel::previousMonth,
        onNextMonth = cashFlowTransactionsViewModel::nextMonth,
        onSelectMonth = cashFlowTransactionsViewModel::selectMonth,
    )
}

@Composable
fun CashFlowTransactionsScreenContent(
    cashFlow: MonthlyCashFlowCalculator.CashFlowResult?,
    monthlyHistory: List<MonthlyCashFlowCalculator.CashFlowResult>?,
    selectedYear: Int,
    selectedMonth: Int,
    decimalSeparator: String,
    onBackClick: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonth: (Int, Int) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                title = { Text(stringResource(R.string.cash_flow_details)) },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            )
        },
        modifier = Modifier.imePadding(),
    ) { contentPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MonthSelector(
                selectedYear = selectedYear,
                selectedMonth = selectedMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onSelectMonth = onSelectMonth,
            )
            CashFlowSummaryCard(cashFlow, decimalSeparator)
            CashFlowGraphCard(monthlyHistory, selectedMonth)
            RankingCard(
                title = stringResource(R.string.top_income),
                transactions = cashFlow?.incomeTransactions ?: emptyList(),
                emptyMessage = stringResource(R.string.no_income_transactions),
            )
            RankingCard(
                title = stringResource(R.string.top_expenses),
                transactions = cashFlow?.expenseTransactions ?: emptyList(),
                emptyMessage = stringResource(R.string.no_expense_transactions),
            )
        }
    }
}

@Composable
fun MonthSelector(
    selectedYear: Int,
    selectedMonth: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonth: (Int, Int) -> Unit,
) {
    val monthNames = DateFormatSymbols(LocalLocale.current.platformLocale).months
    var showMonthDropdown by remember { mutableStateOf(false) }
    var showYearDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Default.ChevronLeft, contentDescription = null)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            // Month dropdown
            Column {
                OutlinedButton(onClick = { showMonthDropdown = true }) {
                    Text(monthNames[selectedMonth - 1])
                }
                DropdownMenu(
                    expanded = showMonthDropdown,
                    onDismissRequest = { showMonthDropdown = false },
                ) {
                    monthNames.take(12).forEachIndexed { index, name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onSelectMonth(selectedYear, index + 1)
                                showMonthDropdown = false
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            // Year dropdown
            Column {
                OutlinedButton(onClick = { showYearDropdown = true }) {
                    Text(selectedYear.toString())
                }
                DropdownMenu(
                    expanded = showYearDropdown,
                    onDismissRequest = { showYearDropdown = false },
                ) {
                    val currentYear =
                        java.util.Calendar
                            .getInstance()
                            .get(java.util.Calendar.YEAR)
                    (currentYear downTo currentYear - 10).forEach { year ->
                        DropdownMenuItem(
                            text = { Text(year.toString()) },
                            onClick = {
                                onSelectMonth(year, selectedMonth)
                                showYearDropdown = false
                            },
                        )
                    }
                }
            }
        }
        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun CashFlowSummaryCard(
    cashFlow: MonthlyCashFlowCalculator.CashFlowResult?,
    decimalSeparator: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.cash_flow_this_month),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            if (cashFlow == null) {
                Text(
                    stringResource(R.string.no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            } else {
                AmountSummaryRow(
                    label = stringResource(R.string.income),
                    amount = cashFlow.totalIncome,
                    decimalSeparator = decimalSeparator,
                )
                AmountSummaryRow(
                    label = stringResource(R.string.expenses),
                    amount = cashFlow.totalExpenses,
                    decimalSeparator = decimalSeparator,
                    negate = true,
                )
                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                AmountSummaryRow(
                    label = stringResource(R.string.net_flow),
                    amount = cashFlow.netFlow,
                    decimalSeparator = decimalSeparator,
                    bold = true,
                )
            }
        }
    }
}

@Composable
fun AmountSummaryRow(
    label: String,
    amount: BigDecimal,
    decimalSeparator: String,
    negate: Boolean = false,
    bold: Boolean = false,
) {
    val displayAmount = if (negate) amount.negate() else amount
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Text(
            formatAmount(displayAmount, decimalSeparator),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            color = amountColor(displayAmount),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun CashFlowGraphCard(
    monthlyHistory: List<MonthlyCashFlowCalculator.CashFlowResult>?,
    selectedMonth: Int,
) {
    val incomeColor = MaterialTheme.colorScheme.primary
    val expenseColor = MaterialTheme.colorScheme.error
    val selectedBarAlpha = 1f
    val unselectedBarAlpha = 0.4f
    val textColor = LocalContentColor.current
    val textMeasurer = rememberTextMeasurer()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.income_vs_expenses),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendItem(color = incomeColor, label = stringResource(R.string.income))
                LegendItem(color = expenseColor, label = stringResource(R.string.expenses))
            }
            Spacer(Modifier.height(12.dp))
            if (monthlyHistory.isNullOrEmpty()) {
                Text(
                    stringResource(R.string.no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            } else {
                Canvas(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                ) {
                    drawBarChart(
                        monthlyHistory = monthlyHistory,
                        selectedMonth = selectedMonth,
                        incomeColor = incomeColor,
                        expenseColor = expenseColor,
                        selectedBarAlpha = selectedBarAlpha,
                        unselectedBarAlpha = unselectedBarAlpha,
                        textColor = textColor,
                        textMeasurer = textMeasurer,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawBarChart(
    monthlyHistory: List<MonthlyCashFlowCalculator.CashFlowResult>,
    selectedMonth: Int,
    incomeColor: Color,
    expenseColor: Color,
    selectedBarAlpha: Float,
    unselectedBarAlpha: Float,
    textColor: Color,
    textMeasurer: TextMeasurer,
) {
    val monthLabels = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
    val labelHeight = 20.dp.toPx()
    val chartHeight = size.height - labelHeight
    val barGroupWidth = size.width / 12f
    val barWidth = barGroupWidth * 0.35f
    val gap = barGroupWidth * 0.05f

    val maxValue =
        monthlyHistory.maxOfOrNull {
            maxOf(it.totalIncome, it.totalExpenses)
        } ?: BigDecimal.ONE
    val maxFloat = maxValue.toFloat().coerceAtLeast(1f)

    monthlyHistory.forEachIndexed { index, result ->
        val isSelected = index + 1 == selectedMonth
        val alpha = if (isSelected) selectedBarAlpha else unselectedBarAlpha

        val incomeHeight = (result.totalIncome.toFloat() / maxFloat) * chartHeight
        val expenseHeight = (result.totalExpenses.toFloat() / maxFloat) * chartHeight

        val groupX = index * barGroupWidth

        // Income bar
        drawRect(
            color = incomeColor.copy(alpha = alpha),
            topLeft = Offset(groupX + gap, chartHeight - incomeHeight),
            size = Size(barWidth, incomeHeight),
        )

        // Expense bar
        drawRect(
            color = expenseColor.copy(alpha = alpha),
            topLeft = Offset(groupX + gap + barWidth, chartHeight - expenseHeight),
            size = Size(barWidth, expenseHeight),
        )

        // Month label
        val labelStyle =
            TextStyle(
                fontSize = 10.sp,
                color = if (isSelected) textColor else textColor.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )
        val labelResult = textMeasurer.measure(monthLabels[index], labelStyle)
        drawText(
            textLayoutResult = labelResult,
            topLeft =
                Offset(
                    groupX + (barGroupWidth - labelResult.size.width) / 2f,
                    chartHeight + 4.dp.toPx(),
                ),
        )
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawRect(color = color)
        }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun RankingCard(
    title: String,
    transactions: List<Transaction>,
    emptyMessage: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            if (transactions.isEmpty()) {
                Text(
                    emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalContentColor.current.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    transactions.forEach { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            selected = false,
                            onClick = {},
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CashFlowTransactionsScreenPreview() {
    val sampleTransactions =
        listOf(
            Transaction(
                firstLine = 1,
                lastLine = 3,
                date = "2026-06-01",
                status = null,
                code = null,
                payee = "Salary",
                note = null,
                postings =
                    listOf(
                        Posting(
                            "Assets:Checking",
                            Amount("5000.00", "$", "$ 5000.00"),
                            null,
                            null,
                            null,
                            null,
                        ),
                        Posting(
                            "Income:Salary",
                            Amount("-5000.00", "$", "$ -5000.00"),
                            null,
                            null,
                            null,
                            null,
                        ),
                    ),
            ),
            Transaction(
                firstLine = 5,
                lastLine = 7,
                date = "2026-06-05",
                status = null,
                code = null,
                payee = "Grocery Store",
                note = null,
                postings =
                    listOf(
                        Posting(
                            "Expenses:Food",
                            Amount("150.00", "$", "$ 150.00"),
                            null,
                            null,
                            null,
                            null,
                        ),
                        Posting(
                            "Assets:Checking",
                            Amount("-150.00", "$", "$ -150.00"),
                            null,
                            null,
                            null,
                            null,
                        ),
                    ),
            ),
        )

    val sampleHistory =
        (1..12).map { month ->
            MonthlyCashFlowCalculator.CashFlowResult(
                totalIncome = BigDecimal(3000 + (Math.random() * 2000).toInt()),
                totalExpenses = BigDecimal(1500 + (Math.random() * 1500).toInt()),
                netFlow = BigDecimal.ZERO,
                period = String.format(Locale.US, "2026-%02d", month),
                incomeTransactions = if (month == 6) sampleTransactions.take(1) else emptyList(),
                expenseTransactions = if (month == 6) sampleTransactions.drop(1) else emptyList(),
            )
        }

    NanoLedgerTheme {
        CashFlowTransactionsScreenContent(
            cashFlow =
                MonthlyCashFlowCalculator.CashFlowResult(
                    totalIncome = BigDecimal("5000.00"),
                    totalExpenses = BigDecimal("1635.00"),
                    netFlow = BigDecimal("3365.00"),
                    period = "2026-06",
                    incomeTransactions = sampleTransactions.take(1),
                    expenseTransactions = sampleTransactions.drop(1),
                ),
            monthlyHistory = sampleHistory,
            selectedYear = 2026,
            selectedMonth = 6,
            decimalSeparator = ".",
            onBackClick = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onSelectMonth = { _, _ -> },
        )
    }
}
