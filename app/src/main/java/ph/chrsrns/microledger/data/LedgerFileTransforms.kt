package ph.chrsrns.microledger.data

// Pure file-content transforms for LedgerRepository writes (V334/V335).
// Each takes the cached file lines and produces the complete new file
// text; no ContentResolver or stream access inside.

internal fun emitDeleteTransaction(
    contents: List<String>,
    firstLine: Int,
    lastLine: Int,
): String {
    val sb = StringBuilder()
    contents.forEachIndexed { i, line ->
        if (i in firstLine..lastLine) return@forEachIndexed
        // If the line after the transaction is empty, consider it a
        // divider for the next transaction and skip it as well
        if (i == lastLine + 1 && line == "") return@forEachIndexed
        sb.append(line).append("\n")
    }
    return sb.toString()
}

internal fun emitReplaceTransaction(
    contents: List<String>,
    firstLine: Int,
    lastLine: Int,
    text: String,
): String {
    val sb = StringBuilder()
    contents.forEachIndexed { i, line ->
        // If we encounter the first line of the transaction, write out the replacement
        if (i == firstLine) {
            sb.append(text)
            return@forEachIndexed
        }

        // Just skip all the next lines
        if (i in firstLine + 1..lastLine) return@forEachIndexed

        // If the line after the transaction is empty, consider it a
        // divider for the next transaction and skip it as well
        if (i == lastLine + 1 && line == "") return@forEachIndexed
        sb.append(line).append("\n")
    }
    return sb.toString()
}

internal fun emitAppend(
    contents: List<String>,
    text: String,
): String {
    val sb = StringBuilder()
    contents.forEach { sb.append(it).append("\n") }
    if (contents.isNotEmpty() && contents.last() != "") {
        sb.append("\n")
    }
    sb.append(text)
    return sb.toString()
}

internal fun emitAddTemplate(
    contents: List<String>,
    firstTransactionLine: Int?,
    text: String,
): String {
    var newTemplateIndex = 0 // Index to place template
    var addSeparator = true
    var prepend = true

    // If a transaction exists, insert new template to the comment before that transaction
    if (firstTransactionLine != null) {
        for (i in firstTransactionLine downTo 1) {
            val fileContentLine = contents[i]

            if (fileContentLine.isBlank()) addSeparator = false

            if (fileContentLine.startsWith("; ")) {
                newTemplateIndex = i + 1
                break
            }
        }
    } else {
        // If no transactions yet, search for the last comment line
        for (i in contents.size - 1 downTo 0) {
            val fileContentLine = contents[i]

            if (fileContentLine.isBlank()) addSeparator = false

            if (fileContentLine.startsWith("; ")) {
                newTemplateIndex = i
                prepend = false
                break
            }
        }
    }

    val sb = StringBuilder()
    contents.forEachIndexed { i, line ->
        // If we encounter the index chosen for the template, write the
        // template next to the original line
        if (i == newTemplateIndex) {
            if (prepend) {
                sb.append(text).append("\n")
                // Enforce a blank line separator
                if (addSeparator) sb.append("\n")

                sb.append(line).append("\n")
            } else {
                sb.append(line).append("\n")
                sb.append(text).append("\n")

                // Enforce a blank line separator
                if (addSeparator) sb.append("\n")
            }
            return@forEachIndexed
        }

        sb.append(line).append("\n")
    }
    // An index past the end of the file (e.g. an empty journal) still
    // emits the template text (V358)
    if (newTemplateIndex >= contents.size) {
        sb.append(text).append("\n")
        if (addSeparator) sb.append("\n")
    }
    return sb.toString()
}

internal fun emitUpdateTemplate(
    contents: List<String>,
    firstLine: Int,
    lastLine: Int,
    text: String,
): String {
    val sb = StringBuilder()
    contents.forEachIndexed { i, line ->
        // If we encounter the first line of the template, write out the replacement
        if (i == firstLine) {
            sb.append(text).append("\n")
            return@forEachIndexed
        }

        // Skip the remaining lines of the old template
        if (i in firstLine + 1..lastLine) return@forEachIndexed

        sb.append(line).append("\n")
    }
    return sb.toString()
}

internal fun emitDeleteTemplate(
    contents: List<String>,
    firstLine: Int,
    lastLine: Int,
): String {
    val sb = StringBuilder()
    contents.forEachIndexed { i, line ->
        if (i in firstLine..lastLine) return@forEachIndexed
        sb.append(line).append("\n")
    }
    return sb.toString()
}
