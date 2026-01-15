import { useState, useEffect } from "react";
import Alert from "./Alert.jsx";
import CustomTable from "./CustomTable.jsx";
import Loading from "./Loading.jsx";
import formatters from "./utils/formatters.js";

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
                <div className="grid grid-cols-[70%_30%] gap-4 mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
                    <div>
                        <h1 className="text-3xl font-bold tracking-tight text-gray-900">Movers</h1>
                    </div>
                    <div>
                        <select
                            id="mover"
                            name="mover"
                            value={selectedMover}
                            onChange={handleChange}
                            required
                            className="h-6 border rounded px-2 overflow-auto">
                            { moverOptions.map((moverOption) => (
                                <option key={moverOption.id} value={moverOption.symbol}>
                                    {moverOption.symbol}
                                </option>
                            ))}
                        </select>
                    </div>
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
                            const headers = [
                                "Last Price",
                                "Net Change",
                                "Net % Change",
                                "Total Volume",
                                "Volume",
                                "Market Share",
                                "Trades"
                            ];
                            const rows = movers.sort((a, b) => b.netChange - a.netChange).map(row => [
                                row.lastPrice,
                                row.netChange,
                                row.netPercentChange / 100.0,
                                row.totalVolume,
                                row.volume,
                                row.marketShare,
                                row.trades
                            ]);
                            const cellFormatters = [
                                (cell) => formatters.currency.format(cell),
                                (cell) => formatters.currency.format(cell),
                                (cell) => formatters.percentage.format(cell),
                                (cell) => formatters.number.format(cell),
                                (cell) => formatters.number.format(cell),
                                (cell) => cell,
                                (cell) => cell,
                            ];
                            return <CustomTable headers={headers} rowData={rows} cellFormatters={cellFormatters} />;
                        })()
                    }
                </div>
            </main>
        </>
    );
}

export default Movers;
