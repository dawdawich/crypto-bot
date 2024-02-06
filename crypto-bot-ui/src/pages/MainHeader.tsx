import {useLocation} from "wouter";
import {Button, Divider} from "@mui/material";
import "../css/HeaderStyles.css";
import {useState} from "react";
import { useSDK } from '@metamask/sdk-react';
import {requestSalt} from "../service/AccountService";
import {useAuth} from "../context/AuthContext";

const MainHeader = () => {
    const { authInfo, login, logout } = useAuth();
    const [location, navigate] = useLocation();
    const { sdk, connected, connecting, provider, chainId } = useSDK();

    const connect = async () => {
        try {
            const address = (await sdk?.connect() as string[])?.[0];
            const salt = await requestSalt(address!);
            const message = `Signature will be valid until:\n${formatDate(parseInt(salt))}`;
            const signature = await provider?.request({
                method: 'personal_sign',
                params: [message, address]
            }) as string;
            login({address, signature});
        } catch(err) {
            console.warn(`failed to connect to wallet`, err);
        }
    };
    const formatDate = (seconds: number) => {
        const date = new Date(0);
        date.setUTCSeconds(seconds);

        const year = date.getUTCFullYear();
        const month = String(date.getUTCMonth() + 1).padStart(2, '0');
        const day = String(date.getUTCDate()).padStart(2, '0');
        const hours = String(date.getUTCHours()).padStart(2, '0');
        const minutes = String(date.getUTCMinutes()).padStart(2, '0');
        const secondsFormatted = String(date.getUTCSeconds()).padStart(2, '0');

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
                        authInfo &&
                        <Button color="inherit" onClick={() => navigate("/analyzer")}
                                variant={location === "/analyzer" ? 'contained' : undefined}>Analyzers</Button>
                    }
                    <Button color="inherit" onClick={() => navigate("/top-analyzers")}
                            variant={location === "/top-analyzers" ? 'contained' : undefined}>Public Top
                        Analyzers</Button>
                    {
                        authInfo &&
                        <Button color="inherit" onClick={() => navigate("/manager")}
                                variant={location === "/manager" ? 'contained' : undefined}>Managers</Button>
                    }
                    {
                        authInfo &&
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
                {authInfo ? (
                    <div className="user-info">
                        <Button onClick={logout}><span>{authInfo.address}</span></Button>
                    </div>
                ) : (
                    <Button onClick={connect}>Connect</Button>
                )}
            </header>
            <Divider sx={{bgcolor: 'eee'}}/>
        </div>
    );
}

export default MainHeader;
