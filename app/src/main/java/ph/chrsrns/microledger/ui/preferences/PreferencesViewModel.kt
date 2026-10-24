package ph.chrsrns.microledger.ui.preferences

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import ph.chrsrns.microledger.data.PreferencesDataSource
import ph.chrsrns.microledger.data.SettingsState
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel
    @Inject
    constructor(
        application: Application,
        private val preferencesDataSource: PreferencesDataSource,
    ) : AndroidViewModel(application) {
        val fileUri: LiveData<Uri?> = preferencesDataSource.fileUri
        val settingsState: LiveData<SettingsState> = preferencesDataSource.settingsState

        val settingsCallbacks =
            SettingsCallbacks(
                onDecimalSeparatorChange = ::storeDecimalSeparator,
                onDefaultCurrencyChange = ::storeDefaultCurrency,
                onDefaultStatusChange = ::storeDefaultStatus,
                onCurrencyBeforeAmountChange = ::storeCurrencyBeforeAmount,
                onPostingWidthChange = ::storePostingWidth,
                onCurrencyAmountSpacingChange = ::storeCurrencyAmountSpacing,
                onTransactionStatusPresentByDefaultChange = ::storeTransactionStatusPresentByDefault,
                onTransactionCodePresentByDefaultChange = ::storeTransactionCodePresentByDefault,
                onTransactionPayeePresentByDefaultChange = ::storeTransactionPayeePresentByDefault,
                onTransactionNotePresentByDefaultChange = ::storeTransactionNotePresentByDefault,
                onTransactionCurrenciesPresentByDefaultChange = ::storeTransactionCurrenciesPresentByDefault,
                onPostingAmountPresentByDefaultChange = ::storePostingAmountPresentByDefault,
                onPostingCostPresentByDefaultChange = ::storePostingCostPresentByDefault,
                onPostingAssertionPresentByDefaultChange = ::storePostingAssertionPresentByDefault,
                onPostingAssertionCostPresentByDefaultChange = ::storePostingAssertionCostPresentByDefault,
                onPostingCommentPresentByDefaultChange = ::storePostingCommentPresentByDefault,
                onAssetsPrefixesChange = ::storeAssetsPrefixes,
                onLiabilitiesPrefixesChange = ::storeLiabilitiesPrefixes,
                onEquityPrefixesChange = ::storeEquityPrefixes,
                onIncomePrefixesChange = ::storeIncomePrefixes,
                onExpensesPrefixesChange = ::storeExpensesPrefixes,
            )

        fun storeFileUri(uri: Uri) = preferencesDataSource.setFileUri(uri)

        fun storeDecimalSeparator(separator: String) = preferencesDataSource.setDecimalSeparator(separator)

        fun storeDefaultCurrency(currency: String) = preferencesDataSource.setDefaultCurrency(currency)

        fun storeDefaultStatus(status: String) = preferencesDataSource.setDefaultStatus(status)

        fun storeCurrencyBeforeAmount(enable: Boolean) =
            preferencesDataSource.setCurrencyBeforeAmount(
                enable,
            )

        fun storePostingWidth(width: Int) = preferencesDataSource.setPostingWidth(width)

        fun storeCurrencyAmountSpacing(spacing: Boolean) = preferencesDataSource.setCurrencyAmountSpacing(spacing)

        fun storeTransactionStatusPresentByDefault(status: Boolean) = preferencesDataSource.setTransactionStatusPresentByDefault(status)

        fun storeTransactionCodePresentByDefault(code: Boolean) = preferencesDataSource.setTransactionCodePresentByDefault(code)

        fun storeTransactionPayeePresentByDefault(payee: Boolean) = preferencesDataSource.setTransactionPayeePresentByDefault(payee)

        fun storeTransactionNotePresentByDefault(note: Boolean) = preferencesDataSource.setTransactionNotePresentByDefault(note)

        fun storeTransactionCurrenciesPresentByDefault(currencies: Boolean) =
            preferencesDataSource.setTransactionCurrenciesPresentByDefault(currencies)

        fun storePostingAmountPresentByDefault(amount: Boolean) = preferencesDataSource.setPostingAmountPresentByDefault(amount)

        fun storePostingCostPresentByDefault(cost: Boolean) = preferencesDataSource.setPostingCostPresentByDefault(cost)

        fun storePostingAssertionPresentByDefault(assertion: Boolean) = preferencesDataSource.setPostingAssertionPresentByDefault(assertion)

        fun storePostingAssertionCostPresentByDefault(assertionCost: Boolean) =
            preferencesDataSource.setPostingAssertionCostPresentByDefault(assertionCost)

        fun storePostingCommentPresentByDefault(comment: Boolean) = preferencesDataSource.setPostingCommentPresentByDefault(comment)

        fun storeAssetsPrefixes(prefixes: List<String>) = preferencesDataSource.setAssetsPrefixes(prefixes)

        fun storeLiabilitiesPrefixes(prefixes: List<String>) = preferencesDataSource.setLiabilitiesPrefixes(prefixes)

        fun storeEquityPrefixes(prefixes: List<String>) = preferencesDataSource.setEquityPrefixes(prefixes)

        fun storeIncomePrefixes(prefixes: List<String>) = preferencesDataSource.setIncomePrefixes(prefixes)

        fun storeExpensesPrefixes(prefixes: List<String>) = preferencesDataSource.setExpensesPrefixes(prefixes)
    }
