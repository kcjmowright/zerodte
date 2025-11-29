import { createRoot } from "react-dom/client";
import "./index.css"
import { BrowserRouter, Routes, Route } from "react-router";
import Account from "./Account.jsx";
import Dashboard from "./Dashboard.jsx";
import HeaderNav from "./HeaderNav.jsx";
import Login from "./Login.jsx";
import Movers from "./Movers.jsx";
import Orders from "./Orders.jsx";
import GammaExposure from "./GammaExposure.jsx";

createRoot(document.getElementById("root")).render(
    <BrowserRouter>
        <HeaderNav />
        <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="account" element={<Account />} />
            <Route path="gex" element={<GammaExposure />} />
            <Route path="movers" element={<Movers />} />
            <Route path="orders" element={<Orders />} />
            <Route path="login" element={<Login />} />
        </Routes>
    </BrowserRouter>
);
