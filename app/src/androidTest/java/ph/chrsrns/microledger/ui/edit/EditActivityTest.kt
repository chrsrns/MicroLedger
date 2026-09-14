package ph.chrsrns.microledger.ui.edit

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ph.chrsrns.microledger.R
import ph.chrsrns.microledger.ui.main.MainActivity
import ph.chrsrns.microledger.utils.FileManagerRule

@RunWith(AndroidJUnit4::class)
class EditActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val fileManagerRule = FileManagerRule(composeRule)

    val context: Context by lazy { ApplicationProvider.getApplicationContext() }

    @Before
    fun setupFile() {
        fileManagerRule.configureAppWithFile("test.journal")
    }

    @Test
    fun canDoSimpleEdit() {
        composeRule.onNodeWithText("2023-09-04").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription(context.getString(R.string.edit)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Reconciliation").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Reconciliation").assertIsDisplayed().performTextReplacement("Changed description")
        composeRule.onNodeWithContentDescription(context.getString(R.string.save)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("2023-09-04").assertIsDisplayed()
        composeRule.onNodeWithText("Friend | Changed description").assertIsDisplayed()
    }

    @Test
    fun codeInAccountSurvivesEdit() {
        composeRule.onNodeWithText("(123) Restaurant | Dinner with friend").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription(context.getString(R.string.edit)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Dinner with friend").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Dinner with friend").assertIsDisplayed().performTextReplacement("Changed description")
        composeRule.onNodeWithContentDescription(context.getString(R.string.save)).assertIsDisplayed().performClick()
        composeRule.onAllNodesWithText("2023-09-02").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("(123) Restaurant | Changed description").assertIsDisplayed()
    }

    @Test
    fun canDoEditWithOnlyNote() {
        composeRule.onNodeWithText("2026-02-26").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription(context.getString(R.string.edit)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Found").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Found").assertIsDisplayed().performTextReplacement("Stolen")
        composeRule.onNodeWithContentDescription(context.getString(R.string.save)).assertIsDisplayed().performClick()
        composeRule.onNodeWithText("2026-02-26").assertIsDisplayed()
        composeRule.onNodeWithText("Stolen").assertIsDisplayed()
    }

    @Test
    fun canEditPostingFromBottomSheet() {
        composeRule.onAllNodesWithText("Dinner with friend").onFirst().assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription(context.getString(R.string.edit)).assertIsDisplayed().performClick()

        composeRule.onNodeWithText("expenses:food:restaurant").assertIsDisplayed().performClick()
        composeRule.onNodeWithText(context.getString(R.string.edit_posting)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.account)).assertIsDisplayed().performTextReplacement("expenses:food:restaurant:friend")
        composeRule.onNodeWithTag("posting_sheet_save").assertIsDisplayed().performClick()

        composeRule.onNodeWithContentDescription(context.getString(R.string.save)).assertIsDisplayed().performClick()
        composeRule.onAllNodesWithText("Dinner with friend").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("expenses:food:restaurant:friend").assertIsDisplayed()
    }
}
