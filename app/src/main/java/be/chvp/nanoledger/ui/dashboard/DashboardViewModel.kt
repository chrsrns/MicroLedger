package be.chvp.nanoledger.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
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
        ledgerRepository.transactions.map { transactions ->
            netWorthCalculator.calculate(transactions, preferencesDataSource.getDecimalSeparator())
        }

    val accountBalances: LiveData<AccountBalanceCalculator.AccountBalancesResult> =
        ledgerRepository.transactions.map { transactions ->
            accountBalanceCalculator.calculate(
                transactions,
                preferencesDataSource.getDecimalSeparator(),
            )
        }

    val currentMonthCashFlow: LiveData<MonthlyCashFlowCalculator.CashFlowResult> =
        ledgerRepository.transactions.map { transactions ->
            val today = Calendar.getInstance()
            cashFlowCalculator.calculateForMonth(
                transactions,
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH) + 1,
                preferencesDataSource.getDecimalSeparator(),
            )
        }
}
