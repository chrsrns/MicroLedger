package be.chvp.nanoledger.ui.templates

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.chvp.nanoledger.data.LedgerRepository
import be.chvp.nanoledger.data.PreferencesDataSource
import be.chvp.nanoledger.data.TransactionTemplate
import be.chvp.nanoledger.ui.common.TransactionFormViewModel
import be.chvp.nanoledger.ui.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

// Note: UUID and IO are used by updateTemplate method

@HiltViewModel
class TemplateFormViewModel
@Inject
constructor(
    application: Application,
    preferencesDataSource: PreferencesDataSource,
    ledgerRepository: LedgerRepository,
) : TransactionFormViewModel(application, preferencesDataSource, ledgerRepository) {
    val templates = ledgerRepository.templates

    private val _templateNotFound = MutableLiveData<Event<Boolean>?>(null)
    val templateNotFound: LiveData<Event<Boolean>?> = _templateNotFound

    fun loadTemplate(templateId: String) {
        val template = ledgerRepository.templates.value?.find { it.id == templateId }
        if (template == null) {
            _templateNotFound.postValue(Event(true))
            return
        }

        setPayee(template.payee)
        setNote(template.note)
        setStatus(template.status)
        setCode(template.code)
        setPostings(template.postings)
    }

    fun updateTemplate(
        templateId: String,
        name: String,
        onFinish: suspend () -> Unit,
    ) {
        val uri = preferencesDataSource.getFileUri()
        if (uri == null) {
            viewModelScope.launch { onFinish() }
            return
        }
        setSaving(true)
        viewModelScope.launch(IO) {
            val template =
                TransactionTemplate(
                    firstLine = -1,
                    lastLine = -1,
                    id = templateId,
                    name = name,
                    payee = payee.value,
                    note = note.value,
                    status = status.value,
                    code = code.value,
                    postings = postings.value ?: emptyList(),
                )
            ledgerRepository.updateTemplate(
                uri,
                template,
                onFinish = {
                    postSaving(false)
                    viewModelScope.launch { onFinish() }
                },
                onMismatch = {
                    postSaving(false)
                    postMismatch(Event(0))
                    viewModelScope.launch { onFinish() }
                },
                onWriteError = { e: IOException ->
                    postSaving(false)
                    postError(Event(e))
                    viewModelScope.launch { onFinish() }
                },
                onReadError = { e: IOException ->
                    postSaving(false)
                    postError(Event(e))
                    viewModelScope.launch { onFinish() }
                },
            )
        }
    }

    override fun save(onFinish: suspend () -> Unit) {
        // Templates don't use the standard save method
        // They use saveAsTemplate or updateTemplate instead
        // TODO: Add a new abstract class that the abstract class
        //          `TransactionFormViewModel` and to-be-written
        //          abstract class `TemplateFormViewModel`
        viewModelScope.launch { onFinish() }
    }
}
