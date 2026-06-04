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
            fun filterTransactions() {
                val account = _selectedAccount.value
                val currency = _selectedCurrency.value
                val transactions = ledgerRepository.transactions.value
                value = if (account != null && transactions != null) {
                    transactions.filter { transaction ->
                        transaction.postings.any { posting ->
                            posting.account == account &&
                                    (currency == null || posting.amount?.currency == currency)
                        }
                    }
                } else {
                    emptyList()
                }
            }
            addSource(ledgerRepository.transactions) { filterTransactions() }
            addSource(_selectedAccount) { filterTransactions() }
            addSource(_selectedCurrency) { filterTransactions() }
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
