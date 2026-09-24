package ph.chrsrns.microledger.ui.cashflowtransactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import ph.chrsrns.microledger.data.AccountTypePrefixes
import ph.chrsrns.microledger.data.LedgerRepository
import ph.chrsrns.microledger.data.PreferencesDataSource
import ph.chrsrns.microledger.data.ReportingPreferences
import ph.chrsrns.microledger.data.Transaction
import ph.chrsrns.microledger.data.reporting.MonthlyCashFlowCalculator
import ph.chrsrns.microledger.data.reporting.ReportingInputs
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class CashFlowTransactionsViewModel
    @Inject
    constructor(
        application: Application,
        private val ledgerRepository: LedgerRepository,
        private val preferencesDataSource: PreferencesDataSource,
    ) : AndroidViewModel(application) {
        private val cashFlowCalculator = MonthlyCashFlowCalculator()

        val decimalSeparator: LiveData<String> = preferencesDataSource.decimalSeparator
        val assetsPrefixes: LiveData<List<String>> = preferencesDataSource.assetsPrefixes
        val liabilitiesPrefixes: LiveData<List<String>> = preferencesDataSource.liabilitiesPrefixes
        val equityPrefixes: LiveData<List<String>> = preferencesDataSource.equityPrefixes
        val incomePrefixes: LiveData<List<String>> = preferencesDataSource.incomePrefixes
        val expensesPrefixes: LiveData<List<String>> = preferencesDataSource.expensesPrefixes

        private val _selectedYear = MutableLiveData(Calendar.getInstance().get(Calendar.YEAR))
        val selectedYear: LiveData<Int> = _selectedYear

        private val _selectedMonth = MutableLiveData(Calendar.getInstance().get(Calendar.MONTH) + 1)
        val selectedMonth: LiveData<Int> = _selectedMonth

        private fun reportingInputs(transactions: List<Transaction>) =
            ReportingInputs(
                transactions = transactions,
                preferences =
                    ReportingPreferences(
                        decimalSeparator = preferencesDataSource.getDecimalSeparator(),
                        prefixes =
                            AccountTypePrefixes(
                                assets = preferencesDataSource.getAssetsPrefixes(),
                                liabilities = preferencesDataSource.getLiabilitiesPrefixes(),
                                equity = preferencesDataSource.getEquityPrefixes(),
                                income = preferencesDataSource.getIncomePrefixes(),
                                expenses = preferencesDataSource.getExpensesPrefixes(),
                            ),
                    ),
            )

        val currentMonthCashFlow: LiveData<MonthlyCashFlowCalculator.CashFlowResult> =
            MediatorLiveData<MonthlyCashFlowCalculator.CashFlowResult>().apply {
                fun compute() {
                    val transactions = ledgerRepository.transactions.value ?: return
                    val year = _selectedYear.value ?: return
                    val month = _selectedMonth.value ?: return
                    value =
                        cashFlowCalculator.calculateForMonth(
                            reportingInputs(transactions),
                            year,
                            month,
                        )
                }
                addSource(ledgerRepository.transactions) { compute() }
                addSource(_selectedYear) { compute() }
                addSource(_selectedMonth) { compute() }
                addSource(preferencesDataSource.incomePrefixes) { compute() }
                addSource(preferencesDataSource.expensesPrefixes) { compute() }
            }

        val monthlyHistory: LiveData<List<MonthlyCashFlowCalculator.CashFlowResult>> =
            MediatorLiveData<List<MonthlyCashFlowCalculator.CashFlowResult>>().apply {
                fun compute() {
                    val transactions = ledgerRepository.transactions.value ?: return
                    val year = _selectedYear.value ?: return
                    val month = _selectedMonth.value ?: return
                    // Build the rolling 12-month window ending at (year, month) inclusive.
                    value =
                        (11 downTo 0).map { offset ->
                            // Subtract offset months from the selected month.
                            val totalMonths = (year * 12 + month - 1) - offset
                            val windowYear = totalMonths / 12
                            val windowMonth = totalMonths % 12 + 1
                            cashFlowCalculator.calculateForMonth(
                                reportingInputs(transactions),
                                windowYear,
                                windowMonth,
                            )
                        }
                }
                addSource(ledgerRepository.transactions) { compute() }
                addSource(_selectedYear) { compute() }
                addSource(_selectedMonth) { compute() }
                addSource(preferencesDataSource.incomePrefixes) { compute() }
                addSource(preferencesDataSource.expensesPrefixes) { compute() }
            }

        fun selectMonth(
            year: Int,
            month: Int,
        ) {
            _selectedYear.value = year
            _selectedMonth.value = month
        }

        fun previousMonth() {
            val year = _selectedYear.value ?: return
            val month = _selectedMonth.value ?: return
            if (month == 1) {
                _selectedYear.value = year - 1
                _selectedMonth.value = 12
            } else {
                _selectedMonth.value = month - 1
            }
        }

        fun nextMonth() {
            val year = _selectedYear.value ?: return
            val month = _selectedMonth.value ?: return
            if (month == 12) {
                _selectedYear.value = year + 1
                _selectedMonth.value = 1
            } else {
                _selectedMonth.value = month + 1
            }
        }

        fun getTransactionIndex(transaction: Transaction): Int? =
            ledgerRepository.transactions.value
                ?.indexOf(transaction)
                ?.takeIf { it >= 0 }
    }
