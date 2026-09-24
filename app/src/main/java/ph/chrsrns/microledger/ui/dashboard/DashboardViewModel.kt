package ph.chrsrns.microledger.ui.dashboard

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import ph.chrsrns.microledger.data.AccountTypePrefixes
import ph.chrsrns.microledger.data.LedgerRepository
import ph.chrsrns.microledger.data.PreferencesDataSource
import ph.chrsrns.microledger.data.ReportingPreferences
import ph.chrsrns.microledger.data.Transaction
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

        val decimalSeparator: LiveData<String> = preferencesDataSource.decimalSeparator

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

        val netWorth: LiveData<NetWorthCalculator.NetWorthResult> =
            MediatorLiveData<NetWorthCalculator.NetWorthResult>().apply {
                fun compute() {
                    val transactions = ledgerRepository.transactions.value ?: return
                    value = netWorthCalculator.calculate(reportingInputs(transactions))
                }
                addSource(ledgerRepository.transactions) { compute() }
                addSource(preferencesDataSource.assetsPrefixes) { compute() }
                addSource(preferencesDataSource.liabilitiesPrefixes) { compute() }
            }

        val accountBalances: LiveData<AccountBalanceCalculator.AccountBalancesResult> =
            MediatorLiveData<AccountBalanceCalculator.AccountBalancesResult>().apply {
                fun compute() {
                    val transactions = ledgerRepository.transactions.value ?: return
                    value = accountBalanceCalculator.calculate(reportingInputs(transactions))
                }
                addSource(ledgerRepository.transactions) { compute() }
                addSource(preferencesDataSource.assetsPrefixes) { compute() }
                addSource(preferencesDataSource.liabilitiesPrefixes) { compute() }
                addSource(preferencesDataSource.equityPrefixes) { compute() }
                addSource(preferencesDataSource.incomePrefixes) { compute() }
                addSource(preferencesDataSource.expensesPrefixes) { compute() }
            }

        val currentMonthCashFlow: LiveData<MonthlyCashFlowCalculator.CashFlowResult> =
            MediatorLiveData<MonthlyCashFlowCalculator.CashFlowResult>().apply {
                fun compute() {
                    val transactions = ledgerRepository.transactions.value ?: return
                    val today = Calendar.getInstance()
                    value =
                        cashFlowCalculator.calculateForMonth(
                            reportingInputs(transactions),
                            today.get(Calendar.YEAR),
                            today.get(Calendar.MONTH) + 1,
                        )
                }
                addSource(ledgerRepository.transactions) { compute() }
                addSource(preferencesDataSource.incomePrefixes) { compute() }
                addSource(preferencesDataSource.expensesPrefixes) { compute() }
            }
    }
