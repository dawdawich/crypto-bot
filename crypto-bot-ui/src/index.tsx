import React from 'react';
import './index.css';
import App from './App';
import {createRoot} from "react-dom/client";
import {ToastContainer} from "react-toastify";
import 'react-toastify/dist/ReactToastify.css';

let container = document.getElementById('root') as HTMLElement;
const root = createRoot(container);
root.render(
    <div style={{position: 'relative'}}>
        <App/>
        <ToastContainer style={{position: 'absolute', zIndex: '100'}}/>
    </div>
);
