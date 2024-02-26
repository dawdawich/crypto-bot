import React from 'react';
import './index.css';
import App from './App';
import {createRoot} from "react-dom/client";
import {MetaMaskProvider} from "@metamask/sdk-react";
import {ToastContainer} from "react-toastify";
import 'react-toastify/dist/ReactToastify.css';

let container = document.getElementById('root') as HTMLElement;
const root = createRoot(container);
root.render(
    <React.StrictMode>
        <MetaMaskProvider debug={true} sdkOptions={{
            dappMetadata: {
                name: "JOAT",
                url: 'http://dawdawich.space'
            }
        }}>
            <div style={{position: 'relative'}}>
                <App/>
                <ToastContainer style={{position: 'absolute', zIndex: '100'}}/>
            </div>
        </MetaMaskProvider>
    </React.StrictMode>
);
