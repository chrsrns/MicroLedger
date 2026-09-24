package ph.chrsrns.microledger.data.reporting

import ph.chrsrns.microledger.data.ReportingPreferences
import ph.chrsrns.microledger.data.Transaction

data class ReportingInputs(
    val transactions: List<Transaction>,
    val preferences: ReportingPreferences,
)
