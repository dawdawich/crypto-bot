import {SymbolModel} from "../model/SymbolModel";
import {SERVER_HOST} from "./Constants";
import {fetchWrapper} from "../components/api/fetchWrapper";
import {FetchMethods} from "../components/api/type";


const API_URL = `${SERVER_HOST}/symbol`

export const fetchSymbolsList = async (authToken: string) => {
    const response = await fetchWrapper({
        url: `${API_URL}`,
        method: FetchMethods.GET,
        token: authToken,
    });
    //TODO: Handling response and errors in next steps
    if (response.ok) {
        return await response.json();
    }
    throw new Error('Failed to fetch symbols list');
}

export const fetchSymbolsNameList = async () => {
    const response = await fetchWrapper({
        url: `${API_URL}/names`,
        method: FetchMethods.GET,
    });
    //TODO: Handling response and errors in next steps
    if (response.ok) {
        return await response.json();
    }
    throw new Error('Failed to fetch symbols list');
}

export const createSymbol = async (symbol: SymbolModel, authToken: string) => {
    const response = await fetchWrapper({
        url: `${API_URL}/names`,
        method: FetchMethods.POST,
        body: symbol
    });
    if (response.ok) {
        return;
    }
    throw new Error('Failed to create symbol');
}
