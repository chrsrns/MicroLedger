package be.chvp.nanoledger.data.parser

import be.chvp.nanoledger.data.Posting
import be.chvp.nanoledger.data.TransactionTemplate
import be.chvp.nanoledger.data.extractPostingLenient

object TemplateParser {
    fun extractTemplates(lines: List<String>): List<TransactionTemplate> {
        val templates = mutableListOf<TransactionTemplate>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            i += 1

            // Stop if we hit a non-comment line (templates only at the beginning)
            if (!line.startsWith(";")) {
                break
            }

            if (line.startsWith("; template-start:")) {
                val firstLine = i - 1
                var lastLine = firstLine
                val name = line.substringAfter("; template-start:").trim()
                var id: String? = null
                var payee: String? = null
                var note: String? = null
                var status: String? = null
                var code: String? = null
                val postings = mutableListOf<Posting>()

                while (i < lines.size) {
                    val currTemplateLine = lines[i].trim()

                    if (currTemplateLine.startsWith("; template-end")) {
                        lastLine = i
                        i += 1
                        break
                    }
                    val content = currTemplateLine.substringAfter("; ")
                    when {
                        content.startsWith("id: ") -> id = content.substringAfter("id: ")
                        content.startsWith("payee: ") -> payee = content.substringAfter("payee: ")
                        content.startsWith("note: ") -> note = content.substringAfter("note: ")
                        content.startsWith("status: ") ->
                            status =
                                content.substringAfter("status: ")

                        content.startsWith("code: ") -> code = content.substringAfter("code: ")
                        content.startsWith("account: ") ->
                            postings.add(extractPostingLenient(content.substringAfter("account: ")))
                    }

                    i += 1
                }
                if (id == null || firstLine == lastLine) continue

                templates.add(
                    TransactionTemplate(
                        firstLine,
                        lastLine,
                        id = id,
                        name = name,
                        payee = payee,
                        note = note,
                        status = status,
                        code = code,
                        postings = postings,
                    ),
                )
            }
        }

        return templates
    }

    fun extractNonTemplateContent(fileContent: String): String {
        val lines = fileContent.lines()
        val result = mutableListOf<String>()
        var skipUntilEnd = false

        for (line in lines) {
            val trimmed = line.trim()

            if (skipUntilEnd) {
                if (trimmed == "; template-end") {
                    skipUntilEnd = false
                }
                continue
            }

            if (trimmed.startsWith("; template-start:")) {
                skipUntilEnd = true
                continue
            }

            result.add(line)
        }

        return result.joinToString("\n").trimStart()
    }

    fun buildTemplateSection(templates: List<TransactionTemplate>): String {
        if (templates.isEmpty()) return ""

        return templates
            .flatMap { it.toCommentLines() }
            .joinToString("\n") + "\n"
    }
}
