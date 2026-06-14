package ph.chrsrns.microledger.data.reporting

import ph.chrsrns.microledger.data.Transaction
import java.math.BigDecimal

/**
 * Calculates account balances from a list of transactions.
 *
 * Provides current balances for each account, grouped by account type
 * (Assets, Liabilities, Equity, Income, Expenses).
 */
class AccountBalanceCalculator {
    /**
     * Balance information for a single account.
     */
    data class AccountBalance(
        val account: String,
        val balance: BigDecimal,
        val currency: String,
        val transactions: List<Transaction>,
    )

    /**
     * Result containing balances grouped by account type.
     */
    data class AccountBalancesResult(
        val assets: List<AccountBalance>,
        val liabilities: List<AccountBalance>,
        val equity: List<AccountBalance>,
        val income: List<AccountBalance>,
        val expenses: List<AccountBalance>,
    )

    /**
     * Calculates all account balances from the given transactions.
     *
     * @param transactions List of transactions to process
     * @param decimalSeparator The user's decimal separator used to parse quantities
     * @return AccountBalancesResult containing balances grouped by type
     */
    fun calculate(
        transactions: List<Transaction>,
        decimalSeparator: String,
        assetsPrefixes: List<String> = listOf("Assets"),
        liabilitiesPrefixes: List<String> = listOf("Liabilities"),
        equityPrefixes: List<String> = listOf("Equity"),
        incomePrefixes: List<String> = listOf("Income"),
        expensesPrefixes: List<String> = listOf("Expenses"),
    ): AccountBalancesResult {
        // Map of account name -> (currency -> (balance, mutableSet of transactions))
        val accountBalances =
            mutableMapOf<String, MutableMap<String, Pair<BigDecimal, MutableSet<Transaction>>>>()

        for (transaction in transactions) {
            for (posting in transaction.postings) {
                val amount = posting.amount ?: continue
                val account = posting.account ?: continue
                val quantity = parseQuantity(amount.quantity, decimalSeparator)
                val currency = amount.currency

                val currencyMap = accountBalances.getOrPut(account) { mutableMapOf() }
                val (existingBalance, existingTransactions) =
                    currencyMap[currency] ?: Pair(BigDecimal.ZERO, mutableSetOf())
                existingTransactions.add(transaction)
                currencyMap[currency] = Pair(existingBalance + quantity, existingTransactions)
            }
        }

        // Split by account type
        val assets = mutableListOf<AccountBalance>()
        val liabilities = mutableListOf<AccountBalance>()
        val equity = mutableListOf<AccountBalance>()
        val income = mutableListOf<AccountBalance>()
        val expenses = mutableListOf<AccountBalance>()

        for ((account, currencyMap) in accountBalances) {
            for ((currency, pair) in currencyMap) {
                val (rawBalance, transactionSet) = pair
                // For display purposes, negate Liability, Equity, and Income balances so they show as positive
                // (these are credit accounts stored as negative amounts in ledger postings)
                val displayBalance =
                    when {
                        liabilitiesPrefixes.any { account.startsWith(it, ignoreCase = true) } -> rawBalance.negate()
                        equityPrefixes.any { account.startsWith(it, ignoreCase = true) } -> rawBalance.negate()
                        incomePrefixes.any { account.startsWith(it, ignoreCase = true) } -> rawBalance.negate()
                        else -> rawBalance
                    }

                val accountBalance =
                    AccountBalance(
                        account = account,
                        balance = displayBalance,
                        currency = currency,
                        transactions = transactionSet.sortedBy { it.firstLine },
                    )

                when {
                    assetsPrefixes.any { account.startsWith(it, ignoreCase = true) } -> assets.add(accountBalance)
                    liabilitiesPrefixes.any { account.startsWith(it, ignoreCase = true) } ->
                        liabilities.add(
                            accountBalance,
                        )
                    equityPrefixes.any { account.startsWith(it, ignoreCase = true) } -> equity.add(accountBalance)
                    incomePrefixes.any { account.startsWith(it, ignoreCase = true) } -> income.add(accountBalance)
                    expensesPrefixes.any { account.startsWith(it, ignoreCase = true) } ->
                        expenses.add(
                            accountBalance,
                        )
                }
            }
        }

        // Sort each list by account name for consistent output
        return AccountBalancesResult(
            assets = assets.sortedBy { it.account },
            liabilities = liabilities.sortedBy { it.account },
            equity = equity.sortedBy { it.account },
            income = income.sortedBy { it.account },
            expenses = expenses.sortedBy { it.account },
        )
    }
}
