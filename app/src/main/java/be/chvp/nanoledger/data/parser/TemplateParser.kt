package be.chvp.nanoledger.data.parser

import be.chvp.nanoledger.data.TransactionTemplate

object TemplateParser {
    fun parseTemplates(fileContent: String): List<TransactionTemplate> {
        val lines = fileContent.lines()
        val templates = mutableListOf<TransactionTemplate>()
        var currentTemplateLines = mutableListOf<String>()
        var inTemplate = false

        for (line in lines) {
            val trimmed = line.trim()

            // Stop if we hit a non-comment line (templates only at the beginning)
            if (!trimmed.startsWith(";")) {
                break
            }

            if (trimmed.startsWith("; template-start:")) {
                inTemplate = true
                currentTemplateLines = mutableListOf(trimmed)
            } else if (inTemplate) {
                currentTemplateLines.add(trimmed)

                if (trimmed == "; template-end") {
                    val template = TransactionTemplate.fromCommentLines(currentTemplateLines)
                    if (template != null) {
                        templates.add(template)
                    }
                    inTemplate = false
                    currentTemplateLines = mutableListOf()
                }
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
