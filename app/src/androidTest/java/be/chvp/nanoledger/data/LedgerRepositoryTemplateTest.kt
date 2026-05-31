package be.chvp.nanoledger.data

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File


@RunWith(AndroidJUnit4::class)
class LedgerRepositoryTemplateTest {

    /**
     * TemporaryFolder rule creates a temporary directory for test files.
     * Each test gets a fresh folder, and it's automatically cleaned up after.
     */
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Rule
    @JvmField
    val instantExecutorRule = InstantTaskExecutorRule()

    val startingTemplatePortion = """
        |; template-start: Dinner Out
        |; id: 770e8400-e29b-41d4-a716-446655440002
        |; payee: Restaurant
        |; note: Team dinner
        |; status: *
        |; account: Expenses:Food:Dining   EUR
        |; account: Assets:Bank:Checking            EUR
        |; account: Liabilities:CreditCard   USD
        |; template-end""".trim()
    val startingTransactionsPortion = """
        |2024-01-15 * Grocery Store | Weekly shopping
        |    Expenses:Food:Groceries    EUR 45.50
        |    Assets:Bank:Checking
        |
        |; Random comment here
        |
        |2024-01-20 * Coffee Shop | Morning coffee
        |    Expenses:Food:Dining    EUR 4.50
        |    Assets:Cash""".trim()
    val startingLedgerContent = """
        $startingTemplatePortion
        |
        $startingTransactionsPortion
        """.trimMargin()

    private lateinit var context: Context
    private lateinit var repository: LedgerRepository

    @Before
    fun setUp() {
        // Get the Application context for creating the repository
        context = ApplicationProvider.getApplicationContext()
        // Manually instantiate the repository (it's a simple singleton with @Inject constructor)
        repository = LedgerRepository(context as Application)
    }

    @After
    fun tearDown() {
        // Clean up any resources if needed
    }

    private suspend fun setupRepository(content: String): Uri {
        val journalFile = File(tempFolder.root, "test.journal")
        val uri = Uri.fromFile(journalFile)
        journalFile.writeText(content)

        var readFinished = false
        repository.readFrom(uri, onFinish = { readFinished = true }, onReadError = { readFinished = false })
        assert(readFinished)

        // Wait until repository.templates is initialized OR about 15 seconds have elapsed
        withTimeoutOrNull(15000L) {
            while (!repository.templates.isInitialized || !repository.transactions.isInitialized) {
                delay(100)
            }
        }
        return uri
    }

    private fun createTestTemplate(id: String = "770e8400-e29b-41d4-a716-446655440002") = TransactionTemplate(
        firstLine = 0,
        lastLine = 0,
        id = id,
        name = "Daily Groceries",
        payee = "Wet Market",
        note = "Groceries",
        status = "",
        code = "",
        postings = listOf(
            Posting(
                account = "assets:house money",
                amount = Amount(
                    quantity = "", currency = "PHP",
                    original = "PHP"
                ),
                cost = null,
                assertion = null,
                assertionCost = null,
                comment = null
            ),
            Posting(
                account = "expenses:house:food",
                amount = Amount(
                    quantity = "", currency = "PHP",
                    original = "PHP"
                ),
                cost = null,
                assertion = null,
                assertionCost = null,
                comment = null
            )
        )
    )

    private suspend fun addTemplate(uri: Uri, template: TransactionTemplate) {
        var finished = false
        repository.addTemplate(
            uri, template,
            onFinish = { finished = true },
            onMismatch = { error("Mismatch") },
            onWriteError = { throw it },
            onReadError = { throw it }
        )
        assert(finished)
    }

    @Test
    fun testReadTemplatesPopulation() = runTest {
        setupRepository(startingLedgerContent)

        assert(repository.templates.value?.size == 1)
        assertEquals(2, repository.transactions.value?.size)
        val template = repository.templates.value?.get(0)

        assertNotNull(template)
        assertEquals("770e8400-e29b-41d4-a716-446655440002", template?.id)
        assertEquals(3, template?.postings?.size)
    }

