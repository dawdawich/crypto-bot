import React from 'react';
import './index.css';
import App from './App';
import {createRoot} from "react-dom/client";
import {MetaMaskProvider} from "@metamask/sdk-react";

let container = document.getElementById('root') as HTMLElement;
const root = createRoot(container);
root.render(
    <React.StrictMode>
        <MetaMaskProvider debug={true} sdkOptions={{
            dappMetadata: {
                name: "JOAT",
                url: window.location.href
            }
        }}>
            <App/>
        </MetaMaskProvider>
    </React.StrictMode>
);
