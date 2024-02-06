import {SymbolModel} from "../model/SymbolModel";
import {SERVER_HOST} from "./Constants";
import {AuthInfo} from "../model/AuthInfo";

const API_URL = `${SERVER_HOST}/symbol`

export const fetchSymbolsList = async (auth: AuthInfo) => {
    const response = await fetch(`${API_URL}`, {
        method: 'GET',
        headers: {
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        }
    });
    if (response.ok) {
        return await response.json();
    }
    throw new Error('Failed to fetch symbols list');
}

export const fetchSymbolsNameList = async () => {
    const response = await fetch(`${API_URL}/names`, {
        headers: {
            'Access-Control-Allow-Origin': '*'
        }
    });
    if (response.ok) {
        return await response.json();
    }
    throw new Error('Failed to fetch symbols list');
}

export const createSymbol = async (auth: AuthInfo, symbol: SymbolModel) => {
    const response = await fetch(`${API_URL}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify(symbol)
    });
    if (response.ok) {
        return;
    }
    throw new Error('Failed to create symbol');
}
