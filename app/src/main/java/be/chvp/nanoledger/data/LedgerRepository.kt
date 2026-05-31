package be.chvp.nanoledger.data

import android.app.Application
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import be.chvp.nanoledger.data.parser.TemplateParser
import be.chvp.nanoledger.data.parser.extractTransactions
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
                    txs.map { tx -> tx.payee }.filter { payee -> payee != null }.map { payee -> payee!! },
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
        ) {
            try {
                if (!matches(fileUri)) {
                    onMismatch()
                } else {
                    context.contentResolver
                        .openOutputStream(fileUri, "wt")
                        ?.let { OutputStreamWriter(it) }
                        ?.use {
                            fileContents.value!!.forEachIndexed { i, line ->
                                if (i >= transaction.firstLine && i <= transaction.lastLine) {
                                    return@forEachIndexed
                                }
                                // If the line after the transaction is empty, consider it a
                                // divider for the next transaction and skip it as well
                                if (i == transaction.lastLine + 1 && line == "") {
                                    return@forEachIndexed
                                }
                                it.write("${line}\n")
                            }
                        }
                    readFrom(fileUri, onFinish, onReadError)
                }
            } catch (e: IOException) {
                onWriteError(e)
            }
        }

        suspend fun replaceTransaction(
            fileUri: Uri,
            transaction: Transaction,
            text: String,
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
                        ?.use {
                            fileContents.value!!.forEachIndexed { i, line ->
                                // If we encounter the first line of the transaction, write out the replacement
                                if (i == transaction.firstLine) {
                                    it.write(text)
                                    return@forEachIndexed
                                }

                                // Just skip all the next lines
                                if (i > transaction.firstLine && i <= transaction.lastLine) {
                                    return@forEachIndexed
                                }

                                // If the line after the transaction is empty, consider it a
                                // divider for the next transaction and skip it as well
                                if (i == transaction.lastLine + 1 && line == "") {
                                    return@forEachIndexed
                                }
                                it.write("${line}\n")
                            }
                        }
                    readFrom(fileUri, onFinish, onReadError)
                }
            } catch (e: IOException) {
                onWriteError(e)
            }
        }

        suspend fun appendTo(
            fileUri: Uri,
            text: String,
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
                        ?.use {
                            fileContents.value!!.forEach { line ->
                                it.write("${line}\n")
                            }
                            if (!fileContents.value!!.isEmpty() && fileContents.value!!.last() != "") {
                                it.write("\n")
                            }
                            it.write(text)
                        }
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
        ) {
            try {
                if (!matches(fileUri)) {
                    onMismatch()
                } else {
                    var newTemplateIndex = 0 // Index to place template
                    var addSeparator = true
                    var prepend = true

                    // If a transaction exists, insert new template to the comment before that transaction
                    if (transactions.value!!.isNotEmpty()) {
                        for (i in transactions.value!![0].firstLine downTo 1) {
                            val fileContentLine = fileContents.value!![i]

                            if (fileContentLine.isBlank()) addSeparator = false

                            if (fileContentLine.startsWith("; ")) {
                                newTemplateIndex = i + 1
                                break
                            }
                        }
                    }

                    // If no transactions yet, search for the last comment line
                    else {
                        for (i in fileContents.value!!.size - 1 downTo 0) {
                            val fileContentLine = fileContents.value!![i]

                            if (fileContentLine.isBlank()) addSeparator = false

                            if (fileContentLine.startsWith("; ")) {
                                newTemplateIndex = i
                                prepend = false
                                break
                            }
                        }
                    }

                    val text = template.toCommentLines().joinToString("\n")

                    context.contentResolver
                        .openOutputStream(fileUri, "wt")
                        ?.let { OutputStreamWriter(it) }
                        ?.use {
                            fileContents.value!!.forEachIndexed { i, line ->
                                // If we encounter the first line of the transaction,
                                // append the template after the original line
                                if (i == newTemplateIndex) {
                                    if (prepend) {
                                        it.write("${text}\n")
                                        // Enforce a blank line separator
                                        if (addSeparator) it.write("\n")

                                        it.write("${line}\n")
                                    } else {
                                        it.write("${line}\n")
                                        it.write("${text}\n")

                                        // Enforce a blank line separator
                                        if (addSeparator) it.write("\n")
                                    }
                                    return@forEachIndexed
                                }

                                it.write("${line}\n")
                            }
                        }
                    readFrom(fileUri, onFinish, onReadError)
                }
            } catch (e: IOException) {
                onWriteError(e)
            }
        }

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
            try {
                if (!matches(fileUri)) {
                    onMismatch()
                } else {
                    val text = template.toCommentLines().joinToString("\n")
                    context.contentResolver
                        .openOutputStream(fileUri, "wt")
                        ?.let { OutputStreamWriter(it) }
                        ?.use {
                            fileContents.value!!.forEachIndexed { i, line ->
                                // If we encounter the first line of the template, write out the replacement
                                if (i == existing.firstLine) {
                                    it.write("${text}\n")
                                    return@forEachIndexed
                                }

                                // Skip the remaining lines of the old template
                                if (i > existing.firstLine && i <= existing.lastLine) {
                                    return@forEachIndexed
                                }

                                it.write("${line}\n")
                            }
                        }
                    readFrom(fileUri, onFinish, onReadError)
                }
            } catch (e: IOException) {
                onWriteError(e)
            }
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
            try {
                if (!matches(fileUri)) {
                    onMismatch()
                } else {
                    context.contentResolver
                        .openOutputStream(fileUri, "wt")
                        ?.let { OutputStreamWriter(it) }
                        ?.use {
                            fileContents.value!!.forEachIndexed { i, line ->
                                if (i >= existing.firstLine && i <= existing.lastLine) {
                                    return@forEachIndexed
                                }
                                it.write("${line}\n")
                            }
                        }
                    readFrom(fileUri, onFinish, onReadError)
                }
            } catch (e: IOException) {
                onWriteError(e)
            }
        }
    }
