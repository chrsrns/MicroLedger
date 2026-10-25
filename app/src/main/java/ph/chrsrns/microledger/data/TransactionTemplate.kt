package ph.chrsrns.microledger.data

import ph.chrsrns.microledger.data.parser.extractPosting

data class TransactionTemplate(
    val firstLine: Int,
    val lastLine: Int,
    val id: String,
    val name: String,
    val payee: String?,
    val note: String?,
    val status: String?,
    val code: String?,
    val postings: List<Posting>,
) {
    fun toCommentLines(): List<String> {
        val lines = mutableListOf<String>()
        lines.add("; template-start: $name")
        lines.add("; id: $id")
        payee?.let { lines.add("; payee: $it") }
        note?.let { lines.add("; note: $it") }
        status?.let { lines.add("; status: $it") }
        code?.let { lines.add("; code: $it") }
        postings.filter { !it.account.isNullOrEmpty() }.forEach { posting ->
            posting.let {
                lines.add(
                    "; account: ${
                        it.formatLenient(
                            0,
                            currencyBeforeAmount = true,
                            currencyAmountSpacing = true,
                            currencyEnabled = true,
                        ).trim()
                    }",
                )
            }
        }
        lines.add("; template-end")
        return lines
    }
}

fun extractPostingLenient(line: String): Posting = extractPosting(line, lenient = true)
