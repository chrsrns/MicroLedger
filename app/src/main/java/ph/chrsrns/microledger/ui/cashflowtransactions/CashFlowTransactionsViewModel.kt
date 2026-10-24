package ph.chrsrns.microledger.ui.cashflowtransactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import dagger.hilt.android.lifecycle.HiltViewModel
import ph.chrsrns.microledger.data.AccountTypePrefixes
import ph.chrsrns.microledger.data.LedgerRepository
import ph.chrsrns.microledger.data.PreferencesDataSource
import ph.chrsrns.microledger.data.Transaction
import ph.chrsrns.microledger.data.reporting.MonthlyCashFlowCalculator
import ph.chrsrns.microledger.data.reporting.ReportingInputs
import ph.chrsrns.microledger.ui.util.reportingLiveData
import ph.chrsrns.microledger.di.MonthProvider
import javax.inject.Inject

@HiltViewModel
class CashFlowTransactionsViewModel
    @Inject
    constructor(
        application: Application,
        private val ledgerRepository: LedgerRepository,
        private val preferencesDataSource: PreferencesDataSource,
        private val monthProvider: MonthProvider,
    ) : AndroidViewModel(application) {
        private val cashFlowCalculator = MonthlyCashFlowCalculator()

        val decimalSeparator: LiveData<String> =
            preferencesDataSource.reportingPreferences.map { it.decimalSeparator }
        val prefixes: LiveData<AccountTypePrefixes> =
            preferencesDataSource.reportingPreferences.map { it.prefixes }

        private val _currentYear = MutableLiveData(monthProvider.current().year)
        val currentYear: LiveData<Int> = _currentYear

        private val _selectedYear = MutableLiveData(monthProvider.current().year)
        val selectedYear: LiveData<Int> = _selectedYear

        private val _selectedMonth = MutableLiveData(monthProvider.current().month)
        val selectedMonth: LiveData<Int> = _selectedMonth

        val currentMonthCashFlow: LiveData<MonthlyCashFlowCalculator.CashFlowResult> =
            reportingLiveData(
                ledgerRepository.transactions,
                preferencesDataSource.reportingPreferences,
                _selectedYear,
                _selectedMonth,
            ) { inputs ->
                val year = _selectedYear.value ?: return@reportingLiveData null
                val month = _selectedMonth.value ?: return@reportingLiveData null
                cashFlowCalculator.calculateForMonth(inputs, year, month)
            }

        val monthlyHistory: LiveData<List<MonthlyCashFlowCalculator.CashFlowResult>> =
            reportingLiveData(
                ledgerRepository.transactions,
                preferencesDataSource.reportingPreferences,
                _selectedYear,
                _selectedMonth,
            ) { inputs ->
                val year = _selectedYear.value ?: return@reportingLiveData null
                val month = _selectedMonth.value ?: return@reportingLiveData null
                cashFlowCalculator.calculateRollingWindow(inputs, year, month, 12)
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
