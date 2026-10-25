package ph.chrsrns.microledger.ui.dashboard

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import dagger.hilt.android.lifecycle.HiltViewModel
import ph.chrsrns.microledger.data.LedgerRepository
import ph.chrsrns.microledger.data.PreferencesDataSource
import ph.chrsrns.microledger.data.reporting.AccountBalanceCalculator
import ph.chrsrns.microledger.data.reporting.MonthlyCashFlowCalculator
import ph.chrsrns.microledger.data.reporting.NetWorthCalculator
import ph.chrsrns.microledger.di.MonthProvider
import ph.chrsrns.microledger.ui.util.reportingLiveData
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        application: Application,
        private val preferencesDataSource: PreferencesDataSource,
        private val ledgerRepository: LedgerRepository,
        private val monthProvider: MonthProvider,
    ) : AndroidViewModel(application) {
        private val netWorthCalculator = NetWorthCalculator()
        private val accountBalanceCalculator = AccountBalanceCalculator()
        private val cashFlowCalculator = MonthlyCashFlowCalculator()

        val fileUri: LiveData<Uri?> = preferencesDataSource.fileUri

        val decimalSeparator: LiveData<String> =
            preferencesDataSource.reportingPreferences.map { it.decimalSeparator }

        val netWorth: LiveData<List<NetWorthCalculator.CurrencyNetWorth>> =
            reportingLiveData(
                ledgerRepository.transactions,
                preferencesDataSource.reportingPreferences,
            ) { netWorthCalculator.calculate(it) }

        val accountBalances: LiveData<AccountBalanceCalculator.AccountBalancesResult> =
            reportingLiveData(
                ledgerRepository.transactions,
                preferencesDataSource.reportingPreferences,
            ) { accountBalanceCalculator.calculate(it) }

        val currentMonthCashFlow: LiveData<MonthlyCashFlowCalculator.CashFlowResult> =
            reportingLiveData(
                ledgerRepository.transactions,
                preferencesDataSource.reportingPreferences,
            ) {
                val current = monthProvider.current()
                cashFlowCalculator.calculateForMonth(it, current.year, current.month)
            }
    }
