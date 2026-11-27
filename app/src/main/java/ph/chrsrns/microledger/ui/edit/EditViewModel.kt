package ph.chrsrns.microledger.ui.edit

import android.app.Application
import dagger.hilt.android.lifecycle.HiltViewModel
import ph.chrsrns.microledger.data.LedgerRepository
import ph.chrsrns.microledger.data.PreferencesDataSource
import ph.chrsrns.microledger.data.Transaction
import ph.chrsrns.microledger.ui.common.TransactionFormViewModel
import javax.inject.Inject

@HiltViewModel
class EditViewModel
    @Inject
    constructor(
        application: Application,
        preferencesDataSource: PreferencesDataSource,
        ledgerRepository: LedgerRepository,
    ) : TransactionFormViewModel(application, preferencesDataSource, ledgerRepository) {
        private lateinit var sourceTransaction: Transaction

        fun loadTransactionFromIndex(index: Int) {
            sourceTransaction = ledgerRepository.transactions.value!![index]
            setFromTransaction(sourceTransaction)
        }

        override fun save(onFinish: suspend () -> Unit) =
            saveWith(
                { uri ->
                    ledgerRepository.replaceTransaction(uri, sourceTransaction, toTransactionString())
                },
                onFinish,
            )
    }
