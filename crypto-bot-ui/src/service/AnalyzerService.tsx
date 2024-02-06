import {AnalyzerModel} from "../model/AnalyzerModel";
import {SERVER_HOST} from "./Constants";
import {Analyzer} from "../pages/analyzer/model/Analyzer";
import {fetchWrapper} from "../components/api/fetchWrapper";
import {FetchMethods} from "../components/api/type";

const API_URL = `${SERVER_HOST}/analyzer`;

export const fetchTopAnalyzersData = async () => {
    try {
        const response = await fetchWrapper({
            url: `${API_URL}/top20`,
            method: FetchMethods.GET
        });
        if (response.ok) {
            return await response.json();
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to fetch data');
}

export const fetchAnalyzerData = async (analyzerId: string) => {
    try {
        const response = await fetchWrapper({
            url: `${API_URL}/${analyzerId}`,
            method: FetchMethods.GET
        });
        if (response.ok) {
            return await response.json();
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to fetch data');
}

export const fetchAnalyzersList = async (authToken: string, page: number, size: number) => {
    try {
        const response = await fetchWrapper({
            url: `${API_URL}/?page=${page}&size=${size}`,
            method: FetchMethods.GET,
            token: authToken,

        });
        //TODO: Handling response and errors in next steps
        if (response.ok) {
            return await response.json() as {analyzers: Analyzer[], totalSize: number};
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to fetch analyzers list');
}
export const createAnalyzer = async (analyzer: AnalyzerModel, authToken: string) => {
    const response = await fetchWrapper({
        url: `${API_URL}`,
        method: FetchMethods.POST,
        token: authToken,
        body: analyzer
    });
    //TODO: Handling response and errors in next steps
    if (response.ok) {
        return;
    }
    throw new Error('Failed to create analyzer');
}

export const changeAnalyzerStatus = async (id: string, status: boolean, authToken: string) => {
    const activation_state = status ? 'activate' : 'deactivate';
    const response = await fetchWrapper({
        url: `${API_URL}/${id}/${activation_state}`,
        method: FetchMethods.PUT,
        token: authToken,
    });
    //TODO: Handling response and errors in next steps
    if (response.ok) {
        return;
    }
    throw new Error('Failed to change status');
}

export const deleteAnalyzer = async (id: string, authToken: string) => {
    const response = await fetchWrapper({
        url: `${API_URL}/${id}`,
        method: FetchMethods.DELETE,
        token: authToken,
    });
    //TODO: Handling response and errors in next steps
    if (response.ok) {
        return;
    }
    throw new Error('Failed to delete analyzer');
}
