# MicroLedger journal format

MicroLedger reads a pragmatic subset of (h)ledger plain-text journal syntax. A file is a sequence of transactions separated by blank lines.

## Encoding

- UTF-8.
- LF line endings.
- Lines starting with `;` are comments, except for the special template block markers.

## Transaction header

```
YYYY-MM-DD [STATUS] [(CODE)] PAYEE [| NOTE]
```

- `YYYY-MM-DD`: the transaction date. Use hyphens. The regex also accepts `/` and `.`, but the monthly cash-flow parser splits the date on `-`, so other separators break the dashboard period filter.
- `STATUS`: optional. `*` for cleared, `!` for pending.
- `(CODE)`: optional transaction code, e.g. `(INV-001)`.
- `PAYEE`: required free text.
- `NOTE`: optional, after `|`.

## Postings

Each posting is indented with at least one space or tab. The account and amount are separated by at least two spaces or a tab.

```
    Account:Subaccount  AMOUNT
```

Account names can contain spaces and colons. Colons are the conventional separator for sub-accounts.

## Amounts

The amount is split into a quantity and a currency. Both forms are accepted:

- Currency before quantity: `€ 1000.00`, `EUR 1000.00`, `€1000.00`.
- Currency after quantity: `1000.00 €`, `1000.00EUR`.

The quantity is a number with an optional leading minus. Use the decimal separator configured in the app (`.` by default). Avoid thousands separators; they are stripped and may produce unexpected numbers.

## Balanced transactions

MicroLedger does not enforce that postings sum to zero. For correct dashboard numbers, credit accounts (Liabilities, Equity, Income) should carry negative amounts and debit accounts (Assets, Expenses) positive amounts.

Examples:

```
2026-07-01 * Opening Balance
    Assets:Checking        € 1000.00
    Equity:Opening        € -1000.00
```

```
2026-08-01 * Employer | Salary
    Assets:Checking        € 2500.00
    Income:Salary         € -2500.00
```

```
2026-08-02 * Supermarket
    Expenses:Food          € 85.50
    Assets:Checking       € -85.50
```

## Costs and assertions

- Unit cost: `€ 10 @ 5 USD`
- Total cost: `€ 10 @@ 50 USD`
- Balance assertion: `Assets:Checking  € 1000.00 = € 1000.00`

Cost and assertion support is limited to the forms above.

## Comments

A comment starts with `;`. It can appear at the end of a posting or on its own indented line.

```
    Assets:Cash  € 50.00  ; withdrew today
    ; this posting is just a note
```

## Account types for reporting

The dashboard classifies accounts by prefix, case-insensitively:

- `Assets`
- `Liabilities`
- `Equity`
- `Income`
- `Expenses`

Sub-accounts inherit the type: `Expenses:Food` is an expense. The prefixes can be customized in the app, but the default list above is recommended for a shareable demo file.

## Templates

Optional templates can live at the top of the file as commented blocks. They do not appear on the dashboard but can speed up data entry.

```
; template-start: Grocery
; id: grocery
; payee: Supermarket
; account: Expenses:Food
; account: Assets:Checking
; template-end
```

## Unsupported constructs

Do not rely on these:

- `include other.journal` (not resolved)
- Amount expressions like `(1 * € 2)`
- Commodity declarations
- Automated transactions

Keep the file to the patterns in this guide for reliable parsing and round-tripping.

## Minimal example

```
2026-08-03 * Supermarket
    Expenses:Food          € 45.00
    Assets:Checking       € -45.00
```

This is enough to appear in the expenses list and the monthly cash-flow card.
