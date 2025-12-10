import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Cell,
    Tooltip,
} from "recharts";

const colors = [
    "#1f77b4",
    "#ff7f0e",
    "#2ca02c",
    "#d62728",
    "#9467bd",
    "#8c564b",
    "#e377c2",
    "#7f7f7f",
    "#bcbd22",
    "#17becf",
];

const Candlestick = props => {
    const {
        x,
        y,
        width,
        height,
        low,
        high,
        openClose: [open, close],
    } = props;
    const isGrowing = open < close;
    const color = isGrowing ? 'green' : 'red';
    const ratio = Math.abs(height / (open - close));
    const xSize = x + width / 2.0;
    const closeHighV = (close - high) * ratio;
    const closeLowV = (close - low) * ratio;
    const openHighV = (open - high) * ratio;
    const openLowV = (open - low) * ratio;
    // console.log(`
    //     ratio = ${ratio}
    //     xSize = ${xSize}
    //     closeHighV = ${closeHighV}
    //     closeLowV = ${closeLowV}
    //     openHighV = ${openHighV}
    //     openLowV = ${openLowV}
    // `);
    if (isNaN(xSize) || isNaN(closeHighV) || isNaN(closeLowV) || isNaN(openHighV) || isNaN(openLowV)) {
        // console.log(`
        //     ratio = ${ratio}
        //     xSize = ${xSize}
        //     closeHighV = ${closeHighV}
        //     closeLowV = ${closeLowV}
        //     openHighV = ${openHighV}
        //     openLowV = ${openLowV}
        // `);
        //console.log("************ missing value *************************");
        return <></>;
    }
    return (
        <g stroke={color} fill="none" strokeWidth="2">
            <path
                d={`
                  M ${x},${y}
                  L ${x},${y + height}
                  L ${x + width},${y + height}
                  L ${x + width},${y}
                  L ${x},${y}
                `}
            />

            {isGrowing ? (
                <path
                    d={`
                    M ${xSize}, ${y + height}
                    v ${openLowV}
                  `}
                />
            ) : (
                <path
                    d={`
                    M ${xSize}, ${y}
                    v ${closeLowV}
                  `}
                />
            )}
            {/* top line */}
            {isGrowing ? (
                <path
                    d={`
                    M ${xSize}, ${y}
                    v ${closeHighV}
                  `}
                />
            ) : (
                <path
                    d={`
                    M ${xSize}, ${y + height}
                    v ${openHighV}
                  `}
                />
            )}
        </g>
    );
};

const prepareData = data => {
    return data.map((quoteStudy) => {
        const candle = quoteStudy.candle;
        return {
            open: candle.open,
            close: candle.close,
            low: candle.low,
            high: candle.high,
            volume: candle.volume,
            datetime: candle.datetimeISO8601,
            openClose: [candle.open, candle.close],
        };
    });
};

function dateTimeFormatter(value) {
    return value.split("T")[1];
}

const CandleStickChart = ({quoteStudies}) => {
    const data = prepareData(quoteStudies);
    return (
        <BarChart
            style={{ width: "100%", maxHeight: "60vh", aspectRatio: 1 / 1.618 }}
            responsive
            data={data}
        >
            <XAxis dataKey="datetime" angle={-45} tickFormatter={dateTimeFormatter} />
            <YAxis domain={[(min) => min - 5.0, (max) => max + 5.0]} />
            <CartesianGrid strokeDasharray="3 3"/>
            <Tooltip />
            <Bar
                dataKey="openClose"
                fill="#8884d8"
                shape={<Candlestick/>}
                // label={{ position: 'top' }}
            >
                {data.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={colors[index % 20]}/>
                ))}
            </Bar>
        </BarChart>
    );
};

export default CandleStickChart;

/*
            width={800}
            height={600}
            margin={{top: 20, right: 30, left: 20, bottom: 5}}
            style={{ width: "100%", maxHeight: "60vh", aspectRatio: 1 / 1.618 }}
 */