import { useState, useEffect } from "react";
import Loading from "./Loading.jsx";
import Alert from "./Alert.jsx";
import CustomTable from "./CustomTable.jsx";

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
                            const headers = [
                                "Order ID",
                                "Type",
                                "Entered",
                                "Closed",
                                "Status"
                            ];
                            const rowData = orders.map(order => {
                                const orderLegs = order.orderLegCollection.map(orderLeg => [
                                    `${orderLeg.instruction} ${orderLeg.quantity} ${orderLeg.instrument.description} ( ${orderLeg.instrument.symbol} )`
                                ]);
                                return [
                                    [
                                        order.orderId,
                                        order.complexOrderStrategyType,
                                        order.enteredTime,
                                        order.closeTime,
                                        `${order.status} ${order.statusDescription ? order.statusDescription : ''}`
                                    ],
                                    ...orderLegs
                                ]
                            }).flat(1);
                            const cols = 5;
                            return <CustomTable headers={headers} rowData={rowData} cols={cols} />;
                            // return <pre>{JSON.stringify(orders, null , 2)}</pre>
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

[
  {
    "accountNumber": 56622352,
    "cancelable": false,
    "closeTime": "2026-01-14T15:02:00Z",
    "complexOrderStrategyType": "VERTICAL",
    "destinationLinkName": "AutoRoute",
    "duration": "DAY",
    "editable": false,
    "enteredTime": "2026-01-14T15:02:00Z",
    "filledQuantity": 0,
    "orderId": 1005131565219,
    "orderLegCollection": [
      {
        "instruction": "SELL_TO_OPEN",
        "instrument": {
          "assetType": "OPTION",
          "cusip": "0XSP..ME60690000",
          "description": "MINI-SPX 500 PM-SET 01/14/2026 $690 Put",
          "instrumentId": 243964684,
          "optionDeliverables": [
            {
              "deliverableUnits": 100,
              "symbol": "$XSP"
            }
          ],
          "putCall": "PUT",
          "symbol": "XSP   260114P00690000",
          "type": "VANILLA",
          "underlyingSymbol": "XSP"
        },
        "legId": 1,
        "orderLegType": "OPTION",
        "positionEffect": "OPENING",
        "quantity": 1
      },
      {
        "instruction": "BUY_TO_OPEN",
        "instrument": {
          "assetType": "OPTION",
          "cusip": "0XSP..ME60680000",
          "description": "MINI-SPX 500 PM-SET 01/14/2026 $680 Put",
          "instrumentId": 243935628,
          "optionDeliverables": [
            {
              "deliverableUnits": 100,
              "symbol": "$XSP"
            }
          ],
          "putCall": "PUT",
          "symbol": "XSP   260114P00680000",
          "type": "VANILLA",
          "underlyingSymbol": "XSP"
        },
        "legId": 2,
        "orderLegType": "OPTION",
        "positionEffect": "OPENING",
        "quantity": 1
      }
    ],
    "orderStrategyType": "SINGLE",
    "orderType": "NET_CREDIT",
    "price": 1.59,
    "quantity": 1,
    "remainingQuantity": 0,
    "requestedDestination": "AUTO",
    "session": "NORMAL",
    "status": "REJECTED",
    "statusDescription": "You do not have enough available cash/buying power for this order due to recently deposited funds still on hold.",
    "tag": "API_TOS:TRADE_ALL"
  }
]
 */