    @Test
    fun testAddTemplateSimple() = runTest {
        val uri = setupRepository(startingLedgerContent)
        val newTemplate = createTestTemplate()

        addTemplate(uri, newTemplate)

        val expectedFileOutput = """
            $startingTemplatePortion
            |${newTemplate.toCommentLines().joinToString("\n")}
            |
            $startingTransactionsPortion
        """.trimMargin()
        assertEquals(
            expectedFileOutput,
            repository.fileContents.value!!.joinToString("\n")
        )
    }

    @Test
    fun testAddTemplateNoPreexistingComment() = runTest {
        val uri = setupRepository(startingTransactionsPortion.trimMargin())
        val newTemplate = createTestTemplate()

        addTemplate(uri, newTemplate)

        val expectedFileOutput = """
            |${newTemplate.toCommentLines().joinToString("\n")}
            |
            $startingTransactionsPortion
        """.trimMargin()
        assertEquals(
            expectedFileOutput,
            repository.fileContents.value!!.joinToString("\n")
        )
    }

    @Test
    fun testAddTemplateCommentOnly() = runTest {
        val uri = setupRepository("; Test Comment")
        val newTemplate = createTestTemplate()

        addTemplate(uri, newTemplate)

        val expectedFileOutput = """
            |; Test Comment
            |${newTemplate.toCommentLines().joinToString("\n")}
            |
        """.trimMargin()
        assertEquals(
            expectedFileOutput,
            repository.fileContents.value!!.joinToString("\n")
        )
    }

    private suspend fun updateTemplate(uri: Uri, template: TransactionTemplate) {
        var finished = false
        repository.updateTemplate(
            uri, template,
            onFinish = { finished = true },
            onMismatch = { error("Mismatch") },
            onWriteError = { throw it },
            onReadError = { throw it }
        )
        assert(finished)
    }

    @Test
    fun testUpdateTemplateName() = runTest {
        val uri = setupRepository(startingLedgerContent)
        val originalTemplate = repository.templates.value!![0]
        val updatedTemplate = originalTemplate.copy(name = "Updated Dinner")

        updateTemplate(uri, updatedTemplate)

        // Verify LiveData is updated
        assertEquals("Updated Dinner", repository.templates.value!![0].name)

        // Verify file contents are persisted
        val content = repository.fileContents.value!!.joinToString("\n")
        assert(content.contains("; template-start: Updated Dinner"))
        assert(!content.contains("; template-start: Dinner Out"))
    }

    @Test
    fun testUpdateTemplatePostings() = runTest {
        val uri = setupRepository(startingLedgerContent)
        val originalTemplate = repository.templates.value!![0]
        val updatedTemplate = originalTemplate.copy(
            postings = listOf(
                Posting(
                    account = "Expenses:New",
                    amount = Amount(quantity = "50", currency = "USD", original = "USD 50"),
                    cost = null,
                    assertion = null,
                    assertionCost = null,
                    comment = null
                )
            )
        )

        updateTemplate(uri, updatedTemplate)

        // Verify LiveData reflects new postings
        assertEquals(1, repository.templates.value!![0].postings.size)
        assertEquals("Expenses:New", repository.templates.value!![0].postings[0].account)

        // Verify file contents are persisted
        val content = repository.fileContents.value!!.joinToString("\n")
        assert(content.contains("Expenses:New"))
        assert(!content.contains("Liabilities:CreditCard"))
    }

    @Test
    fun testUpdateTemplateNonExistentId() = runTest {
        val uri = setupRepository(startingLedgerContent)
        val originalFileContents = repository.fileContents.value!!.joinToString("\n")
        val nonExistentTemplate = createTestTemplate("non-existent-id")

        updateTemplate(uri, nonExistentTemplate)

        // File should be unchanged
        assertEquals(originalFileContents, repository.fileContents.value!!.joinToString("\n"))
        // Original template should still be there
        assertEquals(1, repository.templates.value?.size)
        assertEquals("770e8400-e29b-41d4-a716-446655440002", repository.templates.value!![0].id)
    }

