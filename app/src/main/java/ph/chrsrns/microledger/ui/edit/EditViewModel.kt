package ph.chrsrns.microledger.ui.edit

import android.app.Application
import androidx.lifecycle.viewModelScope
import ph.chrsrns.microledger.data.LedgerRepository
import ph.chrsrns.microledger.data.PreferencesDataSource
import ph.chrsrns.microledger.data.Transaction
import ph.chrsrns.microledger.ui.common.TransactionFormViewModel
import ph.chrsrns.microledger.ui.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
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

        override fun save(onFinish: suspend () -> Unit) {
            val uri = preferencesDataSource.getFileUri()
            if (uri != null) {
                setSaving(true)
                viewModelScope.launch(IO) {
                    ledgerRepository.replaceTransaction(
                        uri,
                        sourceTransaction,
                        toTransactionString(),
                        {
                            postSaving(false)
                            onFinish()
                        },
                        {
                            postSaving(false)
                            postMismatch(Event(1))
                        },
                        {
                            postSaving(false)
                            postError(Event(it))
                        },
                        {
                            // We ignore a read error, the write went through so the
                            // only thing the user will experience is the
                            // transaction not being in the transaction
                            // overview. Which isn't optimal, but not a big problem
                            // either.
                            postSaving(false)
                            onFinish()
                        },
                    )
                }
            }
        }
    }
