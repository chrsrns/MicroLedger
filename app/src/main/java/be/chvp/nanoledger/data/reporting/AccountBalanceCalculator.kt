package be.chvp.nanoledger.data.reporting

import be.chvp.nanoledger.data.Transaction
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
                        account.startsWith("Liabilities", ignoreCase = true) -> rawBalance.negate()
                        account.startsWith("Equity", ignoreCase = true) -> rawBalance.negate()
                        account.startsWith("Income", ignoreCase = true) -> rawBalance.negate()
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
                    account.startsWith("Assets", ignoreCase = true) -> assets.add(accountBalance)
                    account.startsWith("Liabilities", ignoreCase = true) ->
                        liabilities.add(
                            accountBalance,
                        )
                    account.startsWith("Equity", ignoreCase = true) -> equity.add(accountBalance)
                    account.startsWith("Income", ignoreCase = true) -> income.add(accountBalance)
                    account.startsWith("Expenses", ignoreCase = true) ->
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