    @Test
    fun testUpdateTemplatePreservesOtherTemplates() = runTest {
        val uri = setupRepository(startingLedgerContent)

        // Add a second template
        val secondTemplate = createTestTemplate("second-id")
        addTemplate(uri, secondTemplate)
        assertEquals(2, repository.templates.value?.size)

        // Update only the first template
        val originalFirst = repository.templates.value!!.find {
            it.id == "770e8400-e29b-41d4-a716-446655440002"
        }!!
        val updatedFirst = originalFirst.copy(name = "Updated First")

        updateTemplate(uri, updatedFirst)

        // Verify the first template is updated
        val first = repository.templates.value!!.find {
            it.id == "770e8400-e29b-41d4-a716-446655440002"
        }!!
        assertEquals("Updated First", first.name)

        // Verify the second template is preserved
        val second = repository.templates.value!!.find { it.id == "second-id" }!!
        assertEquals("Daily Groceries", second.name)
        assertEquals(2, second.postings.size)
    }

    @Test
    fun testUpdateTemplatePreservesTransactions() = runTest {
        val uri = setupRepository(startingLedgerContent)
        val originalTemplate = repository.templates.value!![0]
        val updatedTemplate = originalTemplate.copy(name = "Updated Name")

        updateTemplate(uri, updatedTemplate)

        // Verify transactions are preserved
        assertEquals(2, repository.transactions.value?.size)
        val content = repository.fileContents.value!!.joinToString("\n")
        assert(content.contains("Grocery Store"))
        assert(content.contains("Coffee Shop"))
        assert(content.contains("; Random comment here"))
    }

    @Test
    fun testUpdateTemplateMismatch() = runTest {
        val journalFile = File(tempFolder.root, "test.journal")
        val uri = setupRepository(startingLedgerContent)

        // Modify file externally
        journalFile.writeText("Corrupted content")

        var mismatchTriggered = false
        val originalTemplate = repository.templates.value!![0]
        repository.updateTemplate(
            uri, originalTemplate.copy(name = "Should Fail"),
            onFinish = { error("Should have mismatched") },
            onMismatch = { mismatchTriggered = true },
            onWriteError = { throw it },
            onReadError = { throw it }
        )
        assert(mismatchTriggered)
    }

    private suspend fun deleteTemplate(uri: Uri, templateId: String) {
        var finished = false
        repository.deleteTemplate(
            uri, templateId,
            onFinish = { finished = true },
            onMismatch = { error("Mismatch") },
            onWriteError = { throw it },
            onReadError = { throw it }
        )
        assert(finished)
    }

    @Test
    fun testDeleteTemplate() = runTest {
        val uri = setupRepository(startingLedgerContent)

        deleteTemplate(uri, "770e8400-e29b-41d4-a716-446655440002")

        // Verify LiveData is updated
        assertEquals(0, repository.templates.value?.size)

        // Verify file contents no longer contain template lines
        val content = repository.fileContents.value!!.joinToString("\n")
        assert(!content.contains("; template-start: Dinner Out"))
        assert(!content.contains("; template-end"))
        assert(!content.contains("; id: 770e8400-e29b-41d4-a716-446655440002"))
    }

    @Test
    fun testDeleteTemplatePreservesOtherTemplates() = runTest {
        val uri = setupRepository(startingLedgerContent)

        // Add a second template
        val secondTemplate = createTestTemplate("second-id")
        addTemplate(uri, secondTemplate)
        assertEquals(2, repository.templates.value?.size)

        // Delete only the first template
        deleteTemplate(uri, "770e8400-e29b-41d4-a716-446655440002")

        // Verify only the second template remains
        assertEquals(1, repository.templates.value?.size)
        val remaining = repository.templates.value!![0]
        assertEquals("second-id", remaining.id)
        assertEquals("Daily Groceries", remaining.name)
        assertEquals(2, remaining.postings.size)

        // Verify file still contains the second template
        val content = repository.fileContents.value!!.joinToString("\n")
        assert(content.contains("; template-start: Daily Groceries"))
        assert(content.contains("; id: second-id"))
    }

