package ph.chrsrns.microledger.ui.dashboard

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.map
import dagger.hilt.android.lifecycle.HiltViewModel
import ph.chrsrns.microledger.data.LedgerRepository
import ph.chrsrns.microledger.data.PreferencesDataSource
import ph.chrsrns.microledger.data.reporting.AccountBalanceCalculator
import ph.chrsrns.microledger.data.reporting.MonthlyCashFlowCalculator
import ph.chrsrns.microledger.data.reporting.NetWorthCalculator
import ph.chrsrns.microledger.data.reporting.ReportingInputs
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        application: Application,
        private val preferencesDataSource: PreferencesDataSource,
        private val ledgerRepository: LedgerRepository,
    ) : AndroidViewModel(application) {
        private val netWorthCalculator = NetWorthCalculator()
        private val accountBalanceCalculator = AccountBalanceCalculator()
        private val cashFlowCalculator = MonthlyCashFlowCalculator()

        val fileUri: LiveData<Uri?> = preferencesDataSource.fileUri

        val decimalSeparator: LiveData<String> =
            preferencesDataSource.reportingPreferences.map { it.decimalSeparator }

        val netWorth: LiveData<NetWorthCalculator.NetWorthResult> =
            MediatorLiveData<NetWorthCalculator.NetWorthResult>().apply {
                fun compute() {
                    val transactions = ledgerRepository.transactions.value ?: return
                    val preferences = preferencesDataSource.reportingPreferences.value ?: return
                    value = netWorthCalculator.calculate(ReportingInputs(transactions, preferences))
                }
                addSource(ledgerRepository.transactions) { compute() }
                addSource(preferencesDataSource.reportingPreferences) { compute() }
            }

        val accountBalances: LiveData<AccountBalanceCalculator.AccountBalancesResult> =
            MediatorLiveData<AccountBalanceCalculator.AccountBalancesResult>().apply {
                fun compute() {
                    val transactions = ledgerRepository.transactions.value ?: return
                    val preferences = preferencesDataSource.reportingPreferences.value ?: return
                    value = accountBalanceCalculator.calculate(ReportingInputs(transactions, preferences))
                }
                addSource(ledgerRepository.transactions) { compute() }
                addSource(preferencesDataSource.reportingPreferences) { compute() }
            }

        val currentMonthCashFlow: LiveData<MonthlyCashFlowCalculator.CashFlowResult> =
            MediatorLiveData<MonthlyCashFlowCalculator.CashFlowResult>().apply {
                fun compute() {
                    val transactions = ledgerRepository.transactions.value ?: return
                    val preferences = preferencesDataSource.reportingPreferences.value ?: return
                    val today = Calendar.getInstance()
                    value =
                        cashFlowCalculator.calculateForMonth(
                            ReportingInputs(transactions, preferences),
                            today.get(Calendar.YEAR),
                            today.get(Calendar.MONTH) + 1,
                        )
                }
                addSource(ledgerRepository.transactions) { compute() }
                addSource(preferencesDataSource.reportingPreferences) { compute() }
            }
    }
