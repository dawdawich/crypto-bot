import React, {Component, createContext, ErrorInfo, useContext, useState} from "react";
import {AuthInfo} from "../model/AuthInfo";
import {useSDK} from "@metamask/sdk-react";
import {requestSalt} from "../service/AccountService";
import {useLocation} from "wouter";
import {UnauthorizedError} from "../utils/errors/UnauthorizedError";

export interface AuthContextType {
    authInfo: AuthInfo | undefined;
    login: () => void;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({children}) => {
    const [, navigate] = useLocation();
    const {sdk, provider} = useSDK();
    const [authInfo, setAuthInfo] = useState<AuthInfo | undefined>(() => {
        const info = localStorage.getItem('auth.info');
        return info == null ? undefined : JSON.parse(info) as AuthInfo;
    });

    const login = async () => {
        try {
            const address = (await sdk?.connect() as string[])?.[0];
            const salt = await requestSalt(address!);
            const message = `Signature will be valid until:\n${formatDate(parseInt(salt))}`;
            const signature = await provider?.request({
                method: 'personal_sign',
                params: [message, address]
            }) as string;
            let computedAuthInfo = {address, signature};
            setAuthInfo(computedAuthInfo);
            localStorage.setItem('auth.info', JSON.stringify(computedAuthInfo))
        } catch (err) {
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

    const logout = () => {
        setAuthInfo(undefined);
        localStorage.removeItem('auth.info');
    };

    try {
        return (
            <AuthContext.Provider value={{authInfo, login, logout}}>
                {children}
            </AuthContext.Provider>
        );
    } catch (e) {
        if (e instanceof UnauthorizedError) {
            logout();
            navigate('/analyzer');
        } else {
            throw e;
        }
    }
    return (<div></div>);
};

export const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
