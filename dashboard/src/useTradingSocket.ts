import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

export interface TradeEvent {
  tradeId: number;
  ticker: string;
  buyOrderId: number;
  sellOrderId: number;
  price: number;
  quantity: number;
  executedAt: string;
}

export interface OrderBookSnapshot {
  ticker: string;
  bestBid: number | null;
  bestAsk: number | null;
  totalOrders: number;
}

export function useTradingSocket(ticker: string) {
  const [trades, setTrades] = useState<TradeEvent[]>([]);
  const [book, setBook] = useState<OrderBookSnapshot | null>(null);
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS("http://localhost:8081/ws"),
      reconnectDelay: 3000,
      onConnect: () => {
        client.subscribe("/topic/trades", (message) => {
          const event: TradeEvent = JSON.parse(message.body);
          if (event.ticker === ticker) {
            setTrades((prev) => [event, ...prev].slice(0, 20));
          }
        });
        client.subscribe(`/topic/orderbook/${ticker}`, (message) => {
          const snapshot: OrderBookSnapshot = JSON.parse(message.body);
          setBook(snapshot);
        });
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [ticker]);

  return { trades, book };
}