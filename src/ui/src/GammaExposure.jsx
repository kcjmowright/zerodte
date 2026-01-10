import { useState, useEffect } from "react";
import Alert from "./Alert.jsx";
import Loading from "./Loading.jsx";
import GEXChart from "./GEXChart.jsx";
import CandleStickChart from "./CandleStickChart.jsx";
import SmallCheckboxButton from "./SmallCheckboxButton.jsx";
import formatters from "./utils/formatters.js";
import Slider from "./Slider.jsx";

function GammaExposure() {
    const NOW = "Now";
    const [quote, setQuote] = useState(null);
    const [gex, setGEX] = useState(null);
    const [expirationDates, setExpirationDates] = useState([]);
    const [selectedSymbol, setSelectedSymbol] = useState(null);
    const [loading, setLoading] = useState(0);
    const [error, setError] = useState(null);
    const [selectedExpDates, setSelectedExpDates] = useState(getInitialExpDates());
    const [priceHistoryStudies, setPriceHistoryStudies] = useState(null);
    const [chartStartDay, chartEndDay] = getChartDates();
    const [startDate, setStartDate] = useState(chartStartDay);
    const [endDate, setEndDate] = useState(chartEndDay);
    const [showPutGEX, setShowPutGEX] = useState(true);
    const [showPutVolume, setShowPutVolume] = useState(false);
    const [showCallGEX, setShowCallGEX] = useState(true);
    const [showCallVolume, setShowCallVolume] = useState(false);
    const [showAbsoluteGEX, setShowAbsoluteGEX] = useState(false);
    const [showOpenInterest, setShowOpenInterest] = useState(false);
    const [gexHistoryDateTimes, setGexHistoryDateTimes] = useState([]);
    const [gexHistoryDateTimeInput, setGexHistoryDateTimeInput] = useState(null);
    const [gexHistoryDateTime, setGexHistoryDateTime] = useState(null);

    /**
     * Find initial date range for stock chart.
     * @returns {*[]}
     */
    function getChartDates() {
        const dates = [];
        const startDay = new Date();
        const endDay = new Date();
        switch (startDay.getDay()) {
            case 0:
                startDay.setDate(startDay.getDate() - 2);
                endDay.setDate(startDay.getDate());
                break;
            case 1:
                startDay.setDate(startDay.getDate() - 3);
                break;
            case 6:
                startDay.setDate(startDay.getDate() - 1);
                endDay.setDate(startDay.getDate());
                break;
            default:
                startDay.setDate(startDay.getDate() - 1);
        }
        dates.push(startDay.toISOString().split('T')[0]);
        dates.push(endDay.toISOString().split('T')[0]);
        return dates;
    }

    /**
     * Find initial expiration dates.
     * <ul>
     *   <li>The closest trading day.
     *   <li>The following trading day to the closest.
     *   <li>The closest Friday.
     *   <li>The third Friday of this month.
     *   <li>The third Friday of next month.
     * </ul>
     * @returns {*[]} an array of expiration dates.
     */
    function getInitialExpDates() {
        const dates = [];
        const today = new Date();
        const startDay = new Date(today);
        if (startDay.getDay() === 0) { // If Sunday
            startDay.setDate(startDay.getDate() + 1);
        } else if (startDay.getDay() === 6) { // If Saturday
            startDay.setDate(startDay.getDate() + 2);
        }
        dates.push(startDay.toISOString().split('T')[0]);

        const nextDay = new Date(startDay);
        nextDay.setDate(startDay.getDate() + 1);
        dates.push(nextDay.toISOString().split('T')[0]);

        const thisFriday = new Date(startDay);
        const daysUntilFriday = (5 - startDay.getDay() + 7) % 7;
        thisFriday.setDate(startDay.getDate() + (daysUntilFriday || 7));
        dates.push(thisFriday.toISOString().split('T')[0]);

        const thirdFridayThisMonth = getNthFriday(startDay.getFullYear(), startDay.getMonth(), 3);
        if (thirdFridayThisMonth.getTime() > today.getTime()) { // If 3rd Friday this month is not in the past
            dates.push(thirdFridayThisMonth.toISOString().split('T')[0]);
        }

        const nextMonth = new Date(startDay.getFullYear(), startDay.getMonth() + 1, 1);
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

    async function fetchCurrentPriceData(symbol) {
        if (!symbol) {
            return;
        }
        try {
            setLoading(prev => prev + 1);
            const response = await fetch(`/api/v1/price?symbol=${symbol}`);
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

    async function fetchPriceHistoryStudy(symbol, start, end) {
        if (!symbol) {
            return;
        }
        try {
            setLoading(prev => prev + 1);
            const url = `/api/v1/price-history/${symbol}?start=${start}&end=${end}`;
            const response = await fetch(url);
            if (!response.ok) {
                const e = await response.json();
                throw new Error(`${e.message}`);
            }
            const result = await response.json();
            setPriceHistoryStudies(result.priceHistoryStudies);
        } catch (error) {
            setError(error);
        } finally {
            setLoading(prev => prev - 1);
        }
    }

    async function fetchGexHistoryDateTimes(symbol) {
        if (!symbol) {
            return;
        }
        try {
            setGexHistoryDateTimeInput(null);
            setGexHistoryDateTime(null);
            setGexHistoryDateTimes(null);
            const endDate = new Date();
            const end = endDate.toISOString().split("Z")[0]; // Server parser no likey the Z
            const startDate = new Date();
            startDate.setDate(startDate.getDate() - 30);
            const start = startDate.toISOString().split("Z")[0];
            const url = `/api/v1/gex/history/datetimes/${symbol}?start=${start}&end=${end}`;
            const response = await fetch(url);
            if (!response.ok) {
                const e = await response.json();
                throw new Error(`${e.message}`);
            }
            const result = await response.json();
            if (result && result.length) {
                 const dateTimes = result.map(v => {
                    const date = new Date();
                    date.setTime(Date.parse(v));
                    return {
                        label: date.toLocaleString(),
                        value: v
                    };
                });
                const nowValue = {
                    label: NOW,
                    value: NOW
                };
                dateTimes.push(nowValue);
                setGexHistoryDateTime(NOW);
                setGexHistoryDateTimeInput(nowValue);
                setGexHistoryDateTimes(dateTimes);
            }
        } catch (error) {
            console.log(error);
        }
    }

    async function fetchGexHistory(symbol, dateTime) {
        if (!(symbol && dateTime)) {
            return;
        }
        if (dateTime === NOW) {
            return fetchGEXData(symbol, selectedExpDates);
        }
        try {
            const url = `/api/v1/gex/history/${symbol}?dateTime=${dateTime}`;
            const response = await fetch(url);
            if (!response.ok) {
                const e = await response.json();
                throw new Error(`${e.message}`);
            }
            const result = await response.json();
            setGEX(result);
        } catch (error) {
            console.log(error);
        }
    }

    function submitForm(formData) {
        setError(null);
        let symbol = formData.get("symbol").toUpperCase();
        setSelectedSymbol(symbol);
    }

    function handleExpirationDates(event) {
        let expDates = Array.from(event.target.selectedOptions).map(
            option => option.value
        );
        setSelectedExpDates(expDates);
    }

    function onGEXHistoryDateTimesSliderChange(value) {
        setGexHistoryDateTimeInput(value);
    }

    useEffect(() => {
        if (gexHistoryDateTimeInput) {
            const timeout =
                setTimeout(() => setGexHistoryDateTime(gexHistoryDateTimeInput.value), 500);
            return () => clearTimeout(timeout);
        }
    }, [gexHistoryDateTimeInput])

    useEffect(() => {
        if (gexHistoryDateTime) {
            fetchGexHistory(selectedSymbol, gexHistoryDateTime);
        }
    }, [gexHistoryDateTime])

    useEffect(() => {
        (async () => {
            await fetchGEXData(selectedSymbol, selectedExpDates);
            await fetchCurrentPriceData(selectedSymbol);
            await fetchPriceHistoryStudy(selectedSymbol, startDate, endDate);
        })();
    }, [selectedExpDates]);

    useEffect(() => {
        (async () => {
            setGEX(null);
            setQuote(null);
            setSelectedExpDates(getInitialExpDates());
            await fetchOptionExpirationDates(selectedSymbol);
            await fetchGexHistoryDateTimes(selectedSymbol);
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
                            const quoteTime = new Date();
                            quoteTime.setTime(quote.quote.quoteTime);
                            return <>
                                <div>
                                    <h2 className="text-2xl font-bold tracking-tight text-gray-900 mb-4">{quote.reference.description} ({quote.symbol})</h2>
                                    <dl className="flex gap-4 items-center">
                                        <dt className="font-semibold">Last {!quoteTime.getTime() ? "" : `(as of ${quoteTime.toLocaleString()})`}:</dt>
                                        <dd>{formatters.currency.format(quote.quote.lastPrice)}</dd>
                                        <dt className="font-semibold">Close:</dt>
                                        <dd>{formatters.currency.format(quote.quote.closePrice)}</dd>
                                        <dt className="font-semibold">Net Change:</dt>
                                        <dd>{formatters.currency.format(quote.quote.netChange)}</dd>
                                        <dt className="font-semibold">Percent Change:</dt>
                                        <dd>{formatters.percentage.format(quote.quote.netPercentChange / 100.0)}</dd>
                                        <dt className="font-semibold">Volume:</dt>
                                        <dd>{formatters.number.format(quote.quote.totalVolume)}</dd>
                                    </dl>
                                    <div className="flex gap-4">
                                        <label className="font-semibold">Expiration Dates:</label>
                                        <select
                                            disabled={!!gexHistoryDateTimeInput && gexHistoryDateTimeInput.value !== NOW}
                                            id="expirationDates"
                                            name="expirationDates"
                                            value={selectedExpDates}
                                            onChange={handleExpirationDates}
                                            multiple
                                            size="1"
                                            className="
                                                h-6
                                                border
                                                rounded
                                                px-2
                                                overflow-auto
                                                disabled:opacity-60
                                                disabled:bg-gray-100
                                                disabled:border-gray-300
                                                disabled:cursor-not-allowed">
                                            { expirationDates.map((expirationDate) => (
                                                <option key={expirationDate} value={expirationDate}>
                                                    {expirationDate}
                                                </option>
                                              ))
                                            }
                                        </select>
                                        <SmallCheckboxButton
                                            id="showCallGEX"
                                            checked={showCallGEX}
                                            label={showCallGEX ? "Hide Call GEX" : "Show Call GEX"}
                                            onChange={(e) => setShowCallGEX(e.target.checked)} />
                                        <SmallCheckboxButton
                                            id="showPutGEX"
                                            checked={showPutGEX}
                                            label={showPutGEX ? "Hide Put GEX" : "Show Put GEX"}
                                            onChange={(e) => setShowPutGEX(e.target.checked)} />
                                        <SmallCheckboxButton
                                            id="showAbsoluteGEX"
                                            checked={showAbsoluteGEX}
                                            label={showAbsoluteGEX ? "Hide Absolute GEX" : "Show Absolute GEX"}
                                            onChange={(e) => setShowAbsoluteGEX(e.target.checked)} />
                                        <SmallCheckboxButton
                                            id="showOpenInterest"
                                            checked={showOpenInterest}
                                            label={showOpenInterest ? "Hide Open Interest" : "Show Open Interest"}
                                            onChange={(e) => setShowOpenInterest(e.target.checked)} />
                                        <SmallCheckboxButton
                                            id="showCallVolume"
                                            checked={showCallVolume}
                                            label={showCallVolume ? "Hide Call Volume" : "Show Call Volume"}
                                            onChange={(e) => setShowCallVolume(e.target.checked)} />
                                        <SmallCheckboxButton
                                            id="showPutVolume"
                                            checked={showPutVolume}
                                            label={showPutVolume ? "Hide Put Volume" : "Show Put Volume"}
                                            onChange={(e) => setShowPutVolume(e.target.checked)} />
                                    </div>
                                    {
                                        (() => {
                                            if (!gex && loading > 0) {
                                                return <Loading/>;
                                            }
                                            if (!gex) {
                                                return <></>;
                                            }
                                            return <>
                                                <dl className="flex gap-4 items-center">
                                                    <dt className="font-semibold">Total Call GEX:</dt>
                                                    <dd>{formatters.number.format(gex.totalCallGEX)}</dd>
                                                    <dt className="font-semibold">Total Put GEX:</dt>
                                                    <dd>{formatters.number.format(gex.totalPutGEX)}</dd>
                                                    <dt className="font-semibold">Total GEX:</dt>
                                                    <dd>{formatters.number.format(gex.totalGEX)}</dd>
                                                    <dt className="font-semibold">Call Wall</dt>
                                                    <dd>{formatters.number.format(gex.callWall)}</dd>
                                                    <dt className="font-semibold">Put Wall</dt>
                                                    <dd>{formatters.number.format(gex.putWall)}</dd>
                                                    <dt className="font-semibold">Flip Point</dt>
                                                    <dd>{formatters.number.format(gex.flipPoint)}</dd>
                                                </dl>
                                                <Slider data={gexHistoryDateTimes} onValueChange={onGEXHistoryDateTimesSliderChange} />
                                            </>
                                        })()
                                    }
                                </div>
                                { gex && <GEXChart
                                    callWall={gex.callWall}
                                    data={Object.values(gex.gexPerStrike)}
                                    flipPoint={gex.flipPoint}
                                    putWall={gex.putWall}
                                    showAbsoluteGEX={showAbsoluteGEX}
                                    showCallGEX={showCallGEX}
                                    showCallVolume={showCallVolume}
                                    showOpenInterest={showOpenInterest}
                                    showPutGEX={showPutGEX}
                                    showPutVolume={showPutVolume}
                                    spotPrice={gex.spotPrice} />
                                }
                                { priceHistoryStudies && <CandleStickChart quoteStudies={priceHistoryStudies} /> }
                            </>;
                        })()
                    }
                </div>
                {error && <Alert message={error.message} />}
            </main>
        </>
    );
}

export default GammaExposure;