package ph.chrsrns.microledger.ui.cashflowtransactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import ph.chrsrns.microledger.data.LedgerRepository
import ph.chrsrns.microledger.data.PreferencesDataSource
import ph.chrsrns.microledger.data.reporting.MonthlyCashFlowCalculator
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

    private val _selectedYear = MutableLiveData(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: LiveData<Int> = _selectedYear

    private val _selectedMonth = MutableLiveData(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val selectedMonth: LiveData<Int> = _selectedMonth

    val currentMonthCashFlow: LiveData<MonthlyCashFlowCalculator.CashFlowResult> =
        MediatorLiveData<MonthlyCashFlowCalculator.CashFlowResult>().apply {
            fun compute() {
                val transactions = ledgerRepository.transactions.value ?: return
                val year = _selectedYear.value ?: return
                val month = _selectedMonth.value ?: return
                value =
                    cashFlowCalculator.calculateForMonth(
                        transactions,
                        year,
                        month,
                        preferencesDataSource.getDecimalSeparator(),
                        preferencesDataSource.getIncomePrefixes(),
                        preferencesDataSource.getExpensesPrefixes(),
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
                val decimalSeparator = preferencesDataSource.getDecimalSeparator()
                val incomePrefixes = preferencesDataSource.getIncomePrefixes()
                val expensesPrefixes = preferencesDataSource.getExpensesPrefixes()
                // Build the rolling 12-month window ending at (year, month) inclusive.
                value =
                    (11 downTo 0).map { offset ->
                        // Subtract offset months from the selected month.
                        val totalMonths = (year * 12 + month - 1) - offset
                        val windowYear = totalMonths / 12
                        val windowMonth = totalMonths % 12 + 1
                        cashFlowCalculator.calculateForMonth(
                            transactions,
                            windowYear,
                            windowMonth,
                            decimalSeparator,
                            incomePrefixes,
                            expensesPrefixes,
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
}
