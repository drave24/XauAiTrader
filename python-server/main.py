import os
from datetime import datetime

from fastapi import FastAPI
from dotenv import load_dotenv

try:
    import MetaTrader5 as mt5
except ImportError:
    mt5 = None

load_dotenv()

app = FastAPI(title="XAU AI Trader API")



def get_mt5_candles(symbol="XAUUSD", timeframe_name="M5", bars=100):
    """Get recent candles from MetaTrader 5.

    This works on the future Windows VPS where the MT5 terminal is installed.
    It returns None when MT5 is unavailable.
    """
    if mt5 is None:
        return None

    timeframe = getattr(mt5, "TIMEFRAME_M5", None)
    if timeframe is None:
        return None

    if not mt5.initialize():
        return None

    try:
        rates = mt5.copy_rates_from_pos(symbol, timeframe, 0, bars)

        if rates is None or len(rates) == 0:
            return None

        return pd.DataFrame(rates)
    finally:
        mt5.shutdown()

@app.get("/")
def home():
    return {
        "name": "XAU AI Trader API",
        "status": "online"
    }


@app.get("/status")
def status():
    return {
        "bot": "XAU AI Trader",
        "status": "stopped",
        "symbol": "XAUUSD",
        "timeframe": "M5",
        "server_time": datetime.utcnow().isoformat()
    }


@app.get("/signal")
def signal():
    import pandas as pd
    from ai_logic import generate_signal

    # Temporary simulated XAUUSD M5 candles for testing.
    # Real MT5 candles will be connected later on Windows VPS.
    prices = [
        2678.0, 2679.2, 2680.1, 2681.0, 2680.7,
        2681.8, 2682.4, 2683.1, 2682.8, 2684.0,
        2684.8, 2685.2, 2684.9, 2685.6, 2686.1,
        2686.8, 2687.2, 2686.9, 2687.8, 2688.4,
        2688.9, 2689.2, 2688.7, 2689.6, 2690.1,
        2690.5, 2691.0, 2690.8, 2691.5, 2692.0,
        2692.4, 2693.0, 2692.7, 2693.5, 2694.1,
        2694.6, 2695.0, 2695.4, 2696.0, 2696.5,
        2697.0, 2697.4, 2698.0, 2698.5, 2699.0,
        2699.4, 2700.0, 2700.5, 2701.0, 2701.5,
        2702.0, 2702.4, 2703.0, 2703.5, 2704.0,
        2704.5, 2705.0, 2705.4, 2706.0, 2706.5
    ]

    # Use real MT5 candles when MT5 is available.
    # Otherwise use simulated candles for free Colab testing.
    data = get_mt5_candles("XAUUSD", "M5", 100)

    if data is None:
        data = pd.DataFrame({
            "open": [p - 0.3 for p in prices],
            "high": [p + 0.5 for p in prices],
            "low": [p - 0.5 for p in prices],
            "close": prices
        })

    result = generate_signal(data)

    return {
        "symbol": "XAUUSD",
        "timeframe": "M5",
        **result
    }


@app.get("/positions")
def positions():
    return {
        "positions": []
    }


@app.get("/account")
def account():
    return {
        "balance": 0,
        "equity": 0,
        "profit": 0,
        "currency": "USD"
    }


@app.post("/bot/start")
def start_bot():
    return {
        "success": True,
        "status": "start_requested"
    }


@app.post("/bot/stop")
def stop_bot():
    return {
        "success": True,
        "status": "stop_requested"
    }
@app.get("/mt5")
def mt5_status():
    if mt5 is None:
        return {
            "installed": False,
            "connected": False,
            "status": "MetaTrader5 package not available"
        }

    connected = mt5.initialize()

    if not connected:
        return {
            "installed": True,
            "connected": False,
            "status": "MT5 terminal not connected"
        }

    mt5.shutdown()

    return {
        "installed": True,
        "connected": True,
        "status": "MT5 terminal available"
    }
