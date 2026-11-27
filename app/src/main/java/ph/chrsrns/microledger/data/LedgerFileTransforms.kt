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
    text: String,
): String {
    val sb = StringBuilder()
    // Templates go at the top of the file. No blank separator: a blank
    // line would end the initial `;` block scan and orphan existing templates.
    sb.append(text).append("\n")
    contents.forEach { line ->
        sb.append(line).append("\n")
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
