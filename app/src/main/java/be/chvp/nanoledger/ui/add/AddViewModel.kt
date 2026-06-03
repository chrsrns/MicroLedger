package be.chvp.nanoledger.ui.add

import android.app.Application
import androidx.lifecycle.viewModelScope
import be.chvp.nanoledger.data.LedgerRepository
import be.chvp.nanoledger.data.PreferencesDataSource
import be.chvp.nanoledger.ui.common.TransactionFormViewModel
import be.chvp.nanoledger.ui.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AddViewModel
    @Inject
    constructor(
        application: Application,
        preferencesDataSource: PreferencesDataSource,
        ledgerRepository: LedgerRepository,
    ) : TransactionFormViewModel(application, preferencesDataSource, ledgerRepository) {
        fun loadTransactionFromIndex(index: Int) {
            setFromTransaction(ledgerRepository.transactions.value!![index])
            // When copying, set the date to today
            setDate(Date())
        }

        /**
         * Pre-fills the add-transaction form from the saved template identified by [templateId].
         *
         * Copies the payee, note, status, code, and posting accounts from the template, but clears
         * all posting amounts (preserving currencies) so the user can enter the actual values.
         * The date is left as today's date (the default). Does nothing if no template with the
         * given ID exists.
         */
        fun loadFromTemplate(templateId: String) {
            val template =
                ledgerRepository.templates.value
                    ?.find { it.id == templateId } ?: return

            setPayee(template.payee)
            setNote(template.note)
            setStatus(template.status)
            setCode(template.code)

            val postingsWithEmptyAmounts =
                template.postings.map { posting ->
                    posting.withAmount(
                        if (posting.amount != null) {
                            be.chvp.nanoledger.data
                                .Amount("", posting.amount.currency, "")
                        } else {
                            null
                        },
                    )
                }
            setPostings(postingsWithEmptyAmounts)
        }

        override fun save(onFinish: suspend () -> Unit) {
            val uri = preferencesDataSource.getFileUri()
            if (uri != null) {
                setSaving(true)
                viewModelScope.launch(IO) {
                    ledgerRepository.appendTo(
                        uri,
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
