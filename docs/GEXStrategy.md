# GEX Strategy

## Data Capture

Capture `TotalGEX` every 15 minutes during the trading day from 8:30 AM CST to 3:00 PM CST inclusive.


## Analyze

At 8:45 AM CST, analyze GEX data.
Identify support and resistance levels based on GEX.  
Support is the put wall and the highest absolute GEX below the current strike price.
Resistance is the call wall and the highest absolute GEX above the current strike price.

## Trade

### Scenario 1

Price opens in positive gamma territory
Positive GEX significantly exceeds negative GEXd