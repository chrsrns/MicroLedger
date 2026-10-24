package ph.chrsrns.microledger.ui.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ph.chrsrns.microledger.data.AccountTypePrefixes
import ph.chrsrns.microledger.data.Amount
import ph.chrsrns.microledger.data.Cost
import ph.chrsrns.microledger.data.CostType
import ph.chrsrns.microledger.data.LedgerRepository
import ph.chrsrns.microledger.data.Posting
import ph.chrsrns.microledger.data.PreferencesDataSource
import ph.chrsrns.microledger.data.Transaction
import ph.chrsrns.microledger.data.TransactionTemplate
import ph.chrsrns.microledger.data.reporting.parseQuantity
import ph.chrsrns.microledger.ui.util.Event
import java.io.IOException
import java.math.BigDecimal
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import java.util.UUID

val dateFormat = SimpleDateFormat("yyyy-MM-dd")

abstract class TransactionFormViewModel(
    application: Application,
    protected val preferencesDataSource: PreferencesDataSource,
    protected val ledgerRepository: LedgerRepository,
) : AndroidViewModel(application) {
    private val _saving = MutableLiveData(false)
    val saving: LiveData<Boolean> = _saving

    protected fun setSaving(saving: Boolean) {
        _saving.value = saving
    }

    protected fun postSaving(saving: Boolean) {
        _saving.postValue(saving)
    }

    private val _date = MutableLiveData(Date())
    val date: LiveData<Date> = _date
    val formattedDate: LiveData<String> = _date.map { dateFormat.format(it) }

    private val _status =
        MutableLiveData(
            if (preferencesDataSource.getTransactionStatusPresentByDefault()) preferencesDataSource.getDefaultStatus() else null,
        )
    val status: LiveData<String?> = _status

    private val _code =
        MutableLiveData(if (preferencesDataSource.getTransactionCodePresentByDefault()) "" else null)
    val code: LiveData<String?> = _code

    private val _payee =
        MutableLiveData(if (preferencesDataSource.getTransactionPayeePresentByDefault()) "" else null)
    val payee: LiveData<String?> = _payee

    private val _note =
        MutableLiveData(if (preferencesDataSource.getTransactionNotePresentByDefault()) "" else null)
    val note: LiveData<String?> = _note

    val possiblePayees: LiveData<List<String>> =
        ledgerRepository.payees.switchMap { payees ->
            ledgerRepository.notes.switchMap { notes ->
                note.switchMap { note ->
                    val listToSearch: Set<String> = if (note == null) payees + notes else payees
                    payee.map { search ->
                        listToSearch
                            .filter {
                                it.contains(
                                    (search ?: ""),
                                    ignoreCase = true,
                                )
                            }.sorted()
                    }
                }
            }
        }

    val possibleNotes: LiveData<List<String>> =
        ledgerRepository.notes.switchMap { notes ->
            ledgerRepository.payees.switchMap { payees ->
                payee.switchMap { payee ->
                    val listToSearch: Set<String> = if (payee == null) notes + payees else notes
                    note.map { search ->
                        listToSearch
                            .filter {
                                it.contains(
                                    (search ?: ""),
                                    ignoreCase = true,
                                )
                            }.sorted()
                    }
                }
            }
        }

    private val _currencyEnabled =
        MutableLiveData<Boolean>(preferencesDataSource.getTransactionCurrenciesPresentByDefault())
    val currencyEnabled: LiveData<Boolean> = _currencyEnabled

    private val _postings = MutableLiveData(listOf(newPosting()))
    val postings: LiveData<List<Posting>> = _postings
    val accounts: LiveData<List<String>> = ledgerRepository.accounts.map { it.sorted() }
    val unbalancedAmount: LiveData<String> =
        MediatorLiveData<String>().apply {
            fun compute() {
                val ps = postings.value ?: return
                val relevantPostings =
                    ps.filter { p ->
                        !p.isVirtual() && !p.isComment() && !p.account.isNullOrBlank()
                    }
                if (relevantPostings.any { it.assertion != null && it.amount == null }) {
                    value = ""
                    return
                }
                if (relevantPostings.any { it.cost != null }) {
                    value = ""
                    return
                }
                if (relevantPostings
                        .mapNotNull { p -> p.amount }
                        .map { it.currency }
                        .distinct()
                        .count() > 1
                ) {
                    value = ""
                    return
                }

                value =
                    relevantPostings
                        .mapNotNull { p -> p.amount }
                        .map { p -> p.quantity }
                        .map { quantity ->
                            parseQuantity(quantity, preferencesDataSource.getDecimalSeparator())
                        }.fold(BigDecimal.ZERO) { l, r -> l + r }
                        .negate()
                        .let { num ->
                            if (num == BigDecimal.ZERO.setScale(num.scale())) {
                                ""
                            } else {
                                num.toString().replace(
                                    ".",
                                    preferencesDataSource.getDecimalSeparator(),
                                )
                            }
                        }
            }
            addSource(postings) { compute() }
            addSource(preferencesDataSource.decimalSeparator) { compute() }
        }

    val valid: LiveData<Boolean> =
        postings.switchMap { postings ->
            unbalancedAmount.map { unbalancedAmount ->
                val withoutSentinel = postings.dropLast(1)
                val realPostings =
                    withoutSentinel.filter { !it.isVirtual() && !it.isComment() }
                if (realPostings.size < 2) {
                    return@map false
                }
                if (realPostings.any { it.account.isNullOrBlank() }) {
                    return@map false
                }
                val nonComment = withoutSentinel.filter { !it.isComment() }
                // If there is an unbalanced amount, and there are no postings with an empty amount, it's invalid
                if (unbalancedAmount != "" &&
                    nonComment.all { (it.amount?.quantity ?: "") != "" }
                ) {
                    return@map false
                }
                // If there are multiple postings with an empty amount and no assertions, it's invalid
                if (
                    nonComment.count {
                        (it.amount?.quantity ?: "") == "" &&
                            (it.assertion?.quantity ?: "") == ""
                    } > 1
                ) {
                    return@map false
                }
                return@map true
            }
        }

    private val _latestError = MutableLiveData<Event<IOException>?>(null)
    val latestError: LiveData<Event<IOException>?> = _latestError

    protected fun postError(error: Event<IOException>) {
        _latestError.postValue(error)
    }

    private val _latestMismatch = MutableLiveData<Event<Int>?>(null)
    val latestMismatch: LiveData<Event<Int>?> = _latestMismatch

    protected fun postMismatch(mismatch: Event<Int>) {
        _latestMismatch.postValue(mismatch)
    }

    val currencyBeforeAmount: LiveData<Boolean> = preferencesDataSource.currencyBeforeAmount
    val currencyAmountSpacing: LiveData<Boolean> =
        preferencesDataSource.spacingBetweenCurrencyAndAmount
    val decimalSeparator: LiveData<String> = preferencesDataSource.decimalSeparator
    val defaultCurrency: LiveData<String> = preferencesDataSource.defaultCurrency
    val prefixes: LiveData<AccountTypePrefixes> =
        preferencesDataSource.reportingPreferences.map { it.prefixes }

    protected fun toTransactionString(): String {
        val postingWidth = preferencesDataSource.getPostingWidth()
        val currencyBeforeAmount = preferencesDataSource.getCurrencyBeforeAmount()
        val currencyAmountSpacing = preferencesDataSource.getCurrencyAmountSpacing()

        val transaction =
            Transaction(
                0,
                0,
                dateFormat.format(date.value!!),
                status.value,
                code.value,
                payee.value,
                note.value,
                postings.value!!
                    .dropLast(1)
                    .filter { it.isVirtual() || it.isComment() || !it.account.isNullOrBlank() },
            )
        return transaction.format(
            postingWidth,
            currencyBeforeAmount,
            currencyAmountSpacing,
            currencyEnabled.value ?: true,
        )
    }

    abstract fun save(onFinish: suspend () -> Unit)

    fun saveAsTemplate(
        name: String,
        onFinish: suspend (String?) -> Unit,
    ) {
        val uri = preferencesDataSource.getFileUri()
        if (uri == null) {
            viewModelScope.launch { onFinish(null) }
            return
        }
        setSaving(true)
        viewModelScope.launch {
            val templateId = UUID.randomUUID().toString()
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
                    postings = templatePostings(),
                )
            ledgerRepository.addTemplate(
                uri,
                template,
                onFinish = {
                    postSaving(false)
                    viewModelScope.launch { onFinish(templateId) }
                },
                onMismatch = {
                    postSaving(false)
                    postMismatch(Event(0))
                    viewModelScope.launch { onFinish(null) }
                },
                onWriteError = { e: IOException ->
                    postSaving(false)
                    postError(Event(e))
                    viewModelScope.launch { onFinish(null) }
                },
                onReadError = { e: IOException ->
                    postSaving(false)
                    postError(Event(e))
                    viewModelScope.launch { onFinish(null) }
                },
            )
        }
    }

    fun setFromTransaction(transaction: Transaction) {
        setDate(transaction.date)
        setStatus(transaction.status)
        if (transaction.status == null && preferencesDataSource.getTransactionStatusPresentByDefault()) {
            setStatus(" ")
        }
        setCode(transaction.code)
        setPayee(transaction.payee)
        setNote(transaction.note)
        if (
            (
                preferencesDataSource.getTransactionPayeePresentByDefault() &&
                    !preferencesDataSource.getTransactionNotePresentByDefault() &&
                    transaction.payee == null
            ) ||
            (
                preferencesDataSource.getTransactionNotePresentByDefault() &&
                    !preferencesDataSource.getTransactionPayeePresentByDefault() &&
                    transaction.note == null
            )
        ) {
            setPayee(transaction.note)
            setNote(transaction.payee)
        }
        setPostings(transaction.postings)
        _currencyEnabled.value =
            transaction.postings.any {
                (it.amount?.currency ?: "") != "" ||
                    (it.cost?.amount?.currency ?: "") != "" ||
                    (it.assertion?.currency ?: "") != "" ||
                    (it.assertionCost?.amount?.currency ?: "") != ""
            }
    }

    fun setDate(dateMillis: Long) {
        _date.value = Date(dateMillis - TimeZone.getDefault().getOffset(dateMillis))
    }

    fun setDate(newDate: Date) {
        _date.value = newDate
    }

    fun setDate(newDate: String) {
        val parsed = dateFormat.parse(newDate, ParsePosition(0))
        if (parsed != null) {
            _date.value = parsed
        }
    }

    fun setStatus(newStatus: String?) {
        _status.value = newStatus
    }

    fun setCode(newCode: String?) {
        _code.value = newCode
    }

    fun setPayee(newPayee: String?) {
        _payee.value = newPayee
    }

    fun setNote(newNote: String?) {
        _note.value = newNote
    }

    fun toggleStatus() {
        _status.value = if (status.value == null) " " else null
    }

    fun toggleCode() {
        _code.value = if (code.value == null) "" else null
    }

    fun togglePayee() {
        _payee.value = if (payee.value == null) "" else null
    }

    fun toggleNote() {
        _note.value = if (note.value == null) "" else null
    }

    fun toggleCurrency() {
        _currencyEnabled.value = !currencyEnabled.value!!
    }

    fun setPostings(newPostings: List<Posting>) {
        _postings.value = ArrayList(withSentinel(newPostings, ::newPosting))
    }

    fun addPosting() {
        _postings.value =
            ArrayList(
                insertPosting(
                    postings.value ?: listOf(),
                    Posting("", null, null, null, null, null),
                    ::newPosting,
                ),
            )
    }

    fun setPosting(
        index: Int,
        posting: Posting,
    ) {
        updatePosting(index) { posting }
    }

    fun updatePosting(
        index: Int,
        transform: (Posting) -> Posting,
    ) {
        val stored = postings.value ?: return
        _postings.value = ArrayList(updatePostingAt(stored, index, ::newPosting, transform))
    }

    fun removePosting(index: Int) {
        val stored = postings.value ?: return
        _postings.value = ArrayList(removePostingAt(stored, index, ::newPosting))
    }

    fun filterPostings(postings: List<Posting>): ArrayList<Posting> =
        ArrayList(withSentinel(postings, ::newPosting))

    protected fun templatePostings(): List<Posting> {
        val current = postings.value ?: return emptyList()
        if (current.isEmpty()) return current
        val withoutSentinel = current.dropLast(1)
        return withoutSentinel.filter {
            it.isVirtual() || it.isComment() || !it.account.isNullOrBlank()
        }
    }

    fun defaultAmount() = Amount("", preferencesDataSource.getDefaultCurrency(), "")

    fun newPosting(): Posting =
        Posting(
            "",
            if (preferencesDataSource.getPostingAmountPresentByDefault()) defaultAmount() else null,
            if (preferencesDataSource.getPostingCostPresentByDefault()) {
                Cost(
                    defaultAmount(),
                    CostType.UNIT,
                )
            } else {
                null
            },
            if (preferencesDataSource.getPostingAssertionPresentByDefault()) defaultAmount() else null,
            if (preferencesDataSource.getPostingAssertionCostPresentByDefault()) {
                Cost(
                    defaultAmount(),
                    CostType.UNIT,
                )
            } else {
                null
            },
            if (preferencesDataSource.getPostingCommentPresentByDefault()) "" else null,
        )
}
