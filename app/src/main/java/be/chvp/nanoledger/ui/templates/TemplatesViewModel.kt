package be.chvp.nanoledger.ui.templates

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.chvp.nanoledger.data.LedgerRepository
import be.chvp.nanoledger.data.PreferencesDataSource
import be.chvp.nanoledger.data.TransactionTemplate
import be.chvp.nanoledger.ui.util.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class TemplatesViewModel
@Inject
constructor(
    application: Application,
    private val ledgerRepository: LedgerRepository,
    private val preferencesDataSource: PreferencesDataSource,
) : AndroidViewModel(application) {
    val templates: LiveData<List<TransactionTemplate>> = ledgerRepository.templates

    private val _saving = MutableLiveData(false)
    val saving: LiveData<Boolean> = _saving

    private val _latestError = MutableLiveData<Event<IOException>?>(null)
    val latestError: LiveData<Event<IOException>?> = _latestError

    private val _latestReadError = MutableLiveData<Event<IOException>?>(null)
    val latestReadError: LiveData<Event<IOException>?> = _latestReadError

    private val _latestMismatch = MutableLiveData<Event<Int>?>(null)
    val latestMismatch: LiveData<Event<Int>?> = _latestMismatch

    fun deleteTemplate(
        templateId: String,
        onFinish: () -> Unit,
    ) {
        val uri = preferencesDataSource.getFileUri()
        if (uri != null) {
            _saving.value = true
            viewModelScope.launch(IO) {
                ledgerRepository.deleteTemplate(
                    uri,
                    templateId,
                    {
                        _saving.postValue(false)
                        onFinish()
                    },
                    {
                        _saving.postValue(false)
                        _latestMismatch.postValue(Event(0))
                        onFinish()
                    },
                    { error ->
                        _saving.postValue(false)
                        _latestError.postValue(Event(error))
                        onFinish()
                    },
                    { error ->
                        _saving.postValue(false)
                        _latestReadError.postValue(Event(error))
                        onFinish()
                    },
                )
            }
        }
    }

    fun refreshTemplates() {
        val uri = preferencesDataSource.getFileUri()
        if (uri != null) {
            viewModelScope.launch(IO) {
                ledgerRepository.readFrom(
                    uri,
                    {},
                    { error ->
                        _latestReadError.postValue(Event(error))
                    },
                )
            }
        }
    }
}
