import { ComposedChart, Area, Bar, XAxis, YAxis, ReferenceLine, CartesianGrid, Tooltip } from "recharts";
import formatters from "./utils/formatters.js";

const GEXChart = ({data, callWall, putWall, flipPoint, showPutGEX, showCallGEX, showAbsoluteGEX, showOpenInterest}) => {
    const colors = {
        callGEX: "#73d867",
        putGEX: "#d86c91",
        absoluteGEX: "#d8d019",
        openInterest: "#e4b7e4",
        totalGEX: "#413ea0"
    };
    const CustomTooltip = ({ active, payload, label }) => {
        if (active && payload && payload.length) {
            const data = payload[0].payload; // Access the original data object
            return (
                <div style={{ backgroundColor: "white", padding: "10px", border: "1px solid #ccc" }}>
                    <p>Strike: ${label}</p>
                    {showCallGEX && <p style={{ color: colors.callGEX }}>Call GEX: {formatters.number.format(data.callGEX)}</p>}
                    {showPutGEX && <p style={{ color: colors.putGEX }}>Put GEX: {formatters.number.format(data.putGEX)}</p>}
                    {showAbsoluteGEX && <p style={{ color: colors.absoluteGEX }}>Absolute GEX: {formatters.number.format(data.absoluteGEX)}</p>}
                    {showOpenInterest && <p style={{ color: colors.openInterest }}>Open Interest: {formatters.number.format(data.openInterest)}</p>}
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
            }}
        >
            <CartesianGrid stroke="#f5f5f5" />
            <XAxis type="number"  />
            <YAxis dataKey="strike" type="category" scale="band" width="auto" />
            <Tooltip content={<CustomTooltip />}/>
            {showCallGEX && <Area dataKey="callGEX" fill={colors.callGEX} stroke={colors.callGEX} />}
            {showPutGEX && <Area dataKey="putGEX" fill={colors.putGEX} stroke={colors.putGEX} />}
            {showAbsoluteGEX && <Area dataKey="absoluteGEX" fill={colors.absoluteGEX} stroke={colors.absoluteGEX} />}
            {showOpenInterest && <Area dataKey="openInterest" fill={colors.openInterest} stroke={colors.openInterest} />}
            <Bar dataKey="totalGEX" barSize={20} fill="#413ea0" />
            {callWall && <ReferenceLine y={callWall} stroke="#188318" strokeWidth={1} strokeOpacity={0.65} label={{ value: "Call Wall", position: "insideTopRight", fill: "#188318" }} />}
            {putWall && <ReferenceLine y={putWall} stroke="#ff0000" strokeWidth={1} strokeOpacity={0.65} label={{ value: "Put Wall", position: "insideTopRight", fill: "#ff0000" }} />}
            {flipPoint && <ReferenceLine y={flipPoint} stroke="#000000" strokeDasharray="3 3" strokeWidth={1} strokeOpacity={0.65} label={{ value: "Flip Point", position: "insideTopRight", fill: "#000000" }} />}
        </ComposedChart>
    );
};

export default GEXChart;