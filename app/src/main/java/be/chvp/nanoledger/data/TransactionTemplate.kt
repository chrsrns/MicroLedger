package be.chvp.nanoledger.data

import be.chvp.nanoledger.data.parser.COMMENT_REGEX
import be.chvp.nanoledger.data.parser.COST_SPLIT_REGEX
import be.chvp.nanoledger.data.parser.POSTING_SPLIT_REGEX
import be.chvp.nanoledger.data.parser.QUANTITY_AT_END_REGEX
import be.chvp.nanoledger.data.parser.QUANTITY_AT_START_REGEX
import java.util.UUID

data class TransactionTemplate(
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
        postings.forEach { posting ->
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

    companion object {
        fun fromCommentLines(lines: List<String>): TransactionTemplate? {
            if (lines.isEmpty()) return null

            val firstLine = lines.first()
            if (!firstLine.startsWith("; template-start:")) return null

            val name = firstLine.substringAfter("; template-start:").trim()
            var id: String? = null
            var payee: String? = null
            var note: String? = null
            var status: String? = null
            var code: String? = null
            val postings = mutableListOf<Posting>()

            for (line in lines.drop(1)) {
                if (line == "; template-end") break
                if (!line.startsWith("; ")) continue

                val content = line.substringAfter("; ")
                when {
                    content.startsWith("id: ") -> id = content.substringAfter("id: ")
                    content.startsWith("payee: ") -> payee = content.substringAfter("payee: ")
                    content.startsWith("note: ") -> note = content.substringAfter("note: ")
                    content.startsWith("status: ") -> status = content.substringAfter("status: ")
                    content.startsWith("code: ") -> code = content.substringAfter("code: ")
                    content.startsWith("account: ") ->
                        postings.add(extractPostingLenient(content.substringAfter("account: ")))
                }
            }

            if (id == null) return null

            return TransactionTemplate(
                id = id,
                name = name,
                payee = payee,
                note = note,
                status = status,
                code = code,
                postings = postings,
            )
        }
    }
}

fun extractPostingLenient(line: String): Posting {
    var account: String? = null
    var amount: Amount? = null
    var cost: Cost? = null
    var assertion: Amount? = null
    var assertionCost: Cost? = null
    var comment: String? = null

    val commentMatch = COMMENT_REGEX.find(line)
    if (commentMatch != null) {
        comment =
            commentMatch.value
                .trim()
                .trimStart(';')
                .trim()
    }

    val stripped = line.replace(COMMENT_REGEX, "").trim()
    if (stripped.isNotEmpty()) {
        val components = stripped.split(POSTING_SPLIT_REGEX, limit = 2)
        account = components[0]

        if (components.size > 1) {
            val fullAmountString = components[1].trim()
            if (fullAmountString.contains('=')) {
                val amountComponents = fullAmountString.split('=', limit = 2)
                val baseRes = extractAmountAndCost(amountComponents[0].trim())
                amount = baseRes.first
                cost = baseRes.second
                val assertionRes = extractAmountAndCost(amountComponents[1].trim())
                assertion = assertionRes.first
                assertionCost = assertionRes.second
            } else {
                val res = extractAmountAndCost(fullAmountString)
                amount = res.first
                cost = res.second
            }
        }
    }

    return Posting(account, amount, cost, assertion, assertionCost, comment)
}

fun extractAmountAndCost(string: String): Pair<Amount?, Cost?> {
    if (string.isEmpty()) {
        return Pair(null, null)
    }
    if (string.contains(COST_SPLIT_REGEX)) {
        val costType =
            if (string.contains("@@")) {
                CostType.TOTAL
            } else {
                CostType.UNIT
            }

        val (amountString, costString) = string.split(COST_SPLIT_REGEX, limit = 2)
        return Pair(
            extractAmountLenient(amountString.trim()),
            Cost(extractAmountLenient(costString.trim()), costType),
        )
    } else {
        return Pair(extractAmountLenient(string), null)
    }
}

fun extractAmountLenient(string: String): Amount {
    val stripped = string.trim()

    if (stripped.isEmpty()) {
        return Amount("", "", string)
    }

    val matchForStart = QUANTITY_AT_START_REGEX.find(stripped)
    if (matchForStart != null) {
        val groups = matchForStart.groups
        val quantity = groups[1]!!.value.trim()
        val currency = groups[2]!!.value.trim()
        return Amount(quantity, currency, string)
    }
    val matchForEnd = QUANTITY_AT_END_REGEX.find(stripped)
    if (matchForEnd != null) {
        val quantity = matchForEnd.value.trim()
        val currency = stripped.replace(QUANTITY_AT_END_REGEX, "").trim()
        return Amount(quantity, currency, string)
    }

    // If no quantity found, check if the entire string is a valid commodity (currency-only)
    if (stripped.isNotEmpty() && !stripped.contains(Regex("[0-9]"))) {
        return Amount("", stripped, string)
    }

    return Amount("", "", string)
}
