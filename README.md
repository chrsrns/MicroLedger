MicroLedger is a [Plain Text Accounting](https://plaintextaccounting.org/) data entry app for Android.

<a href="https://f-droid.org/packages/ph.chrsrns.microledger/"><img alt="Get it on F-Droid" src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="80px"/></a>

Only ledger/hledger-like syntax is supported.
View your transactions and quickly add new ones, with autocomplete on fields where it makes sense.

MicroLedger also supports deleting and editing transactions.
For editing, note that MicroLedger does not support the full (h)ledger syntax.
Transactions added with MicroLedger should be editable, but if you use more esoteric amount syntax, MicroLedger might not parse those correctly.
Dates are also required to be in ISO syntax (the same way MicroLedger writes them out).
If your date is not in this format, the current date will be picked.
