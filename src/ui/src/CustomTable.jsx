/**
 *
 * @param headers - An array of header values.
 * @param rowData - An array of arrays containing the row data.
 * @param cellFormatters - An optional array of cell formatting functions that takes the cell data as an argument.
 * @returns {React.JSX.Element}
 * @constructor
 */
function CustomTable({ headers, rowData, cellFormatters }) {
    return (
        <table
            className="w-full border-collapse border border-gray-400 bg-white text-sm dark:border-gray-500 dark:bg-gray-800">
            <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                    {
                        headers && headers.map(header => {
                            return <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">
                                {header}
                            </th>;
                        })
                    }
                </tr>
            </thead>
            <tbody>
            {
                rowData && rowData.map((row) => {
                    return <tr>
                        {
                            row && row.map((cell, idx) => {
                                return <td className="border border-gray-300 p-4 text-gray-500 dark:border-gray-700 dark:text-gray-400">
                                    {
                                        ( cellFormatters && cellFormatters[idx] && cellFormatters[idx](cell) ) || cell
                                    }
                                </td>;
                            })
                        }
                    </tr>;
                })
            }
            </tbody>
        </table>
    );
}
export default CustomTable;