import { ComposedChart, Area, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend } from "recharts";

const GEXChart = ({data}) => {
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
        </ComposedChart>
    );
};

export default GEXChart;