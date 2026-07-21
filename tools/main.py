from datetime import datetime
import numpy as np
import pandas as pd
import yfinance as yf
from pathlib import Path


def fetch_and_format_price_data(
    ticker_symbol: str,
    product_id: int,
    start_date: str,
    end_date: str,
    created_by: str = "SYSTEM_SYNC",
) -> pd.DataFrame:
    """下載 yfinance 資料並轉成符合 TB_INVESTMENT_PRICE_HISTORY schema 的 DataFrame"""
    # 1. 從 yfinance 下載資料
    df = yf.download(
        ticker_symbol, start=start_date, end=end_date, progress=False
    )

    if df.empty:
        print(f"警告：未取得 {ticker_symbol} 的資料。")
        return pd.DataFrame()

    # 處理 yfinance 可能回傳的多層欄位索引 (MultiIndex)
    if isinstance(df.columns, pd.MultiIndex):
        df = df["Close"]
        if ticker_symbol in df.columns:
            df = df[[ticker_symbol]].rename(
                columns={ticker_symbol: "market_price"}
            )
        else:
            df.columns = ["market_price"]
    else:
        df = df[["Close"]].rename(columns={"Close": "market_price"})

    # 重置索引，讓 Date 變成欄位
    df = df.reset_index()

    # 2. 映射與填補 SQL schema 要求的欄位
    # 注意：history_id 是 GENERATED ALWAYS AS IDENTITY，故不傳入
    df["product_id"] = int(product_id)
    df["price_date"] = pd.to_datetime(df["Date"]).dt.date
    df["market_price"] = df["market_price"].round(4)  # NUMBER(12,4)
    df["return_rate"] = np.nan  # 留空值 (NULL)

    df["created_by"] = created_by
    df["created_at"] = datetime.utcnow()
    df["updated_by"] = created_by
    df["updated_at"] = datetime.utcnow()

    # 3. 調整欄位順序與過濾
    target_columns = [
        "product_id",
        "price_date",
        "market_price",
        "return_rate",
        "created_by",
        "created_at",
        "updated_by",
        "updated_at",
    ]

    return df[target_columns]


def generate_insert_sql(
    df: pd.DataFrame, table_name: str = "app_investment.TB_INVESTMENT_PRICE_HISTORY"
) -> str:
    """將 DataFrame 轉換為 SQL INSERT 腳本"""
    sql_statements = []
    for _, row in df.iterrows():
        # 處理 NULL 值與日期格式
        return_rate_val = (
            "NULL" if pd.isna(row["return_rate"]) else row["return_rate"]
        )
        price_date_str = f"TO_DATE('{row['price_date']}', 'YYYY-MM-DD')"
        created_at_str = f"TO_TIMESTAMP('{row['created_at'].strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]}', 'YYYY-MM-DD HH24:MI:SS.FF3')"

        sql = f"""INSERT INTO {table_name} (
    product_id, price_date, market_price, return_rate, created_by, created_at, updated_by, updated_at
) VALUES (
    {row['product_id']}, {price_date_str}, {row['market_price']}, {return_rate_val}, 
    '{row['created_by']}', {created_at_str}, '{row['updated_by']}', {created_at_str}
);"""
        sql_statements.append(sql)

    return "\n".join(sql_statements)


# ==========================================
# 使用範例
# ==========================================
if __name__ == "__main__":
    # 設定參數
    TICKER = "009810.TW"  # 元大台灣50 (如果是美股如 AAPL 直接填 "AAPL")
    PRODUCT_ID = 101  # 對應 TB_INVESTMENT_PRODUCT 的 product_id
    START_DATE = "2025-07-20"
    END_DATE = "2026-07-20"

    # 取得處理好的資料
    processed_df = fetch_and_format_price_data(
        ticker_symbol=TICKER,
        product_id=PRODUCT_ID,
        start_date=START_DATE,
        end_date=END_DATE,
        created_by="ETL_USER",
    )

    print("--- 整理後的 DataFrame 預覽 ---")
    print(processed_df.head())
    
    output_dir = Path("investmentData")
    output_dir.mkdir(parents=True, exist_ok=True)  # 如果資料夾不存在就自動建立

    csv_filename = output_dir / f"price_history_{TICKER}_{START_DATE}_{END_DATE}.csv"

    # 匯出 CSV (可供 SQL Developer / DBeaver 匯入)
    processed_df.to_csv(
        csv_filename, index=False, date_format="%Y-%m-%d"
    )
    print(f"\n[V] CSV 檔案已儲存：{csv_filename}")

    # 產生 INSERT 語句 (可直接複製到 SQL 工具執行)
    sql_filename = output_dir / f"insert_data_{TICKER}_{START_DATE}_{END_DATE}.sql"
    sql_script = generate_insert_sql(processed_df)
    with open(sql_filename, "w", encoding="utf-8") as f:
        f.write(sql_script)
    print(f"[V] SQL INSERT 腳本已儲存：{sql_filename}")