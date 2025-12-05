import { ComposedChart, Area, Bar, XAxis, YAxis, ReferenceLine, CartesianGrid, Tooltip, Legend } from "recharts";

const GEXChart = ({data, callWall, putWall, flipPoint}) => {
    return (
        <ComposedChart
            layout="vertical"
            style={{ width: "100%", maxHeight: "60vh", aspectRatio: 1 / 1.618 }}
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
            <XAxis type="number" />
            <YAxis dataKey="strike" type="category" scale="band" width="auto" />
            <Tooltip />
            <Legend />
            <Area dataKey="callGEX" fill="#73d867" stroke="#73d867"/>
            <Area dataKey="putGEX" fill="#d86c91" stroke="#d86c91"/>
            <Bar dataKey="totalGEX" barSize={20} fill="#413ea0" />
            {callWall && <ReferenceLine y={callWall} stroke="#188318" strokeWidth={1} strokeOpacity={0.65} label={{ value: 'Call Wall', position: 'insideTopRight', fill: '#188318' }} />}
            {putWall && <ReferenceLine y={putWall} stroke="#ff0000" strokeWidth={1} strokeOpacity={0.65} label={{ value: 'Put Wall', position: 'insideTopRight', fill: '#ff0000' }} />}
            {flipPoint && <ReferenceLine y={flipPoint} stroke="#000000" strokeDasharray="3 3" strokeWidth={1} strokeOpacity={0.65} label={{ value: 'Flip Point', position: 'insideTopRight', fill: '#000000' }} />}
        </ComposedChart>
    );
};

export default GEXChart;