import {useLocation} from "wouter";
import {Button, Divider} from "@mui/material";
import "../css/HeaderStyles.css";
import {useState} from "react";
import { useSDK } from '@metamask/sdk-react';
import {requestSalt} from "../service/AccountService";

const MainHeader = () => {
    const [address, setAddress] = useState<string | null>(null);
    const [signature, setSignature] = useState<string | null>(null);
    const [location, navigate] = useLocation();
    const { sdk, connected, connecting, provider, chainId } = useSDK();

    const connect = async () => {
        try {
            const accounts = await sdk?.connect();
            setAddress((accounts as string[])?.[0]);
            const salt = await requestSalt(address!);
            const message = `Signature will be valid until:\n${formatDate(parseInt(salt))}`;
            const sign = await provider?.request({
                method: 'personal_sign',
                params: [message, address]
            });
            localStorage.setItem('auth.address', accounts as string)
            localStorage.setItem('auth.signature', sign as string)
        } catch(err) {
            console.warn(`failed to connect..`, err);
        }
    };

    const disconnect = () => {
        setAddress(null);
        setSignature(null);
    };

    const formatDate = (seconds: number) => {
        const date = new Date(seconds * 1000); // Convert seconds to milliseconds

        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const secondsFormatted = String(date.getSeconds()).padStart(2, '0');

        return `${year}-${month}-${day} ${hours}:${minutes}:${secondsFormatted}`;
    }


    const role = localStorage.getItem('auth.role')

    return (
        <div>
            <header className="header">
                <div className="logo-container">
                    <img src="" alt="Logo" className="logo"/>
                </div>
                <nav className="navigation">
                    {
                        signature != null &&
                        <Button color="inherit" onClick={() => navigate("/analyzer")}
                                variant={location === "/analyzer" ? 'contained' : undefined}>Analyzers</Button>
                    }
                    <Button color="inherit" onClick={() => navigate("/top-analyzers")}
                            variant={location === "/top-analyzers" ? 'contained' : undefined}>Public Top
                        Analyzers</Button>
                    {
                        signature != null &&
                        <Button color="inherit" onClick={() => navigate("/manager")}
                                variant={location === "/manager" ? 'contained' : undefined}>Managers</Button>
                    }
                    {
                        signature != null &&
                        <Button color="inherit" onClick={() => navigate("/account")}
                                variant={location === "/account" ? 'contained' : undefined}>Account</Button>
                    }
                    {
                        role === 'ADMIN' &&
                        <Button color="inherit" onClick={() => navigate("/symbols")}
                                variant={location === "/symbols" ? 'contained' : undefined}>Symbols</Button>
                    }
                    {
                        role === 'ADMIN' &&
                        <Button color="inherit" onClick={() => navigate("/monitoring")}
                                variant={location === "/monitoring" ? 'contained' : undefined}>Monitoring</Button>
                    }
                </nav>
                {address ? (
                    <div className="user-info">
                        <button onClick={disconnect}><span>{address}</span></button>
                    </div>
                ) : (
                    <button onClick={connect}>Connect</button>
                )}
            </header>
            <Divider sx={{bgcolor: 'eee'}}/>
        </div>
    );
}

export default MainHeader;
