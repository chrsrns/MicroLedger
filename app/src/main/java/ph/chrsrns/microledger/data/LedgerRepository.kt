package ph.chrsrns.microledger.data

import android.app.Application
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import ph.chrsrns.microledger.data.parser.TemplateParser
import ph.chrsrns.microledger.data.parser.extractTransactions
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LedgerRepository
    @Inject
    constructor(
        private val context: Application,
    ) {
        private val _fileContents = MutableLiveData<List<String>>(emptyList())
        val fileContents: LiveData<List<String>> = _fileContents
        private val _transactions = MutableLiveData<List<Transaction>>(emptyList())
        val transactions: LiveData<List<Transaction>> = _transactions
        private val _templates = MutableLiveData<List<TransactionTemplate>>(emptyList())
        val templates: LiveData<List<TransactionTemplate>> = _templates
        val accounts: LiveData<Set<String>> =
            transactions.map { txs ->
                val result = HashSet<String>()
                txs.forEach { tx -> result.addAll(tx.postings.mapNotNull { p -> p.account }) }
                result
            }
        val payees: LiveData<Set<String>> =
            transactions.map { txs ->
                HashSet(
                    txs
                        .map { tx -> tx.payee }
                        .filter { payee -> payee != null }
                        .map { payee -> payee!! },
                )
            }
        val notes: LiveData<Set<String>> =
            transactions.map { txs ->
                HashSet(
                    txs.map { tx -> tx.note }.filter { note -> note != null }.map { note -> note!! },
                )
            }

        fun matches(fileUri: Uri): Boolean {
            val result = ArrayList<String>()
            fileUri
                .let { context.contentResolver.openInputStream(it) }
                ?.let { BufferedReader(InputStreamReader(it)) }
                ?.use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        result.add(line)
                        line = reader.readLine()
                    }
                }
            return result == fileContents.value
        }

        suspend fun deleteTransaction(
            fileUri: Uri,
            transaction: Transaction,
            onFinish: suspend () -> Unit,
            onMismatch: suspend () -> Unit,
            onWriteError: suspend (IOException) -> Unit,
            onReadError: suspend (IOException) -> Unit,
        ) = writeTransformed(
            fileUri,
            emit = { emitDeleteTransaction(it, transaction.firstLine, transaction.lastLine) },
            onFinish,
            onMismatch,
            onWriteError,
            onReadError,
        )

        suspend fun replaceTransaction(
            fileUri: Uri,
            transaction: Transaction,
            text: String,
            onFinish: suspend () -> Unit,
            onMismatch: suspend () -> Unit,
            onWriteError: suspend (IOException) -> Unit,
            onReadError: suspend (IOException) -> Unit,
        ) = writeTransformed(
            fileUri,
            emit = {
                emitReplaceTransaction(it, transaction.firstLine, transaction.lastLine, text)
            },
            onFinish,
            onMismatch,
            onWriteError,
            onReadError,
        )

        suspend fun appendTo(
            fileUri: Uri,
            text: String,
            onFinish: suspend () -> Unit,
            onMismatch: suspend () -> Unit,
            onWriteError: suspend (IOException) -> Unit,
            onReadError: suspend (IOException) -> Unit,
        ) = writeTransformed(
            fileUri,
            emit = { emitAppend(it, text) },
            onFinish,
            onMismatch,
            onWriteError,
            onReadError,
        )

        private suspend fun writeTransformed(
            fileUri: Uri,
            emit: (List<String>) -> String,
            onFinish: suspend () -> Unit,
            onMismatch: suspend () -> Unit,
            onWriteError: suspend (IOException) -> Unit,
            onReadError: suspend (IOException) -> Unit,
        ) {
            try {
                if (!matches(fileUri)) {
                    onMismatch()
                } else {
                    context.contentResolver
                        .openOutputStream(fileUri, "wt")
                        ?.let { OutputStreamWriter(it) }
                        ?.use { it.write(emit(fileContents.value!!)) }
                    readFrom(fileUri, onFinish, onReadError)
                }
            } catch (e: IOException) {
                onWriteError(e)
            }
        }

        suspend fun readFrom(
            fileUri: Uri,
            onFinish: suspend () -> Unit,
            onReadError: suspend (IOException) -> Unit,
        ) {
            try {
                val result = ArrayList<String>()
                fileUri
                    .let { context.contentResolver.openInputStream(it) }
                    ?.let { BufferedReader(InputStreamReader(it)) }
                    ?.use { reader ->
                        var line = reader.readLine()
                        while (line != null) {
                            result.add(line)
                            line = reader.readLine()
                        }
                    }
                val extracted = extractTransactions(result)
                val templates = TemplateParser.extractTemplates(result)
                _fileContents.postValue(result)
                _transactions.postValue(extracted)
                _templates.postValue(templates)
                onFinish()
            } catch (e: IOException) {
                onReadError(e)
            }
        }

        suspend fun addTemplate(
            fileUri: Uri,
            template: TransactionTemplate,
            onFinish: suspend () -> Unit,
            onMismatch: suspend () -> Unit,
            onWriteError: suspend (IOException) -> Unit,
            onReadError: suspend (IOException) -> Unit,
        ) = writeTransformed(
            fileUri,
            emit = {
                emitAddTemplate(
                    it,
                    transactions.value?.firstOrNull()?.firstLine,
                    template.toCommentLines().joinToString("\n"),
                )
            },
            onFinish,
            onMismatch,
            onWriteError,
            onReadError,
        )

        suspend fun updateTemplate(
            fileUri: Uri,
            template: TransactionTemplate,
            onFinish: suspend () -> Unit,
            onMismatch: suspend () -> Unit,
            onWriteError: suspend (IOException) -> Unit,
            onReadError: suspend (IOException) -> Unit,
        ) {
            val existing = _templates.value?.find { it.id == template.id }
            if (existing == null) {
                onFinish()
                return
            }
            writeTransformed(
                fileUri,
                emit = {
                    emitUpdateTemplate(
                        it,
                        existing.firstLine,
                        existing.lastLine,
                        template.toCommentLines().joinToString("\n"),
                    )
                },
                onFinish,
                onMismatch,
                onWriteError,
                onReadError,
            )
        }

        suspend fun deleteTemplate(
            fileUri: Uri,
            templateId: String,
            onFinish: suspend () -> Unit,
            onMismatch: suspend () -> Unit,
            onWriteError: suspend (IOException) -> Unit,
            onReadError: suspend (IOException) -> Unit,
        ) {
            val existing = _templates.value?.find { it.id == templateId }
            if (existing == null) {
                onFinish()
                return
            }
            writeTransformed(
                fileUri,
                emit = { emitDeleteTemplate(it, existing.firstLine, existing.lastLine) },
                onFinish,
                onMismatch,
                onWriteError,
                onReadError,
            )
        }
    }
