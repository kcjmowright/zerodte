/**
 *
 * @param headers - An optional array of header values.
 * @param rowData - An array of arrays containing the row data and cells.
 * @param cols - An optional number that determines the number of columns and if it's less than the array size of the row, then
 *              it will calculate a column span.
 * @returns {React.JSX.Element}
 * @constructor
 */
function CustomTable({ headers, rowData, cols }) {
    return (
        <table
            className="w-full
                        border-collapse
                        border
                        border-gray-400
                        bg-white
                        text-sm
                        dark:border-gray-500
                        dark:bg-gray-800">
            {
                (() => {
                    return headers &&
                        <thead className="bg-gray-50 dark:bg-gray-700">
                            <tr>
                                {
                                    headers.map(header => {
                                        return <th className="w-1/2
                                                            border
                                                            border-gray-300
                                                            p-4
                                                            text-left
                                                            font-semibold
                                                            text-gray-900
                                                            dark:border-gray-600
                                                            dark:text-gray-200">
                                            {header}
                                        </th>;
                                    })
                                }
                            </tr>
                        </thead>;
                })()
            }
            <tbody>
            {
                rowData && rowData.map((row) => {
                    const attrs = (cols !== row.length) ? { "colspan": cols / row.length } : {};
                    return <tr>
                        {
                            row.map((cell) => {
                                 return <td { ...attrs } className="border
                                                    border-gray-300
                                                    p-4
                                                    text-gray-500
                                                    dark:border-gray-700
                                                    dark:text-gray-400"
                                         >{ cell }</td>;
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