# Tools

This folder contains local helper scripts for project maintenance and data
preparation.

## Investment price history generator

`main.py` downloads price history from Yahoo Finance through `yfinance`, formats
it for `app_investment.TB_INVESTMENT_PRICE_HISTORY`, and writes both CSV and SQL
INSERT output files.

Current default parameters in `main.py`:

- Ticker: `009810.TW`
- Product ID: `101`
- Start date: `2025-07-20`
- End date: `2026-07-20`
- Created by: `ETL_USER`

## Setup

The tools project uses `uv` and Python `3.14`.

```bash
cd tools
uv sync
```

## Run

Run the script from the `tools` directory so generated files are written under
`tools/investmentData`.

```bash
cd tools
uv run python main.py
```

Generated files use this naming pattern:

- `investmentData/price_history_<TICKER>_<START_DATE>_<END_DATE>.csv`
- `investmentData/insert_data_<TICKER>_<START_DATE>_<END_DATE>.sql`

Before running the generated SQL, confirm the `PRODUCT_ID` matches the target row
in `TB_INVESTMENT_PRODUCT`.
