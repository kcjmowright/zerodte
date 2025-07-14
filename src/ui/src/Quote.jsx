import { useState, useEffect } from "react";
import Alert from "./Alert.jsx";
import Loading from "./Loading.jsx";

function Quote() {
    const [quote, setQuote] = useState(null);
    const [selectedSymbol, setSelectedSymbol] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    async function fetchData(symbol) {
        if (!symbol) {
            return;
        }
        try {
            const response = await fetch("/api/v1/quote?symbol=" + symbol);
            if (!response.ok) {
                const e = await response.json();
                throw new Error(`${e.message}`);
            }
            const result = await response.json();
            setQuote(result);
        } catch (error) {
            setError(error);
        } finally {
            setLoading(false);
        }
    }

    async function submitForm(formData) {
        setError(null);
        setLoading(true);
        setSelectedSymbol(formData.get("symbol").toUpperCase());
    }

    useEffect(() => {

        (async () => await fetchData(selectedSymbol))();
    }, [selectedSymbol]);

    return (
        <>
            <header className="bg-white shadow-sm">
                <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
                    <h1 className="text-3xl font-bold tracking-tight text-gray-900">Quote</h1>
                </div>
            </header>
            <main>
                <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
                    <div>
                        <form className="space-y-4" action={submitForm}>
                            <input
                                id="symbol"
                                type="text"
                                name="symbol"
                                placeholder="Enter symbol"
                                required
                                autoFocus
                                className="border border-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"/>
                            <button type="submit" className="text-white bg-gray-400 rounded-md p-1">Get Quote</button>
                        </form>
                    </div>
                    {
                        (() => {
                            if (loading) {
                                return <Loading />;
                            }
                            if (error) {
                                return <Alert message={error.message} />;
                            }
                            if (!quote) {
                                return <div></div>;
                            }
                            return <div>
                                <h1>{quote.symbol}</h1>
                                <dl>
                                    <dt>Close:</dt>
                                    <dd>{quote.quote.closePrice}</dd>

                                    <dt>Net Change:</dt>
                                    <dd>{quote.quote.netChange}</dd>

                                    <dt>Percent Change:</dt>
                                    <dd>{quote.quote.netPercentChange}</dd>

                                    <dt>Volume:</dt>
                                    <dd>{quote.quote.totalVolume}</dd>
                                </dl>
                            </div>;
                        })()
                    }
                </div>
            </main>
        </>
    );
}

export default Quote;
/*
{
    "assetMainType"
:
    "EQUITY",
        "realtime"
:
    true,
        "ssid"
:
    48644470,
        "symbol"
:
    "QQQ",
        "assetSubType"
:
    "ETF",
        "quoteType"
:
    "NBBO",
        "quote"
:
    {
        "closePrice"
    :
        519.93,
            "netChange"
    :
        -1.55,
            "netPercentChange"
    :
        -0.29811705,
            "securityStatus"
    :
        "Normal",
            "totalVolume"
    :
        67662836,
            "tradeTime"
    :
        1748649598851,
            "askMICId"
    :
        "ARCX",
            "askPrice"
    :
        518.49,
            "askSize": 1,
    "askTime": 1748649594452,
    "bidMICId": "ARCX",
    "bidPrice": 518.27,
    "bidSize": 2,
    "bidTime": 1748649594397,
    "highPrice": 520.68,
    "lastMICId": "XADF",
    "lastPrice": 518.38,
    "lastSize": 1,
    "lowPrice": 511.93,
    "mark": 519.11,
    "markChange": -0.82,
    "markPercentChange": -0.15771354,
    "openPrice": 519.44,
    "postMarketChange": -0.73,
    "postMarketPercentChange": -0.1406253,
    "quoteTime": 1748649594452,
    "52WeekHigh": 540.81,
    "52WeekLow": 402.39
  },
  "reference": {
    "cusip": "46090E103",
    "description": "INVESCO QQQ TRUST",
    "exchange": "Q",
    "exchangeName": "NASDAQ",
    "htbRate": 0,
    "isHardToBorrow": false,
    "isShortable": true
  },
  "extended": {
    "askPrice": 0,
    "askSize": 0,
    "bidPrice": 0,
    "bidSize": 0,
    "lastPrice": 518.93,
    "lastSize": 4,
    "mark": 0,
    "quoteTime": 1748592000000,
    "totalVolume": 0,
    "tradeTime": 1748591983000
  },
  "fundamental": {
    "avg10DaysVolume": 51974768,
    "avg1YearVolume": 37737123,
    "declarationDate": "2025-03-21T00:00:00",
    "divAmount": 2.86284,
    "divExDate": "2025-03-24T00:00:00",
    "divFreq": 4,
    "divPayAmount": 0.71571,
    "divPayDate": "2025-04-30T00:00:00",
    "divYield": 0.5517,
    "eps": 126.30331,
    "fundLeverageFactor": 0,
    "nextDivExDate": "2025-06-24T00:00:00",
    "nextDivPayDate": "2025-07-30T00:00:00",
    "peRatio": 4.11652
  },
  "regular": {
    "regularMarketLastPrice": 519.11,
    "regularMarketLastSize": 555567,
    "regularMarketNetChange": -0.82,
    "regularMarketPercentChange": -0.15771354,
    "regularMarketTradeTime": 1748635200324
  }
}
 */