import { useState, useEffect } from "react";
import Loading from "./Loading.jsx";
import Alert from "./Alert.jsx";

function ZeroDteIronCondor({inSymbol}) {
    const [ironCondor, setIronCondor] = useState(null);
    const [selectedSymbol, setSelectedSymbol] = useState(inSymbol);
    const [loading, setLoading] = useState(!!inSymbol);
    const [error, setError] = useState(null);

    async function fetchData(symbol) {
        if (!symbol) {
            return;
        }
        try {
            const response = await fetch("/api/v1/option/zero-dte-iron-condor?symbol=" + symbol);
            if (!response.ok) {
                const e = await response.json();
                throw new Error(`${e.message}`);
            }
            const result = await response.json();
            setIronCondor(result);
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
                    <button type="submit" className="text-white bg-gray-400 rounded-md p-1">Get Iron Condor</button>
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
                    if (!ironCondor) {
                        return <div></div>;
                    }
                    return <div>
                        <h1>Today's Zero DTE Iron Condor for {selectedSymbol}</h1>
                        <table
                            className="w-full border-collapse border border-gray-400 bg-white text-sm dark:border-gray-500 dark:bg-gray-800">
                            <thead className="bg-gray-50 dark:bg-gray-700">
                            <tr>
                                <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">
                                    Delta
                                </th>
                                <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">
                                    Gamma
                                </th>
                                <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">
                                    Theta
                                </th>
                                <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">
                                    Volatility
                                </th>
                                <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">
                                    Strike Price
                                </th>
                                <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">
                                    Bid
                                </th>
                                <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">
                                    Ask
                                </th>
                                <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">
                                    Mark
                                </th>
                                <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">
                                    Volume
                                </th>
                                <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">
                                    Open Interest
                                </th>
                            </tr>
                            </thead>
                            <tbody>
                            {
                                [ironCondor.longPut, ironCondor.shortPut, ironCondor.shortCall, ironCondor.longCall].map((option, i) => {
                                    return (
                                        <>
                                            <tr>
                                                <td className="border bg-gray-50 dark:bg-gray-700 border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400"
                                                    colSpan="10">
                                                    <span
                                                        className="font-bold text-lg">{(i === 0 || i === 3) ? "Long" : "Short"} {option.description}</span> {option.symbol}
                                                </td>
                                            </tr>
                                            <tr>
                                                <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{option.delta}</td>
                                                <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{option.gamma}</td>
                                                <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{option.theta}</td>
                                                <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{option.volatility}</td>
                                                <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{option.strikePrice}</td>
                                                <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{option.bidPrice}</td>
                                                <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{option.askPrice}</td>
                                                <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{option.markPrice}</td>
                                                <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{option.totalVolume}</td>
                                                <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{option.openInterest}</td>
                                            </tr>
                                        </>
                                    );
                                })
                            }
                                <tr>
                                    <td colSpan="7"
                                        className="border bg-gray-50 dark:bg-gray-700 border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">
                                        Credit
                                    </td>
                                    <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">
                                        {-ironCondor.longPut.markPrice + ironCondor.shortPut.markPrice + ironCondor.shortCall.markPrice - ironCondor.longCall.markPrice}
                                    </td>
                                    <td colSpan="2"
                                        className="border bg-gray-50 dark:bg-gray-700 border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">
                                        &nbsp;
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>;
                })()
            }
        </div>
    );
}

