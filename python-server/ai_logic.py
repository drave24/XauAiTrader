import pandas as pd


def generate_signal(
    df,
    confidence_threshold=0.62,
    atr_sl_multiplier=1.5,
    atr_tp1_multiplier=2.25,
    atr_tp2_multiplier=3.0
):
    """
    Conservative XAUUSD M5 signal engine.

    Expected columns:
    open, high, low, close

    Returns:
    signal, confidence, entry, stop_loss, take_profit_1,
    take_profit_2, reason
    """

    if df is None or len(df) < 50:
        return {
            "signal": "WAIT",
            "confidence": 0.0,
            "entry": 0,
            "stop_loss": 0,
            "take_profit_1": 0,
            "take_profit_2": 0,
            "reason": "Not enough candle data"
        }

    data = df.copy()

    # EMA trend
    data["ema20"] = data["close"].ewm(span=20, adjust=False).mean()
    data["ema50"] = data["close"].ewm(span=50, adjust=False).mean()

    # RSI
    change = data["close"].diff()
    gain = change.clip(lower=0)
    loss = -change.clip(upper=0)

    avg_gain = gain.rolling(14).mean()
    avg_loss = loss.rolling(14).mean()

    rs = avg_gain / avg_loss.replace(0, float("nan"))
    data["rsi"] = 100 - (100 / (1 + rs))
    data["rsi"] = data["rsi"].fillna(50.0).infer_objects(copy=False).astype(float)

    # ATR
    previous_close = data["close"].shift(1)

    true_range = pd.concat([
        data["high"] - data["low"],
        (data["high"] - previous_close).abs(),
        (data["low"] - previous_close).abs()
    ], axis=1).max(axis=1)

    data["atr"] = true_range.rolling(14).mean()

    last = data.iloc[-1]
    previous = data.iloc[-2]

    entry = float(last["close"])
    atr = float(last["atr"])

    if atr <= 0:
        return {
            "signal": "WAIT",
            "confidence": 0.0,
            "entry": entry,
            "stop_loss": 0,
            "take_profit_1": 0,
            "take_profit_2": 0,
            "reason": "Invalid ATR"
        }

    # Score each condition
    buy_score = 0
    sell_score = 0

    # 1. EMA trend
    if last["ema20"] > last["ema50"]:
        buy_score += 1
    elif last["ema20"] < last["ema50"]:
        sell_score += 1

    # 2. Price relative to EMA20
    if last["close"] > last["ema20"]:
        buy_score += 1
    elif last["close"] < last["ema20"]:
        sell_score += 1

    # 3. RSI momentum
    if 50 < last["rsi"] < 70:
        buy_score += 1
    elif 30 < last["rsi"] < 50:
        sell_score += 1

    # 4. Current candle momentum
    if last["close"] > previous["close"]:
        buy_score += 1
    elif last["close"] < previous["close"]:
        sell_score += 1

    # Require at least 3 of 4 conditions
    if buy_score >= 3 and buy_score > sell_score:
        signal = "BUY"
        confidence = 0.60 + (buy_score * 0.05)
        stop_loss = entry - (atr * atr_sl_multiplier)
        take_profit_1 = entry + (atr * atr_tp1_multiplier)
        take_profit_2 = entry + (atr * atr_tp2_multiplier)
        reason = f"BUY conditions: {buy_score}/4"

    elif sell_score >= 3 and sell_score > buy_score:
        signal = "SELL"
        confidence = 0.60 + (sell_score * 0.05)
        stop_loss = entry + (atr * atr_sl_multiplier)
        take_profit_1 = entry - (atr * atr_tp1_multiplier)
        take_profit_2 = entry - (atr * atr_tp2_multiplier)
        reason = f"SELL conditions: {sell_score}/4"

    else:
        signal = "WAIT"
        confidence = 0.40
        stop_loss = 0
        take_profit_1 = 0
        take_profit_2 = 0
        reason = f"No strong setup: BUY {buy_score}/4, SELL {sell_score}/4"

    # Final confidence filter
    if confidence < confidence_threshold:
        signal = "WAIT"
        stop_loss = 0
        take_profit_1 = 0
        take_profit_2 = 0

    return {
        "signal": signal,
        "confidence": round(float(confidence), 2),
        "entry": round(entry, 2),
        "stop_loss": round(float(stop_loss), 2),
        "take_profit_1": round(float(take_profit_1), 2),
        "take_profit_2": round(float(take_profit_2), 2),
        "reason": reason
    }
