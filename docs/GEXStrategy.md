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

#### Scenario 1 Description

Price opens in positive gamma territory, above the flip point on the positive side.
Positive GEX significantly exceeds negative GEXd
Below the spot price is a column of prominent positive GEX and a surge of absolute GEX
Since this is below the current price and the absolute GEX zone has a 90% probability 
that price will not fall elow this level, its identified as a level of support.
Maximum positive GEX is above the current spot price and is identified as a prominent
level of resistence.

#### Scenario 1 Action

Wait until 8:45. if price is moving toward support, wait until it reaches the suppoort level
and take a bullish put credit spread.  If the price is at resistence, take an iron condor credit spread
with the outer inner strike prices at  the support and resistence levels and the outer strikes 
5 points away from the inner strikes.

Close the position 10 minutes before close or once it reaches the resistence level.

### Scenario 2

The day starts with the spot price at absolute GEX and the call wall.  The spot price is surrounded by positive GEX. 
GEX is in a declining slope at strikes above the spot price but there is a surge in GEX at a higher strike which is
likely a resistence point for the day.

Action