package ph.chrsrns.microledger.ui.add

import android.app.Application
import dagger.hilt.android.lifecycle.HiltViewModel
import ph.chrsrns.microledger.data.LedgerRepository
import ph.chrsrns.microledger.data.PreferencesDataSource
import ph.chrsrns.microledger.ui.common.TransactionFormViewModel
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
                    posting.copy(
                        amount =
                            if (posting.amount != null) {
                                ph.chrsrns.microledger.data
                                    .Amount("", posting.amount.currency, "")
                            } else {
                                null
                            },
                    )
                }
            setPostings(postingsWithEmptyAmounts)
        }

        override fun save(onFinish: suspend () -> Unit) =
            saveWith({ uri -> ledgerRepository.appendTo(uri, toTransactionString()) }, onFinish)
    }
