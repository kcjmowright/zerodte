import { useState, useEffect } from "react";
import Loading from "./Loading.jsx";
import Alert from "./Alert.jsx";

function Movers() {
    const moverSymbols = [
        "$DJI",
        "$COMPX",
        "$SPX",
        "NYSE",
        "NASDAQ",
        "OTCBB",
        "INDEX_ALL",
        "EQUITY_ALL",
        "OPTION_ALL",
        "OPTION_PUT",
        "OPTION_CALL"
    ];
    const moverOptions = moverSymbols.map((symbol, i) => ({
        id: i,
        symbol: symbol
    }));
    const [movers, setMovers] = useState(null);
    const [selectedMover, setSelectedMover] = useState("$DJI")
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);


    async function handleChange(event) {
        setSelectedMover(event.target.value);
        setError(null);
        setLoading(true);
    }

    async function fetchData(mover) {
        try {
            const response = await fetch("/api/v1/movers/" + mover);
            if (!response.ok) {
                const e = await response.json();
                throw new Error(`${e.message}`);
            }
            const result = await response.json();
            setMovers(result);
        } catch (error) {
            setError(error);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        (async () => await fetchData(selectedMover))();
    }, [selectedMover]);

    return (
        <>
            <header className="bg-white shadow-sm">
                <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
                    <h1 className="text-3xl font-bold tracking-tight text-gray-900">Movers</h1>
                </div>
            </header>
            <main>
                <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
                    <div>
                        <select
                            id="mover"
                            name="mover"
                            value={selectedMover}
                            onChange={handleChange}
                            required
                            className="border border-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500">
                            { moverOptions.map((moverOption) => (
                                <option key={moverOption.id} value={moverOption.symbol}>
                                    {moverOption.symbol}
                                </option>
                            ))}
                        </select>
                    </div>
                    {
                        (() => {
                            if (loading) {
                                return <Loading />;
                            }
                            if (error) {
                                return <Alert message={error.message} />;
                            }
                            return <div>
                                <table className="w-full border-collapse border border-gray-400 bg-white text-sm dark:border-gray-500 dark:bg-gray-800">
                                    <thead className="bg-gray-50 dark:bg-gray-700">
                                    <tr>
                                        <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">Last Price</th>
                                        <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">Net Change</th>
                                        <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">Net % Change</th>
                                        <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">Total Volume</th>
                                        <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">Volume</th>
                                        <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">Market Share</th>
                                        <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">Trades</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {
                                        movers.sort((a, b) => b.netChange - a.netChange).map(mover => {
                                            return <>
                                                <tr>
                                                    <td className="border bg-gray-50 dark:bg-gray-700 border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400" colSpan="7">
                                                        <span className="font-bold text-lg">{mover.description}</span>( {mover.symbol} )
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{mover.lastPrice}</td>
                                                    <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{mover.netChange}</td>
                                                    <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{mover.netPercentChange}</td>
                                                    <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{mover.totalVolume}</td>
                                                    <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{mover.volume}</td>
                                                    <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{mover.marketShare}</td>
                                                    <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">{mover.trades}</td>
                                                </tr>
                                            </>
                                        })
                                    }
                                    </tbody>
                                </table>

                            </div>;
                        })()
                    }
                </div>
            </main>
        </>
    );
}

export default Movers;
