# market-snapshot-tool

Small Java CLI project for fetching crypto market data, computing a few basic statistics, and printing a clean terminal summary.

## MVP goal

Build a minimal, extendable command-line tool that:

- fetches public crypto market data
- parses JSON responses
- computes simple statistics
- prints readable terminal output

## Planned MVP scope

- assets: `BTC`, `ETH`, `SOL`
- metrics: current price, market cap, market cap rank, 24h change, 7d return, 30d return, realized volatility
- API: CoinGecko
- stack: Java 21, Maven, `HttpClient`, Jackson

## Current status

The repository now contains a minimal Maven starter so the project can compile and run cleanly before the market-data logic is added.

## Run

```bash
mvn compile
mvn exec:java
```

Optional for more reliable CoinGecko access during development:

```bash
$env:COINGECKO_DEMO_API_KEY="your-demo-key"
mvn exec:java
```
