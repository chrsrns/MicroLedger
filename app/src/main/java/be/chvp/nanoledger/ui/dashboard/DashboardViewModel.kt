package be.chvp.nanoledger.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import be.chvp.nanoledger.data.LedgerRepository
import be.chvp.nanoledger.data.PreferencesDataSource
import be.chvp.nanoledger.data.reporting.AccountBalanceCalculator
import be.chvp.nanoledger.data.reporting.MonthlyCashFlowCalculator
import be.chvp.nanoledger.data.reporting.NetWorthCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
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

    val decimalSeparator: LiveData<String> = preferencesDataSource.decimalSeparator

    val netWorth: LiveData<NetWorthCalculator.NetWorthResult> =
        MediatorLiveData<NetWorthCalculator.NetWorthResult>().apply {
            fun compute() {
                val transactions = ledgerRepository.transactions.value ?: return
                value =
                    netWorthCalculator.calculate(
                        transactions,
                        preferencesDataSource.getDecimalSeparator(),
                        preferencesDataSource.getAssetsPrefixes(),
                        preferencesDataSource.getLiabilitiesPrefixes(),
                    )
            }
            addSource(ledgerRepository.transactions) { compute() }
            addSource(preferencesDataSource.assetsPrefixes) { compute() }
            addSource(preferencesDataSource.liabilitiesPrefixes) { compute() }
        }

    val accountBalances: LiveData<AccountBalanceCalculator.AccountBalancesResult> =
        MediatorLiveData<AccountBalanceCalculator.AccountBalancesResult>().apply {
            fun compute() {
                val transactions = ledgerRepository.transactions.value ?: return
                value =
                    accountBalanceCalculator.calculate(
                        transactions,
                        preferencesDataSource.getDecimalSeparator(),
                        preferencesDataSource.getAssetsPrefixes(),
                        preferencesDataSource.getLiabilitiesPrefixes(),
                        preferencesDataSource.getEquityPrefixes(),
                        preferencesDataSource.getIncomePrefixes(),
                        preferencesDataSource.getExpensesPrefixes(),
                    )
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
                        transactions,
                        today.get(Calendar.YEAR),
                        today.get(Calendar.MONTH) + 1,
                        preferencesDataSource.getDecimalSeparator(),
                        preferencesDataSource.getIncomePrefixes(),
                        preferencesDataSource.getExpensesPrefixes(),
                    )
            }
            addSource(ledgerRepository.transactions) { compute() }
            addSource(preferencesDataSource.incomePrefixes) { compute() }
            addSource(preferencesDataSource.expensesPrefixes) { compute() }
        }
}
