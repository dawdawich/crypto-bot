import {SymbolModel} from "../model/SymbolModel";
import {SERVER_HOST} from "./Constants";
import {fetchWrapper} from "../components/api/fetchWrapper";


const API_URL = `${SERVER_HOST}/symbol`

export const fetchSymbolsList = async (authToken: string) => {
    const path = ``;
    const request = fetchWrapper({baseUrl: API_URL, token: authToken});
    const response = await request.methodGET(path);
    //TODO: Handling response and errors in next steps
    if (response.ok) {
        return await response.json();
    }
    throw new Error('Failed to fetch symbols list');
}

export const fetchSymbolsNameList = async () => {
    const path = `names`;
    const request = fetchWrapper({baseUrl: API_URL});
    const response = await request.methodGET(path);
    //TODO: Handling response and errors in next steps
    if (response.ok) {
        return await response.json();
    }
    throw new Error('Failed to fetch symbols list');
}

export const createSymbol = async (symbol: SymbolModel, authToken: string) => {
    const path = ``;
    const request = fetchWrapper({baseUrl: API_URL});
    const response = await request.methodPOST(path, symbol);
    if (response.ok) {
        return;
    }
    throw new Error('Failed to create symbol');
}
