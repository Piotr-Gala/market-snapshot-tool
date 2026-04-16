# market-snapshot-tool

Small Java CLI tool that fetches live crypto market data, computes a few simple analytics, and prints a clean terminal snapshot.

It also includes a mini desktop UI, so you can click asset and sorting options instead of using the terminal for every refresh.
Short-lived in-memory caching helps reduce CoinGecko rate-limit pain during repeated refreshes.
The app also handles partial asset failures more gracefully and can export the latest snapshot to CSV.

This project is intentionally small. It is not a trading bot, not an alpha engine, and not a full market data platform. The goal is to show a clean, practical MVP built with plain Java and Maven.

## What it does

- fetches live market data for `BTC`, `ETH`, and `SOL`
- pulls recent price history from CoinGecko
- computes `7d return`, `30d return`, and `30d realized volatility (annualized)`
- shows `market cap`
- prints a compact market snapshot and a tracked-assets ranking by market cap

## Example output

```text
market-snapshot-tool

Snapshot

BTC  Bitcoin    price:   $74,773.74 | 24h:    0.75% | 7d:    4.86% | 30d:    0.74% | 30d vol (ann.):   42.01% | mc:     $1.50T
ETH  Ethereum   price:    $2,359.68 | 24h:    1.91% | 7d:    6.88% | 30d:    0.59% | 30d vol (ann.):   55.62% | mc:   $284.79B
SOL  Solana     price:       $84.79 | 24h:    1.36% | 7d:    2.07% | 30d:  -11.48% | 30d vol (ann.):   53.59% | mc:    $48.78B

Tracked assets by market cap

1. BTC  Bitcoin    | mc:     $1.50T
2. ETH  Ethereum   | mc:   $284.79B
3. SOL  Solana     | mc:    $48.78B
```

## Metrics

- `current price`: latest USD spot price from CoinGecko
- `24h`: CoinGecko 24-hour percentage change
- `7d return`: percentage return computed from sampled daily prices over the last 7 days
- `30d return`: percentage return computed from sampled daily prices over the last 30 days
- `30d vol (ann.)`: realized volatility computed from daily log returns over the last 30 days and annualized with `sqrt(365)`
- `market cap`: latest USD market capitalization

## Stack

- Java 21
- Maven
- Java `HttpClient`
- Jackson
- JUnit 5
- CoinGecko public API

## Project structure

```text
src/main/java/com/piotrgala/marketsnapshot
|- MarketSnapshotApplication.java
|- client/
|- export/
|- model/
|- service/
|- ui/
`- view/
```

- `client`: HTTP requests and JSON parsing
- `export`: CSV export for saved snapshots
- `model`: API response models and internal snapshot models
- `service`: data orchestration and statistics calculation
- `ui`: desktop controls and table-based Swing presentation
- `view`: terminal output formatting

## Run locally

Requirements:

- Java 21
- Maven 3.9+

Compile:

```bash
mvn compile
```

Run:

```bash
mvn exec:java
```

Run the desktop UI:

```bash
mvn exec:java -Dexec.args="--ui"
```

In PowerShell, prefer:

```powershell
mvn -q exec:java "-Dexec.args=--ui"
```

Run tests:

```bash
mvn test
```

Show help:

```bash
mvn exec:java -Dexec.args="--help"
```

## CoinGecko note

This project uses the public CoinGecko API. On free/public access, `429 Too Many Requests` can happen during repeated local runs.

The service includes basic throttling and retry logic for rate limits, but the public API can still be restrictive.

To make repeated UI refreshes less annoying, the app also keeps a small in-memory cache:

- market snapshots: about `45s`
- price history used for analytics: about `15m`

The UI status bar tells you whether the latest view came from `live`, `cached`, or `mixed` data.
If one asset fails but others succeed, the UI keeps the good rows and shows warning details on hover.

## Export

The desktop UI includes an `Export CSV` button that saves the currently visible snapshot order to a CSV file.
That export includes:

- symbol and name
- current price and returns
- volatility and market cap
- data source (`live`, `cached`, `mixed`)
- snapshot timestamp in UTC

## Why this project

The point of this project is to demonstrate:

- clean, minimal Java project structure
- external API integration
- JSON parsing
- simple market data analytics
- practical CLI output

## MVP scope

Do now:

- live market snapshot for a small fixed asset set
- basic returns and volatility
- clean terminal presentation

Later / optional:

- CLI arguments for custom asset lists
- configurable quote currency
- caching for price history
- export to CSV
- optional momentum ranking
