# market-snapshot-tool

Small Java CLI tool that fetches live crypto market data, computes a few simple analytics, and prints a clean terminal snapshot.

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
|- model/
|- service/
`- view/
```

- `client`: HTTP requests and JSON parsing
- `model`: API response models and internal snapshot models
- `service`: data orchestration and statistics calculation
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

Run tests:

```bash
mvn test
```

## CoinGecko note

This project uses the public CoinGecko API. On free/public access, `429 Too Many Requests` can happen during repeated local runs.

The service includes basic throttling and retry logic for rate limits, but the public API can still be restrictive.

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
