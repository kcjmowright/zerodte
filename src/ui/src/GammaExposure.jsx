import { useState, useEffect } from "react";
import Alert from "./Alert.jsx";
import Loading from "./Loading.jsx";
import GEXChart from "./GEXChart.jsx";

function GammaExposure() {
    const [quote, setQuote] = useState(null);
    const [gex, setGEX] = useState(null);
    const [expirationDates, setExpirationDates] = useState([]);
    const [selectedSymbol, setSelectedSymbol] = useState(null);
    const [loading, setLoading] = useState(0);
    const [error, setError] = useState(null);

    async function fetchGEXData(symbol, expDates) {
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
            const response = await fetch(`/api/v1/gex/expirations/${encodeURIComponent(symbol)}`);
            if (!response.ok) {
                const e = await response.json();
                throw new Error(`${e.message}`);
            }
            const result = await response.json();
            setExpirationDates(result);
            setGEX(null);
            const select = document.getElementById("expirationDates");
            if (select) {
                select.selectedIndex = -1;
            }
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
                            if (!quote) {
                                return <div></div>;
                            }
                            return <>
                                <div>
                                    <h2 className="text-2xl font-bold tracking-tight text-gray-900 mb-4">{quote.reference.description} ({quote.symbol})</h2>
                                    <dl className="flex gap-4 items-center">
                                        <dt className="font-semibold">Close:</dt>
                                        <dd>{quote.quote.closePrice}</dd>
                                        <dt className="font-semibold">Net Change:</dt>
                                        <dd>{quote.quote.netChange}</dd>
                                        <dt className="font-semibold">Percent Change:</dt>
                                        <dd>{quote.quote.netPercentChange}</dd>
                                        <dt className="font-semibold">Volume:</dt>
                                        <dd>{quote.quote.totalVolume}</dd>
                                    </dl>
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
                                    {
                                        (() => {
                                            if (!gex) {
                                                return <></>;
                                            }
                                            return <dl className="flex gap-4 items-center">
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
                                                <dt className="font-semibold">Flip Point</dt>
                                                <dd>{gex.flipPoint}</dd>
                                            </dl>
                                        })()
                                    }
                                </div>
                                {
                                    (() => {
                                        if (!gex) {
                                            return <></>;
                                        }
                                        return <GEXChart data={Object.values(gex.gexPerStrike)} callWall={gex.callWall} putWall={gex.putWall} flipPoint={gex.flipPoint} />
                                    })()
                                }
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