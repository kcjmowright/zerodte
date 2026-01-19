import { useEffect, useState, useRef } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

export const useStockWebSocket = (symbol) => {
    const [stockData, setStockData] = useState(null);
    const [isConnected, setIsConnected] = useState(false);
    const clientRef = useRef(null);

    useEffect(() => {
        if (!symbol) {
            return;
        }

        const client = new Client({
            webSocketFactory: () => new SockJS("/ws"),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,

            onConnect: () => {
                console.log("Connected to WebSocket");
                setIsConnected(true);

                // Subscribe to specific stock symbol
                client.subscribe(`/topic/stock/${symbol}`, (message) => {
                    const update = JSON.parse(message.body);
                    setStockData(update);
                });
            },

            onDisconnect: () => {
                setIsConnected(false);
                console.log("Disconnected from WebSocket");
            },

            onStompError: (frame) => {
                console.error("STOMP error:", frame);
            }
        });

        client.activate();
        clientRef.current = client;

        return () => {
            if (clientRef.current) {
                clientRef.current.deactivate();
            }
        };
    }, [symbol]);

    return { stockData, isConnected };
};