package ph.chrsrns.microledger.ui.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import ph.chrsrns.microledger.data.Transaction
import ph.chrsrns.microledger.data.reporting.ReportingInputs
import ph.chrsrns.microledger.data.ReportingPreferences

// Single owner of the two-source MediatorLiveData wiring for reporting
// LiveData (V343/I231). Emits only when transactions and
// reportingPreferences are both non-null and compute returns non-null;
// extraSources trigger recompute but are not read.
fun <T> reportingLiveData(
    transactions: LiveData<List<Transaction>>,
    reportingPreferences: LiveData<ReportingPreferences>,
    vararg extraSources: LiveData<*>,
    compute: (ReportingInputs) -> T?,
): LiveData<T> =
    MediatorLiveData<T>().apply {
        fun recompute() {
            val txs = transactions.value ?: return
            val prefs = reportingPreferences.value ?: return
            value = compute(ReportingInputs(txs, prefs)) ?: return
        }
        addSource(transactions) { recompute() }
        addSource(reportingPreferences) { recompute() }
        extraSources.forEach { addSource(it) { recompute() } }
    }
