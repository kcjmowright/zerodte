import { ComposedChart, Area, Bar, XAxis, YAxis, ReferenceLine, CartesianGrid, Tooltip, Legend } from "recharts";

const GEXChart = ({data, callWall, putWall, flipPoint}) => {
    return (
        <ComposedChart
            layout="vertical"
            style={{ width: "100%", maxHeight: "100vh", aspectRatio: 1 / 1.618 }}
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
            <Area dataKey="callGEX" fill="#8884d8" stroke="#8884d8" />
            <Area dataKey="putGEX" fill="#d8d484" stroke="#d8d484" />
            <Bar dataKey="totalGEX" barSize={20} fill="#413ea0" />
            {callWall && <ReferenceLine y={callWall} stroke="#8884d8" strokeDasharray="3 3" strokeWidth={1} strokeOpacity={0.65} label={{ value: 'Call Wall', position: 'insideTopRight', fill: '#8884d8' }} />}
            {putWall && <ReferenceLine y={putWall} stroke="#d8d484" strokeDasharray="3 3" strokeWidth={1} strokeOpacity={0.65} label={{ value: 'Put Wall', position: 'insideTopRight', fill: '#d8d484' }} />}
            {flipPoint && <ReferenceLine y={flipPoint} stroke="#ff0000" strokeDasharray="3 3" strokeWidth={1} strokeOpacity={0.65} label={{ value: 'Flip Point', position: 'insideTopRight', fill: '#ff0000' }} />}
        </ComposedChart>
    );
};

export default GEXChart;