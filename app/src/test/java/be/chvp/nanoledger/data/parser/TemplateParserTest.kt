package be.chvp.nanoledger.data.parser

import be.chvp.nanoledger.data.TransactionTemplate
import be.chvp.nanoledger.data.parser.TemplateParser.extractTemplates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TemplateParserTest {
    @Test
    fun canParseSimpleSingleTemplate() {
        val fileContent =
            """
                |; template-start: Grocery Run
                |; id: 550e8400-e29b-41d4-a716-446655440000
                |; payee: Whole Foods
                |; note: Weekly groceries
                |; status: !
                |; account: Expenses:Food:Groceries  USD 100.56
                |; template-end
            """.trimMargin()
        val result = extractTemplates(fileContent.split('\n'))

        assertEquals(1, result.size)
        val template: TransactionTemplate = result[0]

        assertEquals("Grocery Run", template.name)
        assertEquals("550e8400-e29b-41d4-a716-446655440000", template.id)
        assertEquals("Whole Foods", template.payee)
        assertEquals("Weekly groceries", template.note)
        assertEquals("!", template.status)

        assertEquals(1, template.postings.size)
        assertEquals("Expenses:Food:Groceries", template.postings[0].account)
        assertNotNull(template.postings[0].amount)
        assertEquals("USD", template.postings[0].amount?.currency)
        assertEquals("100.56", template.postings[0].amount?.quantity)

        assertEquals(
            fileContent,
            template.toCommentLines().joinToString("\n"),
        )
    }

    @Test
    fun canParseTemplateWithOnlyCurrency() {
        val fileContent =
            """
                |; template-start: Simple Template
                |; id: 880e8400-e29b-41d4-a716-446655440003
                |; account: Expenses:Misc  EUR
                |; template-end
            """.trimMargin()
        val result = extractTemplates(fileContent.split('\n'))

        assertEquals(1, result.size)
        val template: TransactionTemplate = result[0]

        assertEquals("Simple Template", template.name)
        assertEquals("880e8400-e29b-41d4-a716-446655440003", template.id)
        assertEquals(null, template.payee)
        assertEquals(null, template.note)
        assertEquals(null, template.status)
        assertEquals(null, template.code)

        assertEquals(1, template.postings.size)
        assertEquals("Expenses:Misc", template.postings[0].account)
        assertNotNull(template.postings[0].amount)
        assertEquals("EUR", template.postings[0].amount?.currency)

        assertEquals(
            fileContent,
            template.toCommentLines().joinToString("\n"),
        )
    }

    @Test
    fun canParseMultipleTemplates() {
        val result =
            extractTemplates(
                """
                |; template-start: Grocery Run
                |; id: 550e8400-e29b-41d4-a716-446655440000
                |; payee: Whole Foods
                |; note: Weekly groceries
                |; status: !
                |; account: Expenses:Food:Groceries    USD
                |; template-end
                |;
                |; template-start: Coffee Shop
                |; id: 660e8400-e29b-41d4-a716-446655440001
                |; payee: Starbucks
                |; note: Morning coffee
                |; status: *
                |; account: Expenses:Food:Coffee        EUR
                |; template-end
                """.trimMargin().split('\n'),
            )

        assertEquals(2, result.size)

        val template1: TransactionTemplate = result[0]
        assertEquals("Grocery Run", template1.name)
        assertEquals("550e8400-e29b-41d4-a716-446655440000", template1.id)
        assertEquals("Whole Foods", template1.payee)
        assertEquals("Weekly groceries", template1.note)
        assertEquals("!", template1.status)
        assertEquals(1, template1.postings.size)
        assertEquals("Expenses:Food:Groceries", template1.postings[0].account)
        assertEquals("USD", template1.postings[0].amount?.currency)

        val template2: TransactionTemplate = result[1]
        assertEquals("Coffee Shop", template2.name)
        assertEquals("660e8400-e29b-41d4-a716-446655440001", template2.id)
        assertEquals("Starbucks", template2.payee)
        assertEquals("Morning coffee", template2.note)
        assertEquals("*", template2.status)
        assertEquals(1, template2.postings.size)
        assertEquals("Expenses:Food:Coffee", template2.postings[0].account)
        assertEquals("EUR", template2.postings[0].amount?.currency)
    }

    @Test
    fun canParseTemplateWithMultiplePostings() {
        val result =
            extractTemplates(
                """
                |; template-start: Dinner Out
                |; id: 770e8400-e29b-41d4-a716-446655440002
                |; payee: Restaurant
                |; note: Team dinner
                |; status: *
                |; account: Expenses:Food:Dining   EUR
                |; account: Assets:Bank:Checking            EUR
                |; account: Liabilities:CreditCard   USD
                |; template-end
                """.trimMargin().split('\n'),
            )

        assertEquals(1, result.size)
        val template: TransactionTemplate = result[0]

        assertEquals("Dinner Out", template.name)
        assertEquals("770e8400-e29b-41d4-a716-446655440002", template.id)
        assertEquals("Restaurant", template.payee)
        assertEquals("Team dinner", template.note)
        assertEquals("*", template.status)

        assertEquals(3, template.postings.size)
        assertEquals("Expenses:Food:Dining", template.postings[0].account)
        assertEquals("EUR", template.postings[0].amount?.currency)
        assertEquals("Assets:Bank:Checking", template.postings[1].account)
        assertEquals("EUR", template.postings[1].amount?.currency)
        assertEquals("Liabilities:CreditCard", template.postings[2].account)
        assertEquals("USD", template.postings[2].amount?.currency)
    }

    @Test
    fun canParseTemplateWithFullPosting() {
        val fileContent =
            """
                |; template-start: Investment Purchase
                |; id: 990e8400-e29b-41d4-a716-446655440004
                |; payee: Brokerage
                |; note: Stock purchase with cost and assertion
                |; account: Assets:Investments:Stocks   10 AAPL @@ $150.00 = 10 AAPL @ $150.00  ; with cost and assertion
                |; template-end
            """.trimMargin()
        val result = extractTemplates(fileContent.split('\n'))

        assertEquals(1, result.size)
        val template: TransactionTemplate = result[0]

        assertEquals("Investment Purchase", template.name)
        assertEquals("990e8400-e29b-41d4-a716-446655440004", template.id)
        assertEquals("Brokerage", template.payee)
        assertEquals("Stock purchase with cost and assertion", template.note)

        assertEquals(1, template.postings.size)
        val posting = template.postings[0]
        assertEquals("Assets:Investments:Stocks", posting.account)
        assertNotNull(posting.amount)
        assertEquals("10", posting.amount.quantity)
        assertEquals("AAPL", posting.amount.currency)
        assertNotNull(posting.cost)
        assertEquals("@@", posting.cost.type.repr)
        assertEquals("150.00", posting.cost.amount.quantity)
        assertEquals("$", posting.cost.amount.currency)
        assertNotNull(posting.assertion)
        assertEquals("10", posting.assertion.quantity)
        assertEquals("AAPL", posting.assertion.currency)
        assertNotNull(posting.assertionCost)
        assertEquals("@", posting.assertionCost.type.repr)
        assertEquals("150.00", posting.assertionCost.amount.quantity)
        assertEquals("$", posting.assertionCost.amount.currency)
        assertEquals("with cost and assertion", posting.comment)
    }

    @Test
    fun canParseTemplateWithFullPostingWithoutAmount() {
        val fileContent =
            """
                |; template-start: Investment Purchase
                |; id: 990e8400-e29b-41d4-a716-446655440004
                |; payee: Brokerage
                |; note: Stock purchase with cost and assertion
                |; account: Assets:Investments:Stocks  AAPL @@ $ 150.00 = AAPL 10 @ $ 150.00  ; with cost and assertion
                |; template-end
            """.trimMargin()
        val result = extractTemplates(fileContent.split('\n'))

        assertEquals(1, result.size)
        val template: TransactionTemplate = result[0]

        assertEquals("Investment Purchase", template.name)
        assertEquals("990e8400-e29b-41d4-a716-446655440004", template.id)
        assertEquals("Brokerage", template.payee)
        assertEquals("Stock purchase with cost and assertion", template.note)

        assertEquals(1, template.postings.size)
        val posting = template.postings[0]
        assertEquals("Assets:Investments:Stocks", posting.account)
        assertNotNull(posting.amount)
        assertEquals("", posting.amount.quantity)
        assertEquals("AAPL", posting.amount.currency)
        assertNotNull(posting.cost)
        assertEquals("@@", posting.cost.type.repr)
        assertEquals("150.00", posting.cost.amount.quantity)
        assertEquals("$", posting.cost.amount.currency)
        assertNotNull(posting.assertion)
        assertEquals("10", posting.assertion.quantity)
        assertEquals("AAPL", posting.assertion.currency)
        assertNotNull(posting.assertionCost)
        assertEquals("@", posting.assertionCost.type.repr)
        assertEquals("150.00", posting.assertionCost.amount.quantity)
        assertEquals("$", posting.assertionCost.amount.currency)
        assertEquals("with cost and assertion", posting.comment)

        assertEquals(
            fileContent,
            template.toCommentLines().joinToString("\n"),
        )
    }
}
