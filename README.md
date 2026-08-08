MicroLedger is a [Plain Text Accounting](https://plaintextaccounting.org/) data entry app for Android — a fork of the simpler app [NanoLedger](https://github.com/chvp/NanoLedger).

<a href="https://f-droid.org/packages/ph.chrsrns.microledger/"><img alt="Get it on F-Droid" src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="80px"/></a>

Only ledger/hledger-like syntax is supported.
View your transactions and quickly add new ones, with autocomplete on fields where it makes sense.

## Features in this fork

- **Dashboard** with a financial overview: net worth, account balances, and monthly cash flow.
- **Account details** — tap an account balance to see all related transactions.
- **Cash-flow details** — tap a cash-flow month to see the related transactions.
- **Transaction templates** to quickly create new entries from saved templates.
- **Add, edit, copy, and delete** transactions, with search and filter on the main list.
- **Autocomplete** for payees, notes, account names, and currencies.
- **Settings** for account type prefixes and other preferences.
- **Tab navigation** between Home, Dashboard, Templates, and Settings in a single Activity.
- **Storage Access Framework** support — keep your journal in any plain-text file on your own storage.
- **Material 3** with dynamic color and dark mode.
- **Offline-first** — no network access is required.

## Demo

<video src="etc/app-recording.webm" controls></video>

## Editing caveats

MicroLedger also supports deleting and editing transactions.
For editing, note that MicroLedger does not support the full (h)ledger syntax.
Transactions added with MicroLedger should be editable, but if you use more esoteric amount syntax, MicroLedger might not parse those correctly.
Dates are also required to be in ISO syntax (the same way MicroLedger writes them out).
If your date is not in this format, the current date will be picked.

## Documentation

- [Architecture overview](docs/architecture.md)
- [Data model](docs/data-model.md)
- [UI screens](docs/ui-screens.md)
- [Journal format](docs/microledger-format.md)

## License

MIT — see [LICENSE](LICENSE).
