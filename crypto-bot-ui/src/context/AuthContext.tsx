import React, {createContext, useCallback, useContext, useEffect, useState} from "react";
import {AuthInfo} from "../model/AuthInfo";
import "@metamask/sdk-react";
import {requestSalt} from "../service/AccountService";
import Web3 from 'web3';
import {formatDateForSignature} from "../utils/date-utils";
import {errorToast} from "../pages/toast/Toasts";

export interface AuthContextType {
    authInfo: AuthInfo | undefined;
    login: () => void;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const web3 = new Web3((window as any).ethereum);
export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({children}) => {
    const [authInfo, setAuthInfo] = useState<AuthInfo | undefined>(() => {
        const auth = localStorage.getItem('auth.info');
        if (auth !== null) {
            return JSON.parse(auth);
        }
        return undefined;
    });

    const connect = useCallback((): void => {
        web3.eth.getAccounts()
            .then(async (accounts) => {
                if (accounts.length > 0) {
                    const salt = await requestSalt(accounts[0]);
                    const message = `Signature will be valid until:\n${formatDateForSignature(parseInt(salt))}`;
                    const signature = await web3.eth.personal.sign(message, accounts[0], '') as string;
                    const auth = {address: accounts[0], signature: signature};
                    setAuthInfo(auth);
                    localStorage.setItem('auth.info', JSON.stringify(auth));
                }
            })
            .catch(() => {
                errorToast("Failed to connect to metamask");
            });
    }, []);

    const login = useCallback(async () => {
        await window.ethereum?.request({method: 'eth_requestAccounts'});
        connect();
    }, [connect]);

    useEffect(() => {
        window.ethereum?.on('accountsChanged', function (accounts) {
            logout();
        });
        if (authInfo) {
            web3.eth.getAccounts().then(accounts => {
                if (accounts[0] !== authInfo.address) {
                    logout();
                }
            })
        }
    }, [authInfo, connect]);

    const logout = () => {
        setAuthInfo(undefined);
        localStorage.removeItem('auth.info');
    };

    return (
        <AuthContext.Provider value={{authInfo, login: login, logout}}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = (): AuthContextType => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
