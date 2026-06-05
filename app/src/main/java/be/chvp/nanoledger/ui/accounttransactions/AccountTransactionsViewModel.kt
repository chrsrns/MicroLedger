package be.chvp.nanoledger.ui.accounttransactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import be.chvp.nanoledger.data.LedgerRepository
import be.chvp.nanoledger.data.PreferencesDataSource
import be.chvp.nanoledger.data.Transaction
import be.chvp.nanoledger.data.reporting.AccountBalanceCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AccountTransactionsViewModel
@Inject
constructor(
    application: Application,
    private val ledgerRepository: LedgerRepository,
    private val preferencesDataSource: PreferencesDataSource,
) : AndroidViewModel(application) {
    private val accountBalanceCalculator = AccountBalanceCalculator()

    val decimalSeparator: LiveData<String> = preferencesDataSource.decimalSeparator

    private val _selectedAccount = MutableLiveData<String?>()
    val selectedAccount: LiveData<String?> = _selectedAccount

    private val _selectedCurrency = MutableLiveData<String?>()
    val selectedCurrency: LiveData<String?> = _selectedCurrency

    val accountBalances: LiveData<AccountBalanceCalculator.AccountBalancesResult> =
        ledgerRepository.transactions.map { transactions ->
            accountBalanceCalculator.calculate(
                transactions,
                preferencesDataSource.getDecimalSeparator()
            )
        }

    val accountTransactions: LiveData<List<Transaction>> =
        MediatorLiveData<List<Transaction>>().apply {
            fun computeTransactions() {
                val account = _selectedAccount.value
                val currency = _selectedCurrency.value
                val balances = accountBalances.value
                value =
                    if (account != null && balances != null) {
                        val allBalances =
                            balances.assets + balances.liabilities + balances.equity +
                                    balances.income + balances.expenses
                        if (currency != null) {
                            allBalances
                                .find { it.account == account && it.currency == currency }
                                ?.transactions ?: emptyList()
                        } else {
                            allBalances
                                .filter { it.account == account }
                                .flatMap { it.transactions }
                                .distinct()
                                .sortedBy { it.firstLine }
                        }
                    } else {
                        emptyList()
                    }
            }
            addSource(accountBalances) { computeTransactions() }
            addSource(_selectedAccount) { computeTransactions() }
            addSource(_selectedCurrency) { computeTransactions() }
        }

    fun selectAccount(account: String, currency: String? = null) {
        _selectedAccount.value = account
        _selectedCurrency.value = currency
    }

    fun clearSelectedAccount() {
        _selectedAccount.value = null
        _selectedCurrency.value = null
    }
}