export default ZeroDteIronCondor;
/*
{
  "longCall": {
    "putCall": "CALL",
    "symbol": "QQQ   250610C00535000",
    "description": "QQQ 06/10/2025 535.00 C",
    "exchangeName": "OPR",
    "bidPrice": 0.02,
    "askPrice": 0.03,
    "lastPrice": 0.03,
    "bidAskSize": "601X295",
    "markPrice": 0.03,
    "bidSize": 601,
    "askSize": 295,
    "lastSize": 39,
    "highPrice": 0.68,
    "lowPrice": 0.01,
    "openPrice": 0,
    "closePrice": 0.52,
    "totalVolume": 136012,
    "ironCondorTimeInLong": 1749586499742,
    "tradeTimeInLong": 1749586498727,
    "netChange": -0.48,
    "volatility": 7.79,
    "delta": 0.087,
    "gamma": 0.253,
    "theta": -0.025,
    "vega": 0.013,
    "rho": 0,
    "timeValue": 0.03,
    "openInterest": 6384,
    "isInTheMoney": false,
    "theoreticalOptionValue": 0.025,
    "theoreticalVolatility": 29,
    "isMini": false,
    "isNonStandard": false,
    "optionDeliverablesList": [
      {
        "symbol": "QQQ",
        "deliverableUnits": 100,
        "assetType": "STOCK"
      }
    ],
    "strikePrice": 535,
    "expirationDate": "2025-06-10T20:00:00",
    "expirationType": "W",
    "multiplier": 100,
    "settlementType": "P",
    "deliverableNote": "100 QQQ",
    "percentChange": -93.2,
    "markChange": -0.49,
    "markPercentChange": -95.15
  },
  "shortCall": {
    "putCall": "CALL",
    "symbol": "QQQ   250610C00535000",
    "description": "QQQ 06/10/2025 535.00 C",
    "exchangeName": "OPR",
    "bidPrice": 0.02,
    "askPrice": 0.03,
    "lastPrice": 0.03,
    "bidAskSize": "601X295",
    "markPrice": 0.03,
    "bidSize": 601,
    "askSize": 295,
    "lastSize": 39,
    "highPrice": 0.68,
    "lowPrice": 0.01,
    "openPrice": 0,
    "closePrice": 0.52,
    "totalVolume": 136012,
    "ironCondorTimeInLong": 1749586499742,
    "tradeTimeInLong": 1749586498727,
    "netChange": -0.48,
    "volatility": 7.79,
    "delta": 0.087,
    "gamma": 0.253,
    "theta": -0.025,
    "vega": 0.013,
    "rho": 0,
    "timeValue": 0.03,
    "openInterest": 6384,
    "isInTheMoney": false,
    "theoreticalOptionValue": 0.025,
    "theoreticalVolatility": 29,
    "isMini": false,
    "isNonStandard": false,
    "optionDeliverablesList": [
      {
        "symbol": "QQQ",
        "deliverableUnits": 100,
        "assetType": "STOCK"
      }
    ],
    "strikePrice": 535,
    "expirationDate": "2025-06-10T20:00:00",
    "expirationType": "W",
    "multiplier": 100,
    "settlementType": "P",
    "deliverableNote": "100 QQQ",
    "percentChange": -93.2,
    "markChange": -0.49,
    "markPercentChange": -95.15
  },
  "longPut": {
    "putCall": "PUT",
    "symbol": "QQQ   250610P00533000",
    "description": "QQQ 06/10/2025 533.00 P",
    "exchangeName": "OPR",
    "bidPrice": 0.04,
    "askPrice": 0.05,
    "lastPrice": 0.05,
    "bidAskSize": "25X331",
    "markPrice": 0.05,
    "bidSize": 25,
    "askSize": 331,
    "lastSize": 3,
    "highPrice": 4.26,
    "lowPrice": 0.02,
    "openPrice": 0,
    "closePrice": 3.27,
    "totalVolume": 100916,
    "ironCondorTimeInLong": 1749586499570,
    "tradeTimeInLong": 1749586490958,
    "netChange": -3.22,
    "volatility": 11.275,
    "delta": -0.104,
    "gamma": 0.198,
    "theta": -0.045,
    "vega": 0.015,
    "rho": 0,
    "timeValue": 0.05,
    "openInterest": 922,
    "isInTheMoney": false,
    "theoreticalOptionValue": 0.045,
    "theoreticalVolatility": 29,
    "isMini": false,
    "isNonStandard": false,
    "optionDeliverablesList": [
      {
        "symbol": "QQQ",
        "deliverableUnits": 100,
        "assetType": "STOCK"
      }
    ],
    "strikePrice": 533,
    "expirationDate": "2025-06-10T20:00:00",
    "expirationType": "W",
    "multiplier": 100,
    "settlementType": "P",
    "deliverableNote": "100 QQQ",
    "percentChange": -98.61,
    "markChange": -3.22,
    "markPercentChange": -98.62
  },
  "shortPut": {
    "putCall": "PUT",
    "symbol": "QQQ   250610P00533000",
    "description": "QQQ 06/10/2025 533.00 P",
    "exchangeName": "OPR",
    "bidPrice": 0.04,
    "askPrice": 0.05,
    "lastPrice": 0.05,
    "bidAskSize": "25X331",
    "markPrice": 0.05,
    "bidSize": 25,
    "askSize": 331,
    "lastSize": 3,
    "highPrice": 4.26,
    "lowPrice": 0.02,
    "openPrice": 0,
    "closePrice": 3.27,
    "totalVolume": 100916,
    "ironCondorTimeInLong": 1749586499570,
    "tradeTimeInLong": 1749586490958,
    "netChange": -3.22,
    "volatility": 11.275,
    "delta": -0.104,
    "gamma": 0.198,
    "theta": -0.045,
    "vega": 0.015,
    "rho": 0,
    "timeValue": 0.05,
    "openInterest": 922,
    "isInTheMoney": false,
    "theoreticalOptionValue": 0.045,
    "theoreticalVolatility": 29,
    "isMini": false,
    "isNonStandard": false,
    "optionDeliverablesList": [
      {
        "symbol": "QQQ",
        "deliverableUnits": 100,
        "assetType": "STOCK"
      }
    ],
    "strikePrice": 533,
    "expirationDate": "2025-06-10T20:00:00",
    "expirationType": "W",
    "multiplier": 100,
    "settlementType": "P",
    "deliverableNote": "100 QQQ",
    "percentChange": -98.61,
    "markChange": -3.22,
    "markPercentChange": -98.62
  }
}
 */