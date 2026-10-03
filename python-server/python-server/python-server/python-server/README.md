# XAU AI Trader — Python Server API

Python backend API for the XAU/USD M5 AI Trader.

## Features

- Bot status
- XAU/USD M5 signal
- AI confidence
- Entry price
- Stop Loss
- Take Profit 1
- Take Profit 2
- Account information
- Open positions
- Start/stop bot commands

## API endpoints

GET /

GET /status

GET /signal

GET /account

GET /positions

POST /bot/start

POST /bot/stop

## Configuration

Copy `.env.example` to `.env` and enter the required MT5 and API settings.

Never commit real passwords, API tokens, or other secrets to GitHub.

## Run locally

Install dependencies:

    pip install -r requirements.txt

Start the API:

    uvicorn main:app --host 0.0.0.0 --port 8000
