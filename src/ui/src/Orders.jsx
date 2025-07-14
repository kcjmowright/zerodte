import { useState, useEffect } from "react";
import Loading from "./Loading.jsx";
import Alert from "./Alert.jsx";

function Orders() {
    const [orders, setOrders] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    async function fetchData() {
        setLoading(true);
        try {
            const response = await fetch("/api/v1/orders");
            if (!response.ok) {
                const e = await response.json();
                throw new Error(`${e.message}`);
            }
            const result = await response.json();
            setOrders(result);
        } catch (error) {
            setError(error);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        (async () => await fetchData())();
    }, []);

    return (
        <>
            <header className="bg-white shadow-sm">
                <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
                    <h1 className="text-3xl font-bold tracking-tight text-gray-900">Orders</h1>
                </div>
            </header>
            <main>
                <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
                    {
                        (() => {
                            if (loading) {
                                return <Loading />;
                            }
                            if (error) {
                                return <Alert message={error.message} />;
                            }
                            if (!orders || !orders.length) {
                                return <div>No orders at this time.</div>;
                            }
                            return <pre>{JSON.stringify(orders, null , 2)}</pre>
                            // return <table
                            //     className="w-full border-collapse border border-gray-400 bg-white text-sm dark:border-gray-500 dark:bg-gray-800">
                            //     <thead className="bg-gray-50 dark:bg-gray-700">
                            //         <tr>
                            //             <th className="w-1/2 border border-gray-300 p-4 text-left font-semibold text-gray-900 dark:border-gray-600 dark:text-gray-200">Last
                            //                 Price
                            //             </th>
                            //         </tr>
                            //     </thead>
                            //     <tbody>
                            //         <tr>
                            //             <td></td>
                            //         </tr>
                            //     </tbody>
                            // </table>
                        })()
                    }
                </div>
            </main>
        </>
    );
}

export default Orders;

/*
[
  {
    "session": "NORMAL",
    "duration": "DAY",
    "orderType": "NET_CREDIT",
    "complexOrderStrategyType": "IRON_CONDOR",
    "quantity": 1,
    "filledQuantity": 1,
    "remainingQuantity": 0,
    "requestedDestination": "AUTO",
    "destinationLinkName": "CDRG",
    "price": 2.81,
    "orderLegCollection": [
      {
        "orderLegType": "OPTION",
        "legId": 1,
        "instruction": "SELL_TO_OPEN",
        "positionEffect": "OPENING",
        "quantity": 1,
        "instrument": {
          "assetType": "OPTION",
          "cusip": "0NVDA.FD50144000",
          "symbol": "NVDA  250613C00144000",
          "description": "NVIDIA CORP 06/13/2025 $144 Call",
          "instrumentId": 234871070,
          "type": "VANILLA",
          "putCall": "CALL",
          "underlyingSymbol": "NVDA",
          "optionDeliverables": [
            {
              "symbol": "NVDA",
              "deliverableUnits": 100
            }
          ]
        }
      },
      {
        "orderLegType": "OPTION",
        "legId": 2,
        "instruction": "BUY_TO_OPEN",
        "positionEffect": "OPENING",
        "quantity": 1,
        "instrument": {
          "assetType": "OPTION",
          "cusip": "0NVDA.FD50150000",
          "symbol": "NVDA  250613C00150000",
          "description": "NVIDIA CORP 06/13/2025 $150 Call",
          "instrumentId": 234584295,
          "type": "VANILLA",
          "putCall": "CALL",
          "underlyingSymbol": "NVDA",
          "optionDeliverables": [
            {
              "symbol": "NVDA",
              "deliverableUnits": 100
            }
          ]
        }
      },
      {
        "orderLegType": "OPTION",
        "legId": 3,
        "instruction": "SELL_TO_OPEN",
        "positionEffect": "OPENING",
        "quantity": 1,
        "instrument": {
          "assetType": "OPTION",
          "cusip": "0NVDA.RD50142000",
          "symbol": "NVDA  250613P00142000",
          "description": "NVIDIA CORP 06/13/2025 $142 Put",
          "instrumentId": 234978066,
          "type": "VANILLA",
          "putCall": "PUT",
          "underlyingSymbol": "NVDA",
          "optionDeliverables": [
            {
              "symbol": "NVDA",
              "deliverableUnits": 100
            }
          ]
        }
      },
      {
        "orderLegType": "OPTION",
        "legId": 4,
        "instruction": "BUY_TO_OPEN",
        "positionEffect": "OPENING",
        "quantity": 1,
        "instrument": {
          "assetType": "OPTION",
          "cusip": "0NVDA.RD50135000",
          "symbol": "NVDA  250613P00135000",
          "description": "NVIDIA CORP 06/13/2025 $135 Put",
          "instrumentId": 234438524,
          "type": "VANILLA",
          "putCall": "PUT",
          "underlyingSymbol": "NVDA",
          "optionDeliverables": [
            {
              "symbol": "NVDA",
              "deliverableUnits": 100
            }
          ]
        }
      }
    ],
    "orderStrategyType": "SINGLE",
    "orderId": 1003494772896,
    "cancelable": false,
    "editable": false,
    "status": "FILLED",
    "enteredTime": "2025-06-10T14:14:47Z",
    "closeTime": "2025-06-10T14:15:07Z",
    "tag": "API_TOS:Empty",
    "accountNumber": 56622352,
    "orderActivityCollection": [
      {
        "activityType": "EXECUTION",
        "executionType": "FILL",
        "quantity": 1,
        "orderRemainingQuantity": 0,
        "executionLegs": [
          {
            "legId": 1,
            "quantity": 1,
            "mismarkedQuantity": 0,
            "price": 1.61,
            "time": "2025-06-10T14:15:07Z"
          },
          {
            "legId": 2,
            "quantity": 1,
            "mismarkedQuantity": 0,
            "price": 0.26,
            "time": "2025-06-10T14:15:07Z"
          },
          {
            "legId": 3,
            "quantity": 1,
            "mismarkedQuantity": 0,
            "price": 1.72,
            "time": "2025-06-10T14:15:07Z"
          },
          {
            "legId": 4,
            "quantity": 1,
            "mismarkedQuantity": 0,
            "price": 0.26,
            "time": "2025-06-10T14:15:07Z"
          }
        ]
      }
    ]
  }
]
 */
