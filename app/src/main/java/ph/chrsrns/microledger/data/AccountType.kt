package ph.chrsrns.microledger.data

enum class AccountType {
    ASSETS,
    LIABILITIES,
    EQUITY,
    INCOME,
    EXPENSES,
}

data class AccountTypePrefixes(
    val assets: List<String> = emptyList(),
    val liabilities: List<String> = emptyList(),
    val equity: List<String> = emptyList(),
    val income: List<String> = emptyList(),
    val expenses: List<String> = emptyList(),
) {
    companion object {
        val EMPTY = AccountTypePrefixes()
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
