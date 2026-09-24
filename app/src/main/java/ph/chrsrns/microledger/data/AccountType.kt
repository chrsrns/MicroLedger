package ph.chrsrns.microledger.data

enum class AccountType {
    ASSETS,
    LIABILITIES,
    EQUITY,
    INCOME,
    EXPENSES,
}

data class AccountTypePrefixes(
    val assets: List<String>,
    val liabilities: List<String>,
    val equity: List<String>,
    val income: List<String>,
    val expenses: List<String>,
) {
    companion object {
        val EMPTY =
            AccountTypePrefixes(
                assets = emptyList(),
                liabilities = emptyList(),
                equity = emptyList(),
                income = emptyList(),
                expenses = emptyList(),
            )
    }

    fun classify(account: String?): AccountType? {
        val accountName = account?.trim() ?: return null
        if (accountName.isBlank()) return null
        return when {
            assets.any { accountName.startsWith(it, ignoreCase = true) } -> AccountType.ASSETS
            liabilities.any { accountName.startsWith(it, ignoreCase = true) } -> AccountType.LIABILITIES
            equity.any { accountName.startsWith(it, ignoreCase = true) } -> AccountType.EQUITY
            income.any { accountName.startsWith(it, ignoreCase = true) } -> AccountType.INCOME
            expenses.any { accountName.startsWith(it, ignoreCase = true) } -> AccountType.EXPENSES
            else -> null
        }
    }
}
