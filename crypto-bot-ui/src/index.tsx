import React from 'react';
import './index.css';
import App from './App';
import {createRoot} from "react-dom/client";

let container = document.getElementById('root') as HTMLElement;
const root = createRoot(container);
root.render(<React.StrictMode><App/></React.StrictMode>);
