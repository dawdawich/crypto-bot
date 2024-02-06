import {createContext, useContext, useState} from "react";
import React from "react";
import {AuthInfo} from "../model/AuthInfo";

interface AuthContextType {
    authInfo: AuthInfo | undefined;
    login: (userToken: AuthInfo) => void;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{children: React.ReactNode}> = ({ children }) => {
    const [authInfo, setAuthInfo] = useState<AuthInfo | undefined>(() => {
        const info = localStorage.getItem('auth.info');
        return info == null ? undefined : JSON.parse(info) as AuthInfo;
    });

    const login = (authInfo: AuthInfo) => {
        setAuthInfo(authInfo);
        localStorage.setItem('auth.info', JSON.stringify(authInfo));
    };

    const logout = () => {
        setAuthInfo(undefined);
        localStorage.removeItem('auth.info');
    };

    return (
        <AuthContext.Provider value={{ authInfo, login, logout }}>
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
