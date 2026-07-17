import { useState } from "react";
import { useTradingSocket } from "./useTradingSocket";
import "./App.css";

const TICKER = "AAPL";

function App() {
  const { trades, book } = useTradingSocket(TICKER);
  const [status, setStatus] = useState("");

  async function fireOrder(side: "BUY" | "SELL") {
    const price = 150 + Math.random() * 5;
    const res = await fetch("http://localhost:8081/orders", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        ticker: TICKER,
        side,
        price: price.toFixed(2),
        quantity: Math.ceil(Math.random() * 10),
        tif: "GTC",
      }),
    });
    const data = await res.json();
    setStatus(`Order ${data.orderId}: ${data.status}, filled ${data.filledQuantity}/${data.quantity}`);
  }

  return (
    <div className="dashboard">
      <h1>{TICKER} — Live Trading Dashboard</h1>

      <div className="controls">
        <button className="buy" onClick={() => fireOrder("BUY")}>Fire BUY order</button>
        <button className="sell" onClick={() => fireOrder("SELL")}>Fire SELL order</button>
        <span className="status">{status}</span>
      </div>

      <div className="panels">
        <div className="panel">
          <h2>Order book</h2>
          {book ? (
            <table>
              <tbody>
                <tr><td>Best bid</td><td className="bid">{book.bestBid ?? "—"}</td></tr>
                <tr><td>Best ask</td><td className="ask">{book.bestAsk ?? "—"}</td></tr>
                <tr><td>Total resting orders</td><td>{book.totalOrders}</td></tr>
              </tbody>
            </table>
          ) : (
            <p>Waiting for data…</p>
          )}
        </div>

        <div className="panel">
          <h2>Trade tape</h2>
          <ul className="tape">
            {trades.length === 0 && <li>No trades yet</li>}
            {trades.map((t) => (
              <li key={t.tradeId}>
                <span className="price">{t.price}</span>
                <span className="qty">×{t.quantity}</span>
                <span className="time">{new Date(t.executedAt).toLocaleTimeString()}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}

export default App;