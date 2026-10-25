package ph.chrsrns.microledger.ui.preferences

// Single carrier for all settable-preference callbacks (I229):
// 21 fields, one per settable preference. fileUri is excluded — it is
// handled through onOpenFile on PreferencesScreen (V347).
data class SettingsCallbacks(
    val onDecimalSeparatorChange: (String) -> Unit = {},
    val onDefaultCurrencyChange: (String) -> Unit = {},
    val onDefaultStatusChange: (String) -> Unit = {},
    val onCurrencyBeforeAmountChange: (Boolean) -> Unit = {},
    val onPostingWidthChange: (Int) -> Unit = {},
    val onCurrencyAmountSpacingChange: (Boolean) -> Unit = {},
    val onTransactionStatusPresentByDefaultChange: (Boolean) -> Unit = {},
    val onTransactionCodePresentByDefaultChange: (Boolean) -> Unit = {},
    val onTransactionPayeePresentByDefaultChange: (Boolean) -> Unit = {},
    val onTransactionNotePresentByDefaultChange: (Boolean) -> Unit = {},
    val onTransactionCurrenciesPresentByDefaultChange: (Boolean) -> Unit = {},
    val onPostingAmountPresentByDefaultChange: (Boolean) -> Unit = {},
    val onPostingCostPresentByDefaultChange: (Boolean) -> Unit = {},
    val onPostingAssertionPresentByDefaultChange: (Boolean) -> Unit = {},
    val onPostingAssertionCostPresentByDefaultChange: (Boolean) -> Unit = {},
    val onPostingCommentPresentByDefaultChange: (Boolean) -> Unit = {},
    val onAssetsPrefixesChange: (List<String>) -> Unit = {},
    val onLiabilitiesPrefixesChange: (List<String>) -> Unit = {},
    val onEquityPrefixesChange: (List<String>) -> Unit = {},
    val onIncomePrefixesChange: (List<String>) -> Unit = {},
    val onExpensesPrefixesChange: (List<String>) -> Unit = {},
)
