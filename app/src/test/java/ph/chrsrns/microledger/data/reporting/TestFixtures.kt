package ph.chrsrns.microledger.data.reporting

import ph.chrsrns.microledger.data.Amount
import ph.chrsrns.microledger.data.Posting
import ph.chrsrns.microledger.data.Transaction

/**
 * Test fixtures and factory functions for creating test data.
 * These reduce boilerplate in calculator tests.
 */

/** Create an Amount with default display value matching quantity + currency */
fun amount(
    quantity: String,
    currency: String = "$",
    original: String = "$quantity $currency".trim(),
): Amount = Amount(quantity, currency, original)

/** Create a Posting with sensible defaults for common test scenarios */
fun posting(
    account: String,
    amount: Amount,
): Posting =
    Posting(
        account = account,
        amount = amount,
        cost = null,
        assertion = null,
        assertionCost = null,
        comment = null,
    )

/** Create a Transaction with sensible defaults */
fun transaction(
    date: String,
    payee: String,
    firstLine: Int,
    lastLine: Int,
    postings: List<Posting>,
    status: String? = null,
    code: String? = null,
    note: String? = null,
): Transaction =
    Transaction(
        firstLine = firstLine,
        lastLine = lastLine,
        date = date,
        status = status,
        code = code,
        payee = payee,
        note = note,
        postings = postings,
    )

/** Common account names used across tests */
object Accounts {
    const val CHECKING = "Assets:Checking"
    const val SAVINGS = "Assets:Savings"
    const val CASH = "Assets:Cash"
    const val CREDIT_CARD = "Liabilities:Credit Card"
    const val OPENING = "Equity:Opening Balances"
    const val SALARY = "Income:Salary"
    const val FOOD = "Expenses:Food"
    const val RENT = "Expenses:Housing:Rent"
}

/**
 * Creates a simple two-posting transaction with an asset account and equity.
 * Useful for "opening balance" style transactions.
 */
fun openingTransaction(
    account: String = Accounts.CHECKING,
    amount: String = "1000.00",
    currency: String = "$",
    date: String = "2024-01-15",
    payee: String = "Opening Balance",
    firstLine: Int = 1,
): Transaction {
    val amt = amount(amount, currency)
    return transaction(
        date = date,
        payee = payee,
        firstLine = firstLine,
        lastLine = firstLine + 2,
        postings =
            listOf(
                posting(account, amt),
                posting(Accounts.OPENING, amount("-$amount", currency)),
            ),
    )
}

/**
 * Creates an income transaction: asset account receives money from income account.
 */
fun incomeTransaction(
    toAccount: String = Accounts.CHECKING,
    amount: String = "5000.00",
    currency: String = "$",
    fromIncome: String = Accounts.SALARY,
    date: String = "2024-01-15",
    payee: String = "Employer",
    firstLine: Int = 1,
): Transaction {
    val amt = amount(amount, currency)
    return transaction(
        date = date,
        payee = payee,
        firstLine = firstLine,
        lastLine = firstLine + 2,
        postings =
            listOf(
                posting(toAccount, amt),
                posting(fromIncome, amount("-$amount", currency)),
            ),
    )
}

/**
 * Creates an expense transaction: expense account debited, asset credited.
 */
fun expenseTransaction(
    expenseAccount: String = Accounts.FOOD,
    amount: String = "150.00",
    currency: String = "$",
    fromAsset: String = Accounts.CHECKING,
    date: String = "2024-01-15",
    payee: String = "Store",
    firstLine: Int = 1,
): Transaction {
    val amt = amount(amount, currency)
    return transaction(
        date = date,
        payee = payee,
        firstLine = firstLine,
        lastLine = firstLine + 2,
        postings =
            listOf(
                posting(expenseAccount, amt),
                posting(fromAsset, amount("-$amount", currency)),
            ),
    )
}

/**
 * Creates a liability transaction: expense incurred on credit.
 */
fun liabilityTransaction(
    expenseAccount: String = Accounts.FOOD,
    amount: String = "150.00",
    currency: String = "$",
    liabilityAccount: String = Accounts.CREDIT_CARD,
    date: String = "2024-01-15",
    payee: String = "Purchase",
    firstLine: Int = 1,
): Transaction {
    val amt = amount(amount, currency)
    return transaction(
        date = date,
        payee = payee,
        firstLine = firstLine,
        lastLine = firstLine + 2,
        postings =
            listOf(
                posting(expenseAccount, amt),
                posting(liabilityAccount, amount("-$amount", currency)),
            ),
    )
}

/**
 * Creates a multi-posting transaction.
 */
fun multiPostingTransaction(
    date: String = "2024-01-15",
    payee: String = "Complex Transaction",
    firstLine: Int = 1,
    vararg postingConfigs: Pair<String, String>,
): Transaction {
    val postings =
        postingConfigs.mapIndexed { index, (account, amountStr) ->
            val isNegative = amountStr.startsWith("-")
            val cleanAmount = amountStr.removePrefix("-")
            val currency =
                if (cleanAmount.contains(" ")) {
                    cleanAmount.substringAfter(" ")
                } else {
                    "$"
                }
            val qty = cleanAmount.substringBefore(" ")
            posting(account, amount(if (isNegative) "-$qty" else qty, currency))
        }
    return transaction(
        date = date,
        payee = payee,
        firstLine = firstLine,
        lastLine = firstLine + postings.size + 1,
        postings = postings,
    )
}