    @Test
    fun testDeleteTemplatePreservesTransactions() = runTest {
        val uri = setupRepository(startingLedgerContent)

        deleteTemplate(uri, "770e8400-e29b-41d4-a716-446655440002")

        // Verify transactions are preserved
        assertEquals(2, repository.transactions.value?.size)
        val content = repository.fileContents.value!!.joinToString("\n")
        assert(content.contains("Grocery Store"))
        assert(content.contains("Coffee Shop"))
        assert(content.contains("; Random comment here"))
    }

    @Test
    fun testDeleteTemplateNonExistentId() = runTest {
        val uri = setupRepository(startingLedgerContent)
        val originalFileContents = repository.fileContents.value!!.joinToString("\n")

        deleteTemplate(uri, "non-existent-id")

        // File should be unchanged
        assertEquals(originalFileContents, repository.fileContents.value!!.joinToString("\n"))
        // Original template should still be there
        assertEquals(1, repository.templates.value?.size)
        assertEquals("770e8400-e29b-41d4-a716-446655440002", repository.templates.value!![0].id)
    }

    @Test
    fun testDeleteTemplateMismatch() = runTest {
        val journalFile = File(tempFolder.root, "test.journal")
        val uri = setupRepository(startingLedgerContent)

        // Modify file externally
        journalFile.writeText("Corrupted content")

        var mismatchTriggered = false
        repository.deleteTemplate(
            uri, "770e8400-e29b-41d4-a716-446655440002",
            onFinish = { error("Should have mismatched") },
            onMismatch = { mismatchTriggered = true },
            onWriteError = { throw it },
            onReadError = { throw it }
        )
        assert(mismatchTriggered)
    }

    @Test
    fun testMismatchScenario() = runTest {
        val journalFile = File(tempFolder.root, "test.journal")
        val uri = setupRepository(startingLedgerContent)

        // Modify file externally
        journalFile.writeText("Corrupted content")

        var mismatchTriggered = false
        repository.addTemplate(
            uri, createTestTemplate("new-id"),
            onFinish = { error("Should have mismatched") },
            onMismatch = { mismatchTriggered = true },
            onWriteError = { throw it },
            onReadError = { throw it }
        )
        assert(mismatchTriggered)
    }

    @Test
    fun testTemplateWorkflow() = runTest {
        val uri = setupRepository(startingLedgerContent)

        // 1. Add new template
        val newTemplate = createTestTemplate("new-id-1")
        addTemplate(uri, newTemplate)
        assertEquals(2, repository.templates.value?.size)

        // 2. Update the new template
        val updatedTemplate = newTemplate.copy(name = "Updated Workflow Name")
        var finished = false
        repository.updateTemplate(
            uri, updatedTemplate,
            onFinish = { finished = true },
            onMismatch = { error("Mismatch") },
            onWriteError = { throw it },
            onReadError = { throw it }
        )
        assert(finished)
        assertEquals("Updated Workflow Name", repository.templates.value!!.find { it.id == "new-id-1" }?.name)

        // 3. Delete the original template
        finished = false
        repository.deleteTemplate(
            uri, "770e8400-e29b-41d4-a716-446655440002",
            onFinish = { finished = true },
            onMismatch = { error("Mismatch") },
            onWriteError = { throw it },
            onReadError = { throw it }
        )
        assert(finished)
        assertEquals(1, repository.templates.value?.size)
        assertEquals("new-id-1", repository.templates.value!![0].id)
    }

