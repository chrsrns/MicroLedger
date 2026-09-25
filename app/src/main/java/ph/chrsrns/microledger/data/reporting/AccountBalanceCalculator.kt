package ph.chrsrns.microledger.data.reporting

import ph.chrsrns.microledger.data.AccountType
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
     * Calculates all account balances from the given inputs.
     *
     * @param inputs Transactions and reporting preferences to process
     * @return AccountBalancesResult containing balances grouped by type
     */
    fun calculate(inputs: ReportingInputs): AccountBalancesResult {
        val decimalSeparator = inputs.preferences.decimalSeparator
        val prefixes = inputs.preferences.prefixes

        // Map of account name -> (currency -> (balance, mutableSet of transactions))
        val accountBalances =
            mutableMapOf<String, MutableMap<String, Pair<BigDecimal, MutableSet<Transaction>>>>()

        for (transaction in inputs.transactions) {
            for (posting in materializeElidedAmounts(transaction, decimalSeparator).postings) {
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
                val type = prefixes.classify(account)
                // For display purposes, negate Liability, Equity, and Income balances so they show as positive
                // (these are credit accounts stored as negative amounts in ledger postings)
                val displayBalance =
                    when (type) {
                        AccountType.LIABILITIES,
                        AccountType.EQUITY,
                        AccountType.INCOME,
                        -> rawBalance.negate()

                        else -> rawBalance
                    }

                val accountBalance =
                    AccountBalance(
                        account = account,
                        balance = displayBalance,
                        currency = currency,
                        transactions = transactionSet.sortedBy { it.firstLine },
                    )

                when (type) {
                    AccountType.ASSETS -> assets.add(accountBalance)
                    AccountType.LIABILITIES -> liabilities.add(accountBalance)
                    AccountType.EQUITY -> equity.add(accountBalance)
                    AccountType.INCOME -> income.add(accountBalance)
                    AccountType.EXPENSES -> expenses.add(accountBalance)
                    null -> {}
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
