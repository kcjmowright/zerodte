import { useState, useEffect } from "react";
import Alert from "./Alert.jsx";
import Loading from "./Loading.jsx";
import VerticalComposedChart from "./VerticalComposedChart.jsx";

function GammaExposure() {
    const [quote, setQuote] = useState(null);
    const [gex, setGEX] = useState(null);
    const [expirationDates, setExpirationDates] = useState([]);
    const [selectedSymbol, setSelectedSymbol] = useState(null);
    const [loading, setLoading] = useState(0);
    const [error, setError] = useState(null);

    async function fetchGEXData(symbol, expDates) {
        console.log("fetchGEXData(symbol:" + symbol + ", expDates:" + expDates);
        if (!symbol) {
            return;
        }
        try {
            setLoading(loading + 1);
            const options = "?suppressDetails=true" + expDates.reduce((a, b) => a + "&expDate=" + b, "");
            const response = await fetch("/api/v1/gex/" + symbol + options);
            if (!response.ok) {
                const e = await response.json();
                throw new Error(`${e.message}`);
            }
            const result = await response.json();
            setGEX(result);
        } catch (error) {
            setError(error);
        } finally {
            setLoading(loading - 1);
        }
    }

    async function fetchCurrentQuoteData(symbol) {
        if (!symbol) {
            return;
        }
        try {
            setLoading(loading + 1);
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
            setLoading(loading - 1);
        }
    }

    async function fetchOptionExpirationDates(symbol) {
        if (!symbol) {
            return;
        }
        try {
            setLoading(loading + 1);
            const response = await fetch("/api/v1/gex/expirations/" + symbol);
            if (!response.ok) {
                const e = await response.json();
                throw new Error(`${e.message}`);
            }
            const result = await response.json();
            setExpirationDates(result);
        } catch (error) {
            setError(error);
        } finally {
            setLoading(loading - 1);
        }
    }

    async function submitForm(formData) {
        setError(null);
        let symbol = formData.get("symbol").toUpperCase();
        setSelectedSymbol(symbol);
    }

    async function handleExpirationDates(event) {
        let expDates = Array.from(event.target.selectedOptions).map(
            option => option.value
        );
        setError(null);
        await fetchGEXData(selectedSymbol, expDates);
    }

    useEffect(() => {
        (async () => await fetchCurrentQuoteData(selectedSymbol))();
        (async () => await fetchGEXData(selectedSymbol, expirationDates))();
        (async () => await fetchOptionExpirationDates(selectedSymbol))();
    }, [selectedSymbol]);

    return (
        <>
            <header className="bg-white shadow-sm">
                <div className="grid grid-cols-[70%_30%] gap-4 mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
                    <div>
                        <h1 className="text-3xl font-bold tracking-tight text-gray-900">Gamma Exposure</h1>
                    </div>
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
                </div>
            </header>
            <main>
                <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
                    {
                        (() => {
                            if (loading > 0) {
                                return <Loading />;
                            }
                            if (!(quote && gex)) {
                                return <div></div>;
                            }
                            return <>
                                <div>
                                    <h2 className="text-2xl font-bold tracking-tight text-gray-900">{quote.symbol}</h2>
                                    <dl className="flex gap-6 items-center">
                                        <dt className="font-semibold">Close:</dt>
                                        <dd>{quote.quote.closePrice}</dd>
                                        <dt className="font-semibold">Net Change:</dt>
                                        <dd>{quote.quote.netChange}</dd>
                                        <dt className="font-semibold">Percent Change:</dt>
                                        <dd>{quote.quote.netPercentChange}</dd>
                                        <dt className="font-semibold">Volume:</dt>
                                        <dd>{quote.quote.totalVolume}</dd>
                                    </dl>
                                    <dl className="flex gap-6 items-center">
                                        <dt className="font-semibold">Total Call GEX:</dt>
                                        <dd>{gex.totalCallGEX}</dd>
                                        <dt className="font-semibold">Total Put GEX:</dt>
                                        <dd>{gex.totalPutGEX}</dd>
                                        <dt className="font-semibold">Total GEX:</dt>
                                        <dd>{gex.totalGEX}</dd>
                                        <dt className="font-semibold">Call Wall</dt>
                                        <dd>{gex.callWall}</dd>
                                        <dt className="font-semibold">Put Wall</dt>
                                        <dd>{gex.putWall}</dd>
                                    </dl>
                                </div>
                                <div className="flex gap-4">
                                    <label className="font-semibold">Expiration Dates:</label>
                                    <select
                                        id="expirationDates"
                                        name="expirationDates"
                                        onChange={handleExpirationDates}
                                        multiple
                                        size="1"
                                        className="h-6 border rounded px-2 overflow-auto">
                                        { expirationDates.map((expirationDate) => (
                                            <option key={expirationDate} value={expirationDate}>
                                                {expirationDate}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                                <VerticalComposedChart data={Object.values(gex.gexPerStrike)}/>
                            </>;
                        })()
                    }
                </div>
                {
                    (() => {
                        if (error) {
                            return <Alert message={error.message} />;
                        }
                    })()
                }
            </main>
        </>
    );
}

export default GammaExposure;
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