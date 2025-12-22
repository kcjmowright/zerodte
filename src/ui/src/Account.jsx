import { useState, useEffect } from 'react';
import Loading from "./Loading.jsx";
import Alert from "./Alert.jsx";
import formatters from "./utils/formatters.js";

function Account() {
    const [account, setAccount] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    async function fetchData() {
        try {
            const response = await fetch("/api/v1/account");
            if (!response.ok) {
                const e = await response.json();
                throw new Error(`${e.message}`);
            }
            const result = await response.json();
            setAccount(result.securitiesAccount);
        } catch (error) {
            setError(error);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        (() => fetchData())();
    }, []);

    return (
        <>
            <header className="bg-white shadow-sm">
                <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
                    <h1 className="text-3xl font-bold tracking-tight text-gray-900">Account</h1>
                </div>
            </header>
            <main>
                <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
                    {
                        (() => {
                            if (loading) {
                                return <Loading />;
                            }
                            if (error) {
                                return <Alert message={error.message} />;
                            }
                            if (!account) {
                                return <p>No data to display</p>;
                            }
                            return (
                                <>
                                <dl className="flex gap-4 items-center mb-4">
                                    <dt className="font-semibold">Account Number:</dt>
                                    <dd>{account.accountNumber}</dd>
                                    <dt className="font-semibold">Liquidation Value:</dt>
                                    <dd>{formatters.currency.format(account.currentBalances.liquidationValue)}</dd>
                                    <dt className="font-semibold">Cash Balance:</dt>
                                    <dd>{formatters.currency.format(account.currentBalances.cashBalance)}</dd>
                                    <dt className="font-semibold">Available Funds:</dt>
                                    <dd>{formatters.currency.format(account.currentBalances.availableFunds)}</dd>
                                    <dt className="font-semibold">Buying Power:</dt>
                                    <dd>{formatters.currency.format(account.currentBalances.buyingPower)}</dd>
                                    <dt className="font-semibold">Day Trading Buying Power:</dt>
                                    <dd>{formatters.currency.format(account.currentBalances.dayTradingBuyingPower)}</dd>
                                    <dt className="font-semibold">Equity:</dt>
                                    <dd>{formatters.currency.format(account.currentBalances.equity)}</dd>
                                </dl>

                                <h2 className="text-2xl font-bold tracking-tight text-gray-900 mb-4">Positions</h2>

                                <table
                                    className="w-full border-collapse border border-gray-400 bg-white text-sm dark:border-gray-500 dark:bg-gray-800">
                                    <thead className="bg-gray-50 dark:bg-gray-700">
                                        <tr>
                                            <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">
                                                &nbsp;</th>
                                            <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">
                                                Qty
                                            </th>
                                            <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">
                                                Avg. Price
                                            </th>
                                            <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">
                                                Market Value
                                            </th>
                                            <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">
                                                Today's Profit/Loss
                                            </th>
                                            <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">
                                                Today's Profit/Loss %
                                            </th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                    {
                                        account.positions && account.positions.sort((a,b) => a.instrument.symbol.localeCompare(b.instrument.symbol)).map(position => {
                                            return <tr>
                                                <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{position.instrument.symbol}</td>
                                                <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{(position.shortQuantity > 0) ? -position.shortQuantity : (position.longQuantity > 0) ? position.longQuantity : "N/A"}</td>
                                                <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{formatters.currency.format(position.averagePrice)}</td>
                                                <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{formatters.currency.format(position.marketValue)}</td>
                                                <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{formatters.currency.format(position.currentDayProfitLoss)}</td>
                                                <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{formatters.percentage.format(position.currentDayProfitLossPercentage / 100.0)}</td>
                                            </tr>;
                                        })
                                    }
                                </tbody>
                            </table>
                          </>
                        )
                        })()
                    }
                </div>
            </main>
        </>
    );
}

