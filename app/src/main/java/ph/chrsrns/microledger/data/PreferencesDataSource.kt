package ph.chrsrns.microledger.data

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.map
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PreferencesDataSource
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        private val sharedPreferences =
            context.getSharedPreferences(
                "ph.chrsrns.microledger.preferences",
                Context.MODE_PRIVATE,
            )

        private val fileUriPref =
            Pref<Uri?>(
                key = "file_uri",
                default = null,
                read = { k, _ -> getString(k, null)?.toUri() },
                write = { k, v -> edit { putString(k, v?.toString()) } },
                live = { k, _ -> stringLiveData(k, null).map { it?.toUri() } },
            )

        val fileUri: LiveData<Uri?> = fileUriPref.liveData(sharedPreferences)

        fun getFileUri(): Uri? = fileUriPref.read(sharedPreferences)

        fun setFileUri(fileUri: Uri?) = fileUriPref.write(sharedPreferences, fileUri)

        private val defaultCurrencyPref = stringPref("default_currency", "€")

        val defaultCurrency: LiveData<String> = defaultCurrencyPref.liveData(sharedPreferences)

        fun getDefaultCurrency(): String = defaultCurrencyPref.read(sharedPreferences)

        fun setDefaultCurrency(currency: String) = defaultCurrencyPref.write(sharedPreferences, currency)

        private val defaultStatusPref = stringPref("default_status", " ")

        val defaultStatus: LiveData<String> = defaultStatusPref.liveData(sharedPreferences)

        fun getDefaultStatus(): String = defaultStatusPref.read(sharedPreferences)

        fun setDefaultStatus(status: String) = defaultStatusPref.write(sharedPreferences, status)

        private val decimalSeparatorPref = stringPref("decimal_separator", ".")

        val decimalSeparator: LiveData<String> = decimalSeparatorPref.liveData(sharedPreferences)

        fun getDecimalSeparator(): String = decimalSeparatorPref.read(sharedPreferences)

        fun setDecimalSeparator(separator: String) = decimalSeparatorPref.write(sharedPreferences, separator)

        private val currencyBeforeAmountPref = booleanPref("currency_before_amount", true)

        val currencyBeforeAmount: LiveData<Boolean> = currencyBeforeAmountPref.liveData(sharedPreferences)

        fun getCurrencyBeforeAmount(): Boolean = currencyBeforeAmountPref.read(sharedPreferences)

        fun setCurrencyBeforeAmount(currencyBeforeAmount: Boolean) = currencyBeforeAmountPref.write(sharedPreferences, currencyBeforeAmount)

        private val postingWidthPref = intPref("posting_width", 72)

        val postingWidth: LiveData<Int> = postingWidthPref.liveData(sharedPreferences)

        fun getPostingWidth(): Int = postingWidthPref.read(sharedPreferences)

        fun setPostingWidth(width: Int) = postingWidthPref.write(sharedPreferences, width)

        private val currencyAmountSpacingPref = booleanPref("spacing_between_currency_and_amount", true)

        val spacingBetweenCurrencyAndAmount: LiveData<Boolean> =
            currencyAmountSpacingPref.liveData(sharedPreferences)

        fun getCurrencyAmountSpacing(): Boolean = currencyAmountSpacingPref.read(sharedPreferences)

        fun setCurrencyAmountSpacing(spacing: Boolean) = currencyAmountSpacingPref.write(sharedPreferences, spacing)

        private val transactionStatusPresentByDefaultPref =
            booleanPref("transaction_status_present_by_default", true)

        val transactionStatusPresentByDefault: LiveData<Boolean> =
            transactionStatusPresentByDefaultPref.liveData(sharedPreferences)

        fun getTransactionStatusPresentByDefault(): Boolean = transactionStatusPresentByDefaultPref.read(sharedPreferences)

        fun setTransactionStatusPresentByDefault(status: Boolean) = transactionStatusPresentByDefaultPref.write(sharedPreferences, status)

        private val transactionCodePresentByDefaultPref =
            booleanPref("transaction_code_present_by_default", false)

        val transactionCodePresentByDefault: LiveData<Boolean> =
            transactionCodePresentByDefaultPref.liveData(sharedPreferences)

        fun getTransactionCodePresentByDefault(): Boolean = transactionCodePresentByDefaultPref.read(sharedPreferences)

        fun setTransactionCodePresentByDefault(code: Boolean) = transactionCodePresentByDefaultPref.write(sharedPreferences, code)

        private val transactionPayeePresentByDefaultPref =
            booleanPref("transaction_payee_present_by_default", true)

        val transactionPayeePresentByDefault: LiveData<Boolean> =
            transactionPayeePresentByDefaultPref.liveData(sharedPreferences)

        fun getTransactionPayeePresentByDefault(): Boolean = transactionPayeePresentByDefaultPref.read(sharedPreferences)

        fun setTransactionPayeePresentByDefault(payee: Boolean) = transactionPayeePresentByDefaultPref.write(sharedPreferences, payee)

        private val transactionNotePresentByDefaultPref =
            booleanPref("transaction_note_present_by_default", true)

        val transactionNotePresentByDefault: LiveData<Boolean> =
            transactionNotePresentByDefaultPref.liveData(sharedPreferences)

        fun getTransactionNotePresentByDefault(): Boolean = transactionNotePresentByDefaultPref.read(sharedPreferences)

        fun setTransactionNotePresentByDefault(note: Boolean) = transactionNotePresentByDefaultPref.write(sharedPreferences, note)

        private val transactionCurrenciesPresentByDefaultPref =
            booleanPref("transaction_currencies_present_by_default", true)

        val transactionCurrenciesPresentByDefault: LiveData<Boolean> =
            transactionCurrenciesPresentByDefaultPref.liveData(sharedPreferences)

        fun getTransactionCurrenciesPresentByDefault(): Boolean = transactionCurrenciesPresentByDefaultPref.read(sharedPreferences)

        fun setTransactionCurrenciesPresentByDefault(currencies: Boolean) =
            transactionCurrenciesPresentByDefaultPref.write(sharedPreferences, currencies)

        private val postingAmountPresentByDefaultPref =
            booleanPref("posting_amount_present_by_default", true)

        val postingAmountPresentByDefault: LiveData<Boolean> =
            postingAmountPresentByDefaultPref.liveData(sharedPreferences)

        fun getPostingAmountPresentByDefault(): Boolean = postingAmountPresentByDefaultPref.read(sharedPreferences)

        fun setPostingAmountPresentByDefault(amount: Boolean) = postingAmountPresentByDefaultPref.write(sharedPreferences, amount)

        private val postingCostPresentByDefaultPref =
            booleanPref("posting_cost_present_by_default", false)

        val postingCostPresentByDefault: LiveData<Boolean> =
            postingCostPresentByDefaultPref.liveData(sharedPreferences)

        fun getPostingCostPresentByDefault(): Boolean = postingCostPresentByDefaultPref.read(sharedPreferences)

        fun setPostingCostPresentByDefault(cost: Boolean) = postingCostPresentByDefaultPref.write(sharedPreferences, cost)

        private val postingAssertionPresentByDefaultPref =
            booleanPref("posting_assertion_present_by_default", false)

        val postingAssertionPresentByDefault: LiveData<Boolean> =
            postingAssertionPresentByDefaultPref.liveData(sharedPreferences)

        fun getPostingAssertionPresentByDefault(): Boolean = postingAssertionPresentByDefaultPref.read(sharedPreferences)

        fun setPostingAssertionPresentByDefault(assertion: Boolean) =
            postingAssertionPresentByDefaultPref.write(sharedPreferences, assertion)

        private val postingAssertionCostPresentByDefaultPref =
            booleanPref("posting_assertion_cost_present_by_default", false)

        val postingAssertionCostPresentByDefault: LiveData<Boolean> =
            postingAssertionCostPresentByDefaultPref.liveData(sharedPreferences)

        fun getPostingAssertionCostPresentByDefault(): Boolean = postingAssertionCostPresentByDefaultPref.read(sharedPreferences)

        fun setPostingAssertionCostPresentByDefault(assertionCost: Boolean) =
            postingAssertionCostPresentByDefaultPref.write(sharedPreferences, assertionCost)

        private val postingCommentPresentByDefaultPref =
            booleanPref("posting_comment_present_by_default", false)

        val postingCommentPresentByDefault: LiveData<Boolean> =
            postingCommentPresentByDefaultPref.liveData(sharedPreferences)

        fun getPostingCommentPresentByDefault(): Boolean = postingCommentPresentByDefaultPref.read(sharedPreferences)

        fun setPostingCommentPresentByDefault(comment: Boolean) = postingCommentPresentByDefaultPref.write(sharedPreferences, comment)

        private val assetsPrefixesPref = stringListPref("assets_prefixes", listOf("Assets"))
        private val liabilitiesPrefixesPref = stringListPref("liabilities_prefixes", listOf("Liabilities"))
        private val equityPrefixesPref = stringListPref("equity_prefixes", listOf("Equity"))
        private val incomePrefixesPref = stringListPref("income_prefixes", listOf("Income"))
        private val expensesPrefixesPref = stringListPref("expenses_prefixes", listOf("Expenses"))

        val assetsPrefixes: LiveData<List<String>> = assetsPrefixesPref.liveData(sharedPreferences)
        val liabilitiesPrefixes: LiveData<List<String>> = liabilitiesPrefixesPref.liveData(sharedPreferences)
        val equityPrefixes: LiveData<List<String>> = equityPrefixesPref.liveData(sharedPreferences)
        val incomePrefixes: LiveData<List<String>> = incomePrefixesPref.liveData(sharedPreferences)
        val expensesPrefixes: LiveData<List<String>> = expensesPrefixesPref.liveData(sharedPreferences)

        fun getAssetsPrefixes(): List<String> = assetsPrefixesPref.read(sharedPreferences)

        fun setAssetsPrefixes(prefixes: List<String>) = assetsPrefixesPref.write(sharedPreferences, prefixes)

        fun getLiabilitiesPrefixes(): List<String> = liabilitiesPrefixesPref.read(sharedPreferences)

        fun setLiabilitiesPrefixes(prefixes: List<String>) = liabilitiesPrefixesPref.write(sharedPreferences, prefixes)

        fun getEquityPrefixes(): List<String> = equityPrefixesPref.read(sharedPreferences)

        fun setEquityPrefixes(prefixes: List<String>) = equityPrefixesPref.write(sharedPreferences, prefixes)

        fun getIncomePrefixes(): List<String> = incomePrefixesPref.read(sharedPreferences)

        fun setIncomePrefixes(prefixes: List<String>) = incomePrefixesPref.write(sharedPreferences, prefixes)

        fun getExpensesPrefixes(): List<String> = expensesPrefixesPref.read(sharedPreferences)

        fun setExpensesPrefixes(prefixes: List<String>) = expensesPrefixesPref.write(sharedPreferences, prefixes)

        val settingsState: LiveData<SettingsState> =
            MediatorLiveData<SettingsState>().apply {
                fun compute() {
                    value =
                        SettingsState(
                            decimalSeparator = getDecimalSeparator(),
                            defaultCurrency = getDefaultCurrency(),
                            defaultStatus = getDefaultStatus(),
                            currencyBeforeAmount = getCurrencyBeforeAmount(),
                            postingWidth = getPostingWidth(),
                            currencyAmountSpacing = getCurrencyAmountSpacing(),
                            transactionStatusPresentByDefault = getTransactionStatusPresentByDefault(),
                            transactionCodePresentByDefault = getTransactionCodePresentByDefault(),
                            transactionPayeePresentByDefault = getTransactionPayeePresentByDefault(),
                            transactionNotePresentByDefault = getTransactionNotePresentByDefault(),
                            transactionCurrenciesPresentByDefault = getTransactionCurrenciesPresentByDefault(),
                            postingAmountPresentByDefault = getPostingAmountPresentByDefault(),
                            postingCostPresentByDefault = getPostingCostPresentByDefault(),
                            postingAssertionPresentByDefault = getPostingAssertionPresentByDefault(),
                            postingAssertionCostPresentByDefault = getPostingAssertionCostPresentByDefault(),
                            postingCommentPresentByDefault = getPostingCommentPresentByDefault(),
                            prefixes =
                                AccountTypePrefixes(
                                    assets = getAssetsPrefixes(),
                                    liabilities = getLiabilitiesPrefixes(),
                                    equity = getEquityPrefixes(),
                                    income = getIncomePrefixes(),
                                    expenses = getExpensesPrefixes(),
                                ),
                        )
                }
                compute()

                fun <T> watch(source: LiveData<T>) {
                    addSource(source) { compute() }
                }
                watch(decimalSeparator)
                watch(defaultCurrency)
                watch(defaultStatus)
                watch(currencyBeforeAmount)
                watch(postingWidth)
                watch(spacingBetweenCurrencyAndAmount)
                watch(transactionStatusPresentByDefault)
                watch(transactionCodePresentByDefault)
                watch(transactionPayeePresentByDefault)
                watch(transactionNotePresentByDefault)
                watch(transactionCurrenciesPresentByDefault)
                watch(postingAmountPresentByDefault)
                watch(postingCostPresentByDefault)
                watch(postingAssertionPresentByDefault)
                watch(postingAssertionCostPresentByDefault)
                watch(postingCommentPresentByDefault)
                watch(assetsPrefixes)
                watch(liabilitiesPrefixes)
                watch(equityPrefixes)
                watch(incomePrefixes)
                watch(expensesPrefixes)
            }

        val reportingPreferences: LiveData<ReportingPreferences> =
            MediatorLiveData<ReportingPreferences>().apply {
                fun compute() {
                    value =
                        ReportingPreferences(
                            decimalSeparator = getDecimalSeparator(),
                            prefixes =
                                AccountTypePrefixes(
                                    assets = getAssetsPrefixes(),
                                    liabilities = getLiabilitiesPrefixes(),
                                    equity = getEquityPrefixes(),
                                    income = getIncomePrefixes(),
                                    expenses = getExpensesPrefixes(),
                                ),
                        )
                }
                addSource(decimalSeparator) { compute() }
                addSource(assetsPrefixes) { compute() }
                addSource(liabilitiesPrefixes) { compute() }
                addSource(equityPrefixes) { compute() }
                addSource(incomePrefixes) { compute() }
                addSource(expensesPrefixes) { compute() }
            }
    }