    @Test
    fun testTemplateWithComplexPostings() = runTest {
        val uri = setupRepository(startingTransactionsPortion.trimMargin())
        val complexTemplate = TransactionTemplate(
            firstLine = 0, lastLine = 0,
            id = "complex-id",
            name = "Complex Template",
            payee = "Complex Payee",
            note = null, status = null, code = null,
            postings = listOf(
                Posting(
                    account = "Expenses:Travel",
                    amount = Amount("100", "USD", "100 USD"),
                    cost = Cost(Amount("0.92", "EUR", "0.92 EUR"), CostType.UNIT),
                    assertion = Amount("500", "USD", "500 USD"),
                    assertionCost = null,
                    comment = "Business trip"
                )
            )
        )

        addTemplate(uri, complexTemplate)

        val template = repository.templates.value!!.find { it.id == "complex-id" }!!
        assertEquals(1, template.postings.size)
        val posting = template.postings[0]
        assertEquals("Expenses:Travel", posting.account)
        assertEquals("100", posting.amount?.quantity)
        assertEquals(CostType.UNIT, posting.cost?.type)
        assertEquals("0.92", posting.cost?.amount?.quantity)
    }

    @Test
    fun testAddTemplateIgnoresInvalidPostingWithEmptyAccount() = runTest {
        val uri = setupRepository(startingTransactionsPortion.trimMargin())
        val templateWithInvalidPosting = TransactionTemplate(
            firstLine = 0,
            lastLine = 0,
            id = "invalid-posting-id",
            name = "Template with Empty Account",
            payee = "Test Payee",
            note = "Test note",
            status = "",
            code = "",
            postings = listOf(
                Posting(
                    account = "Assets:Valid",
                    amount = Amount(
                        quantity = "100",
                        currency = "EUR",
                        original = "EUR 100"
                    ),
                    cost = null,
                    assertion = null,
                    assertionCost = null,
                    comment = null
                ),
                Posting(
                    account = "",
                    amount = Amount(
                        quantity = "",
                        currency = "EUR",
                        original = ""
                    ),
                    cost = null,
                    assertion = null,
                    assertionCost = null,
                    comment = null
                ),
                Posting(
                    account = "Liabilities:AlsoValid",
                    amount = Amount(
                        quantity = "",
                        currency = "EUR",
                        original = ""
                    ),
                    cost = null,
                    assertion = null,
                    assertionCost = null,
                    comment = null
                )
            )
        )

        addTemplate(uri, templateWithInvalidPosting)

        val template = repository.templates.value!!.find { it.id == "invalid-posting-id" }!!
        assertEquals(2, template.postings.size)
        assertEquals("Assets:Valid", template.postings[0].account)
        assertEquals("Liabilities:AlsoValid", template.postings[1].account)

        val content = repository.fileContents.value!!.joinToString("\n")
        assert(content.contains("account: Assets:Valid"))
        assert(content.contains("account: Liabilities:AlsoValid"))
        assert(!content.contains("account: EUR"))
    }

    @Test
    fun testUpdateTemplateIgnoresInvalidPostingWithEmptyAccount() = runTest {
        val uri = setupRepository(startingLedgerContent)
        val originalTemplate = repository.templates.value!![0]
        val updatedTemplate = originalTemplate.copy(
            postings = listOf(
                Posting(
                    account = "Expenses:Valid",
                    amount = Amount(quantity = "50", currency = "USD", original = "USD 50"),
                    cost = null,
                    assertion = null,
                    assertionCost = null,
                    comment = null
                ),
                Posting(
                    account = "",
                    amount = Amount(quantity = "", currency = "USD", original = ""),
                    cost = null,
                    assertion = null,
                    assertionCost = null,
                    comment = null
                )
            )
        )

        updateTemplate(uri, updatedTemplate)

        val template = repository.templates.value!![0]
        assertEquals(1, template.postings.size)
        assertEquals("Expenses:Valid", template.postings[0].account)

        val content = repository.fileContents.value!!.joinToString("\n")
        assert(content.contains("account: Expenses:Valid"))
        assert(!content.contains("account: USD"))
    }
}
