package ph.chrsrns.microledger.ui.main

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ph.chrsrns.microledger.data.Amount
import ph.chrsrns.microledger.data.Posting
import ph.chrsrns.microledger.data.Transaction
import ph.chrsrns.microledger.ui.theme.MicroLedgerTheme

@RunWith(AndroidJUnit4::class)
class TransactionCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displaysSplitHeaderAndClearedChip() {
        composeRule.setContent {
            MicroLedgerTheme(dynamicColor = false) {
                TransactionCard(
                    transaction =
                        Transaction(
                            firstLine = 0,
                            lastLine = 0,
                            date = "2023-09-04",
                            status = "*",
                            code = null,
                            payee = "Friend",
                            note = "Reconciliation",
                            postings = emptyList(),
                        ),
                    selected = false,
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithText("2023-09-04").assertIsDisplayed()
        composeRule.onNodeWithText("Friend | Reconciliation", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("StatusChip").assertIsDisplayed()
        composeRule.onNodeWithText("CLEARED").assertIsDisplayed()
    }

    @Test
    fun displaysPendingChip() {
        composeRule.setContent {
            MicroLedgerTheme(dynamicColor = false) {
                TransactionCard(
                    transaction =
                        Transaction(
                            firstLine = 0,
                            lastLine = 0,
                            date = "2023-09-01",
                            status = "!",
                            code = null,
                            payee = "Employer",
                            note = "Payment",
                            postings = emptyList(),
                        ),
                    selected = false,
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("StatusChip").assertIsDisplayed()
        composeRule.onNodeWithText("PENDING").assertIsDisplayed()
    }

    @Test
    fun hidesStatusChipForBlankAndUnknownStatuses() {
        for (status in listOf(null, " ", "#")) {
            composeRule.setContent {
                MicroLedgerTheme(dynamicColor = false) {
                    TransactionCard(
                        transaction =
                            Transaction(
                                firstLine = 0,
                                lastLine = 0,
                                date = "2023-09-02",
                                status = status,
                                code = null,
                                payee = "Landlord",
                                note = "Rent",
                                postings = emptyList(),
                            ),
                        selected = false,
                        onClick = {},
                    )
                }
            }

            composeRule.onNodeWithTag("StatusChip").assertDoesNotExist()
        }
    }

    @Test
    fun displaysAccountTypeDotAndPositiveAmountColor() {
        var expectedPrimary = Color.Unspecified
        composeRule.setContent {
            MicroLedgerTheme(dynamicColor = false) {
                expectedPrimary = MaterialTheme.colorScheme.primary
                TransactionCard(
                    transaction =
                        Transaction(
                            firstLine = 0,
                            lastLine = 0,
                            date = "2023-09-01",
                            status = "*",
                            code = null,
                            payee = "Employer",
                            note = "Payment",
                            postings =
                                listOf(
                                    Posting(
                                        account = "assets:checking account",
                                        amount = Amount("1000.00", "€", "€ 1000.00"),
                                        cost = null,
                                        assertion = null,
                                        assertionCost = null,
                                        comment = null,
                                    ),
                                ),
                        ),
                    selected = false,
                    onClick = {},
                    assetsPrefixes = listOf("assets"),
                    incomePrefixes = listOf("income"),
                )
            }
        }

        composeRule.onNodeWithTag("AccountDot").assertIsDisplayed()
        composeRule.onNodeWithText("assets:checking account").assertIsDisplayed()
        composeRule.onNodeWithTag("Amount").assertTextColor(expectedPrimary)
    }

    @Test
    fun displaysNegativeAmountColor() {
        var expectedError = Color.Unspecified
        composeRule.setContent {
            MicroLedgerTheme(dynamicColor = false) {
                expectedError = MaterialTheme.colorScheme.error
                TransactionCard(
                    transaction =
                        Transaction(
                            firstLine = 0,
                            lastLine = 0,
                            date = "2023-09-01",
                            status = "*",
                            code = null,
                            payee = "Employer",
                            note = "Payment",
                            postings =
                                listOf(
                                    Posting(
                                        account = "income:work",
                                        amount = Amount("-1000.00", "€", "€ -1000.00"),
                                        cost = null,
                                        assertion = null,
                                        assertionCost = null,
                                        comment = null,
                                    ),
                                ),
                        ),
                    selected = false,
                    onClick = {},
                    assetsPrefixes = listOf("assets"),
                    incomePrefixes = listOf("income"),
                )
            }
        }

        composeRule.onNodeWithTag("Amount").assertTextColor(expectedError)
    }

    @Test
    fun hidesDotForUnmatchedAccount() {
        composeRule.setContent {
            MicroLedgerTheme(dynamicColor = false) {
                TransactionCard(
                    transaction =
                        Transaction(
                            firstLine = 0,
                            lastLine = 0,
                            date = "2023-09-02",
                            status = "*",
                            code = null,
                            payee = "Landlord",
                            note = "Rent",
                            postings =
                                listOf(
                                    Posting(
                                        account = "random:account",
                                        amount = Amount("500.00", "€", "€ 500.00"),
                                        cost = null,
                                        assertion = null,
                                        assertionCost = null,
                                        comment = null,
                                    ),
                                ),
                        ),
                    selected = false,
                    onClick = {},
                    assetsPrefixes = listOf("assets"),
                )
            }
        }

        composeRule.onNodeWithTag("AccountDot").assertDoesNotExist()
    }

    @Test
    fun selectedCardIsTaggedAsSelected() {
        composeRule.setContent {
            MicroLedgerTheme(dynamicColor = false) {
                TransactionCard(
                    transaction =
                        Transaction(
                            firstLine = 0,
                            lastLine = 0,
                            date = "2023-09-04",
                            status = "*",
                            code = null,
                            payee = "Friend",
                            note = "Reconciliation",
                            postings = emptyList(),
                        ),
                    selected = true,
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("Selected").assertExists()
        composeRule.onNodeWithTag("Unselected").assertDoesNotExist()
    }

    @Test
    fun unselectedCardIsTaggedAsUnselected() {
        composeRule.setContent {
            MicroLedgerTheme(dynamicColor = false) {
                TransactionCard(
                    transaction =
                        Transaction(
                            firstLine = 0,
                            lastLine = 0,
                            date = "2023-09-04",
                            status = "*",
                            code = null,
                            payee = "Friend",
                            note = "Reconciliation",
                            postings = emptyList(),
                        ),
                    selected = false,
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("Unselected").assertExists()
        composeRule.onNodeWithTag("Selected").assertDoesNotExist()
    }
}

private fun SemanticsNodeInteraction.assertTextColor(expected: Color): SemanticsNodeInteraction =
    this.assert(
        SemanticsMatcher("text color is $expected") { node ->
            val results = mutableListOf<TextLayoutResult>()
            val action = node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action
            if (action != null) {
                action(results)
            }
            results
                .firstOrNull()
                ?.layoutInput
                ?.style
                ?.color == expected
        },
    )
