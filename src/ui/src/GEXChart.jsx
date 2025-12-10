import { ComposedChart, Area, Bar, XAxis, YAxis, ReferenceLine, CartesianGrid, Tooltip, Legend } from "recharts";

const GEXChart = ({data, callWall, putWall, flipPoint}) => {

    const CustomTooltip = ({ active, payload, label }) => {
        if (active && payload && payload.length) {
            const data = payload[0].payload; // Access the original data object
            return (
                <div style={{ backgroundColor: 'white', padding: '10px', border: '1px solid #ccc' }}>
                    <p>Strike: ${label}</p>
                    <p style={{ color: payload[0].stroke }}>Call GEX: {data.callGEX}</p>
                    <p style={{ color: payload[1].stroke }}>Put GEX: {data.putGEX}</p>
                    <p style={{ color: payload[2].stroke }}>Total GEX: {data.totalGEX}</p>
                </div>
            );
        }
        return null;
    };

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
            <XAxis type="number"  />
            <YAxis dataKey="strike" type="category" scale="band" width="auto" />
            <Tooltip content={<CustomTooltip />}/>
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

/*
style={{ width: "100%", maxHeight: "100vh", aspectRatio: 1 / 1.618 }}
 */