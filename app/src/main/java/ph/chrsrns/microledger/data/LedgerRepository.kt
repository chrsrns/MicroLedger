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

        private fun readLines(fileUri: Uri): List<String>? {
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
                ?: return null
            return result
        }

        fun matches(fileUri: Uri): Boolean =
            (readLines(fileUri) ?: throw IOException("openInputStream returned null")) ==
                fileContents.value

        suspend fun deleteTransaction(
            fileUri: Uri,
            transaction: Transaction,
        ): LedgerWriteResult =
            writeTransformed(fileUri) {
                emitDeleteTransaction(it, transaction.firstLine, transaction.lastLine)
            }

        suspend fun replaceTransaction(
            fileUri: Uri,
            transaction: Transaction,
            text: String,
        ): LedgerWriteResult =
            writeTransformed(fileUri) {
                emitReplaceTransaction(it, transaction.firstLine, transaction.lastLine, text)
            }

        suspend fun appendTo(
            fileUri: Uri,
            text: String,
        ): LedgerWriteResult = writeTransformed(fileUri) { emitAppend(it, text) }

        private suspend fun writeTransformed(
            fileUri: Uri,
            emit: (List<String>) -> String,
        ): LedgerWriteResult {
            try {
                if (!matches(fileUri)) {
                    return LedgerWriteResult.Mismatch
                }
                context.contentResolver
                    .openOutputStream(fileUri, "wt")
                    ?.let { OutputStreamWriter(it) }
                    ?.use { it.write(emit(fileContents.value!!)) }
                    ?: return LedgerWriteResult.WriteError(
                        IOException("openOutputStream returned null"),
                    )
                return readFrom(fileUri)
                    ?.let { LedgerWriteResult.RefreshError(it) }
                    ?: LedgerWriteResult.Success
            } catch (e: IOException) {
                return LedgerWriteResult.WriteError(e)
            } catch (e: SecurityException) {
                return LedgerWriteResult.WriteError(IOException(e))
            }
        }

        suspend fun readFrom(fileUri: Uri): IOException? {
            try {
                val result =
                    readLines(fileUri) ?: return IOException("openInputStream returned null")
                val extracted = extractTransactions(result)
                val templates = TemplateParser.extractTemplates(result)
                _fileContents.postValue(result)
                _transactions.postValue(extracted)
                _templates.postValue(templates)
                return null
            } catch (e: IOException) {
                return e
            } catch (e: SecurityException) {
                return IOException(e)
            }
        }

        suspend fun addTemplate(
            fileUri: Uri,
            template: TransactionTemplate,
        ): LedgerWriteResult =
            writeTransformed(fileUri) {
                emitAddTemplate(
                    it,
                    template.toCommentLines().joinToString("\n"),
                )
            }

        suspend fun updateTemplate(
            fileUri: Uri,
            template: TransactionTemplate,
        ): LedgerWriteResult {
            val existing =
                _templates.value?.find { it.id == template.id }
                    ?: return LedgerWriteResult.Success
            return writeTransformed(fileUri) {
                emitUpdateTemplate(
                    it,
                    existing.firstLine,
                    existing.lastLine,
                    template.toCommentLines().joinToString("\n"),
                )
            }
        }

        suspend fun deleteTemplate(
            fileUri: Uri,
            templateId: String,
        ): LedgerWriteResult {
            val existing =
                _templates.value?.find { it.id == templateId }
                    ?: return LedgerWriteResult.Success
            return writeTransformed(fileUri) {
                emitDeleteTemplate(it, existing.firstLine, existing.lastLine)
            }
        }
    }
