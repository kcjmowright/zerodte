import { ComposedChart, Area, Bar, Cell, XAxis, YAxis, ReferenceLine, CartesianGrid, Tooltip } from "recharts";
import formatters from "./utils/formatters.js";

const colors = {
    absoluteGEX: "#d8d019",
    callGEX: "#73d867",
    callVolume: "#536e83",
    callWall: "#188318",
    flipPoint: "#000000",
    openInterest: "#e4b7e4",
    putGEX: "#d86c91",
    putVolume: "#b36119",
    putWall: "#ff0000",
    spotPrice: "#e36235ff",
    totalGEXPositive: "#413ea0",
    totalGEXNegative: "#ff0000"
};

const GEXChart = ({data, spotPrice, callWall, putWall, flipPoint, showPutGEX, showCallGEX, showAbsoluteGEX, showOpenInterest, showCallVolume, showPutVolume}) => {
    const adjustedSpotPrice = !(data && data.length) ? 0.0 : data.reduce((closest, current) => {
        return Math.abs(current.strike - spotPrice) < Math.abs(closest.strike - spotPrice)
            ? current
            : closest;
    }).strike;

    const CustomTooltip = ({ active, payload, label }) => {
        if (active && payload && payload.length) {
            const data = payload[0].payload; // Access the original data object
            return (
                <div style={{ backgroundColor: "white", padding: "10px", border: "1px solid #ccc" }}>
                    <p>Strike: {formatters.currency.format(label)}</p>
                    {showCallGEX && <p style={{ color: colors.callGEX }}>Call GEX: {formatters.number.format(data.callGEX)}</p>}
                    {showPutGEX && <p style={{ color: colors.putGEX }}>Put GEX: {formatters.number.format(data.putGEX)}</p>}
                    {showAbsoluteGEX && <p style={{ color: colors.absoluteGEX }}>Absolute GEX: {formatters.number.format(data.absoluteGEX)}</p>}
                    {showOpenInterest && <p style={{ color: colors.openInterest }}>Open Interest: {formatters.number.format(data.openInterest)}</p>}
                    {showCallVolume && <p style={{ color: colors.callVolume }}>Call Volume: {formatters.number.format(data.callVolume)}</p>}
                    {showPutVolume && <p style={{ color: colors.putVolume }}>Open Interest: {formatters.number.format(data.putVolume)}</p>}
                    <p style={{ color: colors.totalGEX }}>Total GEX: {formatters.number.format(data.totalGEX)}</p>
                </div>
            );
        }
        return null;
    };

    return (
        <ComposedChart
            layout="vertical"
            style={{ width: "100%", maxHeight: "80vh", aspectRatio: 1 / 1.618 }}
            responsive
            data={data}
            margin={{
                top: 20,
                right: 0,
                bottom: 0,
                left: 0,
            }}>
            <CartesianGrid stroke="#f5f5f5" />
            <XAxis type="number" xAxisId="gexAxis" />
            <XAxis type="number" xAxisId="otherAxis" />
            <YAxis dataKey="strike" type="category" scale="band" width="auto" />
            <Tooltip content={<CustomTooltip />}/>
            {showCallGEX && <Area xAxisId="gexAxis" dataKey="callGEX" fill={colors.callGEX} stroke={colors.callGEX} />}
            {showPutGEX && <Area xAxisId="gexAxis" dataKey="putGEX" fill={colors.putGEX} stroke={colors.putGEX} />}
            {showAbsoluteGEX && <Area xAxisId="otherAxis" dataKey="absoluteGEX" fill={colors.absoluteGEX} stroke={colors.absoluteGEX} />}
            {showOpenInterest && <Area xAxisId="otherAxis" dataKey="openInterest" fill={colors.openInterest} stroke={colors.openInterest} />}
            {showCallVolume && <Area xAxisId="otherAxis" dataKey="callVolume" fill={colors.callVolume} stroke={colors.callVolume} />}
            {showPutVolume && <Area xAxisId="otherAxis" dataKey="putVolume" fill={colors.putVolume} stroke={colors.putVolume} />}
            <Bar xAxisId="gexAxis" dataKey="totalGEX" barSize={20}>
                {data.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.totalGEX < 0 ? colors.totalGEXNegative : colors.totalGEXPositive} />
                ))}
            </Bar>
            {adjustedSpotPrice && <ReferenceLine y={adjustedSpotPrice} strokeDasharray="3 3" stroke={colors.spotPrice} strokeWidth={1} strokeOpacity={0.65} label={{ value: `Spot: ${formatters.currency.format(spotPrice)}`, position: "insideRight", fill: colors.spotPrice }} />}
            {callWall && <ReferenceLine y={callWall} stroke={colors.callWall} strokeDasharray="3 3" strokeWidth={1} strokeOpacity={0.65} label={{ value: `CW: ${formatters.currency.format(callWall)}`, position: "insideRight", fill: colors.callWall }} />}
            {putWall && <ReferenceLine y={putWall} stroke={colors.putWall} strokeDasharray="3 3" strokeWidth={1} strokeOpacity={0.65} label={{ value: `PW: ${formatters.currency.format(putWall)}`, position: "insideRight", fill: colors.putWall }} />}
            {flipPoint && <ReferenceLine y={flipPoint} stroke={colors.flipPoint} strokeDasharray="3 3" strokeWidth={1} strokeOpacity={0.65} label={{ value: `FP: ${formatters.currency.format(flipPoint)}`, position: "insideRight", fill: colors.flipPoint }} />}
        </ComposedChart>
    );
};

export default GEXChart;