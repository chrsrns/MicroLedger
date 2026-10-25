package ph.chrsrns.microledger.data

// Snapshot of all user-tunable settings for the settings screen
// (I228). fileUri is deliberately not part of this — it is handled
// through PreferencesDataSource.fileUri / onOpenFile (V347).
data class SettingsState(
    val decimalSeparator: String = ".",
    val defaultCurrency: String = "€",
    val defaultStatus: String = " ",
    val currencyBeforeAmount: Boolean = true,
    val postingWidth: Int = 72,
    val currencyAmountSpacing: Boolean = true,
    val transactionStatusPresentByDefault: Boolean = true,
    val transactionCodePresentByDefault: Boolean = false,
    val transactionPayeePresentByDefault: Boolean = true,
    val transactionNotePresentByDefault: Boolean = true,
    val transactionCurrenciesPresentByDefault: Boolean = true,
    val postingAmountPresentByDefault: Boolean = true,
    val postingCostPresentByDefault: Boolean = false,
    val postingAssertionPresentByDefault: Boolean = false,
    val postingAssertionCostPresentByDefault: Boolean = false,
    val postingCommentPresentByDefault: Boolean = false,
    val prefixes: AccountTypePrefixes = AccountTypePrefixes.EMPTY,
)