export default Account;
/*
{
    "securitiesAccount": {
        "type": "MARGIN",
        "accountNumber": "56622352",
        "roundTrips": 0,
        "isDayTrader": false,
        "isClosingOnlyRestricted": false,
        "pfcbFlag": false,
        "positions": [
            {
                "shortQuantity": 0,
                "averagePrice": 18.3966,
                "currentDayProfitLoss": 5.5,
                "currentDayProfitLossPercentage": 0.43,
                "longQuantity": 1,
                "settledLongQuantity": 1,
                "settledShortQuantity": 0,
                "instrument": {
                    "assetType": "OPTION",
                    "cusip": "0QQQ..SV50510000",
                    "symbol": "QQQ   250731P00510000",
                    "description": "INVESCO QQQ TR 07/31/2025 $510 Put",
                    "type": "VANILLA",
                    "putCall": "PUT",
                    "underlyingSymbol": "QQQ"
                },
                "marketValue": 1291.5,
                "longOpenProfitLoss": -548.16,
                "taxLotAverageLongPrice": 18.3966,
                "maintenanceRequirement": 0,
                "previousSessionLongQuantity": 1,
                "averageLongPrice": 18.39,
                "currentDayCost": 0
            },
            {
                "shortQuantity": 1,
                "averagePrice": 8.3934,
                "currentDayProfitLoss": -1.58,
                "currentDayProfitLossPercentage": -0.41,
                "longQuantity": 0,
                "settledLongQuantity": 0,
                "settledShortQuantity": -1,
                "instrument": {
                    "assetType": "OPTION",
                    "cusip": "0QQQ..RK50500000",
                    "symbol": "QQQ   250620P00500000",
                    "description": "INVESCO QQQ TR 06/20/2025 $500 Put",
                    "type": "VANILLA",
                    "putCall": "PUT",
                    "underlyingSymbol": "QQQ"
                },
                "marketValue": -390.5,
                "maintenanceRequirement": 0,
                "currentDayCost": 0
            }
        ],
        "currentBalances": {
            "accruedInterest": 0,
            "cashBalance": 2124.67,
            "cashReceipts": 0,
            "longOptionMarketValue": 1291.5,
            "liquidationValue": 3025.67,
            "longMarketValue": 0,
            "moneyMarketFund": 0,
            "savings": 0,
            "shortMarketValue": 0,
            "pendingDeposits": 0,
            "availableFunds": 2124.67,
            "availableFundsNonMarginableTrade": 2124.67,
            "buyingPower": 5342,
            "buyingPowerNonMarginableTrade": 2124.67,
            "dayTradingBuyingPower": 8498,
            "equity": 2124.67,
            "equityPercentage": 100,
            "longMarginValue": 0,
            "maintenanceCall": 0,
            "maintenanceRequirement": 0,
            "marginBalance": 0,
            "regTCall": 0,
            "shortBalance": 0,
            "shortMarginValue": 0,
            "shortOptionMarketValue": -390.5,
            "sma": 2671,
            "mutualFundValue": 0,
            "bondValue": 0
        },
        "initialBalances": {
            "accruedInterest": 0,
            "availableFundsNonMarginableTrade": 2124.67,
            "bondValue": 8498.68,
            "buyingPower": 5342,
            "cashBalance": 2124.67,
            "cashAvailableForTrading": 0,
            "cashReceipts": 0,
            "dayTradingBuyingPower": 8498,
            "dayTradingBuyingPowerCall": 0,
            "dayTradingEquityCall": 0,
            "equity": 3021.75,
            "equityPercentage": 100,
            "liquidationValue": 3021.75,
            "longMarginValue": 0,
            "longOptionMarketValue": 1286,
            "longStockValue": 0,
            "maintenanceCall": 0,
            "maintenanceRequirement": 0,
            "margin": 2124.67,
            "marginEquity": 2124.67,
            "moneyMarketFund": 0,
            "mutualFundValue": 2124.67,
            "regTCall": 0,
            "shortMarginValue": 0,
            "shortOptionMarketValue": -388.92,
            "shortStockValue": -388.92,
            "totalCash": 0,
            "isInCall": false,
            "pendingDeposits": 0,
            "marginBalance": 0,
            "shortBalance": 0,
            "accountValue": 3021.75
        },
        "projectedBalances": {
            "availableFunds": 2124.67,
            "availableFundsNonMarginableTrade": 2124.67,
            "buyingPower": 5342,
            "dayTradingBuyingPower": 8498,
            "dayTradingBuyingPowerCall": 0,
            "isInCall": false,
            "maintenanceCall": 0,
            "regTCall": 0,
            "stockBuyingPower": 5342
        }
    }
}
 */