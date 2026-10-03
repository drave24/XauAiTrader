from fastapi import FastAPI
from datetime import datetime

app = FastAPI(title="XAU AI Trader API")


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
    return {
        "symbol": "XAUUSD",
        "timeframe": "M5",
        "signal": "WAIT",
        "confidence": 0,
        "entry": 0,
        "stop_loss": 0,
        "take_profit_1": 0,
        "take_profit_2": 0
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
