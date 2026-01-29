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