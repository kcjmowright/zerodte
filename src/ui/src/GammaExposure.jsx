import { useState, useEffect } from "react";
import Alert from "./Alert.jsx";
import Loading from "./Loading.jsx";
import GEXChart from "./GEXChart.jsx";
import CandleStickChart from "./CandleStickChart.jsx";

function GammaExposure() {
    const [quote, setQuote] = useState(null);
    const [gex, setGEX] = useState(null);
    const [expirationDates, setExpirationDates] = useState([]);
    const [selectedSymbol, setSelectedSymbol] = useState(null);
    const [loading, setLoading] = useState(0);
    const [error, setError] = useState(null);
    const [selectedExpDates, setSelectedExpDates] = useState(getInitialExpDates());
    const [quoteStudies, setQuoteStudies] = useState(null);

    function getInitialExpDates() {
        const today = new Date();
        const dates = [];

        // Today's date
        dates.push(today.toISOString().split('T')[0]);

        // Tomorrow's date
        const tomorrow = new Date(today);
        tomorrow.setDate(today.getDate() + 1);
        dates.push(tomorrow.toISOString().split('T')[0]);

        // This Friday
        const thisFriday = new Date(today);
        const daysUntilFriday = (5 - today.getDay() + 7) % 7;
        thisFriday.setDate(today.getDate() + (daysUntilFriday || 7));
        dates.push(thisFriday.toISOString().split('T')[0]);

        // 3rd Friday of this month
        const thirdFridayThisMonth = getNthFriday(today.getFullYear(), today.getMonth(), 3);
        dates.push(thirdFridayThisMonth.toISOString().split('T')[0]);

        // 3rd Friday of next month
        const nextMonth = new Date(today.getFullYear(), today.getMonth() + 1, 1);
        const thirdFridayNextMonth = getNthFriday(nextMonth.getFullYear(), nextMonth.getMonth(), 3);
        dates.push(thirdFridayNextMonth.toISOString().split('T')[0]);

        return dates;
    }

    function getNthFriday(year, month, n) {
        const firstDay = new Date(year, month, 1);
        const firstFriday = new Date(firstDay);
        // Calculate days until first Friday
        const daysUntilFriday = (5 - firstDay.getDay() + 7) % 7;
        firstFriday.setDate(1 + daysUntilFriday);
        // Add weeks to get to the nth Friday
        firstFriday.setDate(firstFriday.getDate() + (n - 1) * 7);

        return firstFriday;
    }

    async function fetchGEXData(symbol, expDates) {
        if (!symbol) {
            return;
        }
        try {
            setLoading(prev => prev + 1);
            const options = "suppressDetails=true" + expDates.reduce((a, b) => a + "&expDate=" + b, "");
            const response = await fetch(`/api/v1/gex/${symbol}?${options}`);
            if (!response.ok) {
                const e = await response.json();
                throw new Error(`${e.message}`);
            }
            const result = await response.json();
            setGEX(result);
        } catch (error) {
            setError(error);
        } finally {
            setLoading(prev => prev - 1);
        }
    }

    async function fetchCurrentQuoteData(symbol) {
        if (!symbol) {
            return;
        }
        try {
            setLoading(prev => prev + 1);
            const response = await fetch(`/api/v1/quote?symbol=${symbol}`);
            if (!response.ok) {
                const e = await response.json();
                throw new Error(`${e.message}`);
            }
            const result = await response.json();
            setQuote(result);
        } catch (error) {
            setError(error);
        } finally {
            setLoading(prev => prev - 1);
        }
    }

    async function fetchOptionExpirationDates(symbol) {
        if (!symbol) {
            return;
        }
        try {
            setLoading(prev => prev + 1);
            const response = await fetch(`/api/v1/gex/expirations/${symbol}`);
            if (!response.ok) {
                const e = await response.json();
                throw new Error(`${e.message}`);
            }
            const result = await response.json();
            setExpirationDates(result);
        } catch (error) {
            setError(error);
        } finally {
            setLoading(prev => prev - 1);
        }
    }

    async function fetchQuoteStudies(symbol, startDate, endDate) {
        if (!symbol) {
            return;
        }
        try {
            setLoading(prev => prev + 1);
            const url = `/api/v1/price-history/${symbol}?startDate=${startDate}&endDate=${endDate}`;
            const response = await fetch(url);
            if (!response.ok) {
                const e = await response.json();
                throw new Error(`${e.message}`);
            }
            const result = await response.json();
            setQuoteStudies(result.quoteStudies);
        } catch (error) {
            setError(error);
        } finally {
            setLoading(prev => prev - 1);
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
        setSelectedExpDates(expDates);
    }

    useEffect(() => {
        (async () => {
            await fetchGEXData(selectedSymbol, selectedExpDates);
            await fetchCurrentQuoteData(selectedSymbol);
        })();
    }, [selectedExpDates]);

    useEffect(() => {
        (async () => {
            setGEX(null);
            setLoading(0);
            await fetchCurrentQuoteData(selectedSymbol)
            await fetchOptionExpirationDates(selectedSymbol);
            await fetchGEXData(selectedSymbol, selectedExpDates);
            const today = new Date();
            const oneWeekAgo = new Date(today);
            oneWeekAgo.setDate(today.getDate() - 7);
            const startDate = oneWeekAgo.toISOString().split("T")[0];
            const endDate = today.toISOString().split("T")[0]
            await fetchQuoteStudies(selectedSymbol, startDate, endDate);
        })();
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
                            if (!quote && loading > 0) {
                                return <Loading />;
                            }
                            if (!quote) {
                                return <div></div>;
                            }
                            return <>
                                <div>
                                    <h2 className="text-2xl font-bold tracking-tight text-gray-900 mb-4">{quote.reference.description} ({quote.symbol})</h2>
                                    <dl className="flex gap-4 items-center">
                                        <dt className="font-semibold">Last:</dt>
                                        <dd>{quote.quote.lastPrice}</dd>
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
                                            value={selectedExpDates}
                                            onChange={handleExpirationDates}
                                            multiple
                                            size="1"
                                            className="h-6 border rounded px-2 overflow-auto">
                                            { expirationDates.map((expirationDate) => (
                                                <option key={expirationDate} value={expirationDate}>
                                                    {expirationDate}
                                                </option>
                                              ))
                                            }
                                        </select>
                                    </div>
                                    {
                                        (() => {
                                            if (!gex && loading > 0) {
                                                return <Loading />;
                                            }
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
                                        return <div className="grid grid-cols-[70%_30%] items-start">
                                            { quoteStudies && <CandleStickChart quoteStudies={quoteStudies} /> }
                                            <GEXChart data={Object.values(gex.gexPerStrike)} callWall={gex.callWall} putWall={gex.putWall} flipPoint={gex.flipPoint} />
                                        </div>
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