import {SymbolModel} from "../model/SymbolModel";

const API_URL = 'http://localhost:8080/symbol'

export const fetchSymbolsList = async (authToken: string) => {
    const response = await fetch(`${API_URL}`, {
        method: 'GET',
        headers: {
            Authorization: `Bearer ${authToken}`
        }
    });
    if (response.ok) {
        return await response.json();
    }
    throw new Error('Failed to fetch symbols list');
}

export const fetchSymbolsNameList = async () => {
    const response = await fetch(`${API_URL}/names`);
    if (response.ok) {
        return await response.json();
    }
    throw new Error('Failed to fetch symbols list');
}

export const createSymbol = async (symbol: SymbolModel, authToken: string) => {
    const response = await fetch(`${API_URL}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${authToken}`
        },
        body: JSON.stringify(symbol)
    });
    if (response.ok) {
        return;
    }
    throw new Error('Failed to create symbol');
}
