package ph.chrsrns.microledger.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import ph.chrsrns.microledger.R
import ph.chrsrns.microledger.ui.main.MainActivity

class FileManagerRule(
    private val composeRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>,
    private val context: Context = ApplicationProvider.getApplicationContext(),
) : TestRule {
    private val createdUris = mutableListOf<Uri>()

    fun configureAppWithFile(filename: String) {
        val testContext = InstrumentationRegistry.getInstrumentation().context

        val fileUri =
            insertIntoDownloads(
                context = context,
                displayName = filename,
                mimeType = "application/octet-stream",
                data = testContext.assets.open(filename).use { it.readBytes() },
            )

        context
            .getSharedPreferences("ph.chrsrns.microledger.preferences", Context.MODE_PRIVATE)
            .edit()
            .putString("file_uri", fileUri.toString())
            .apply()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule
                .onAllNodesWithText(context.getString(R.string.no_transactions_yet))
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    private fun insertIntoDownloads(
        context: Context,
        displayName: String,
        mimeType: String,
        data: ByteArray,
    ): Uri {
        val resolver = context.contentResolver

        val externalUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI

        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            }

        val uri = resolver.insert(externalUri, values) ?: error("Failed to insert into MediaStore: $displayName")

        resolver.openOutputStream(uri)?.use { os ->
            os.write(data)
            os.flush()
        } ?: error("Failed to open output stream for $uri")

        createdUris += uri
        return uri
    }

    private fun cleanup() {
        val resolver = context.contentResolver
        createdUris.forEach { runCatching { resolver.delete(it, null, null) } }
        createdUris.clear()
    }

    override fun apply(
        base: Statement,
        description: Description,
    ): Statement =
        object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } finally {
                    cleanup()
                }
            }
        }
}
