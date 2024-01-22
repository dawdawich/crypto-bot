import {AnalyzerModel} from "../model/AnalyzerModel";
import {SERVER_HOST} from "./Constants";
import {Analyzer} from "../pages/analyzer/model/Analyzer";
import {fetchWrapper} from "../components/api/fetchWrapper";

const API_URL = `${SERVER_HOST}/analyzer`;

export const fetchTopAnalyzersData = async () => {
    try {
        const path = `top20`;
        const request = fetchWrapper({baseUrl: API_URL});
        const response = await request.methodGET(path);
        //TODO: Handling response and errors in next steps
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
        const path = `${analyzerId}`
        const request = fetchWrapper({baseUrl: API_URL})
        const response = await request.methodGET(path)
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
        const path = `?page=${page}&size=${size}`
        const request = fetchWrapper({baseUrl: API_URL, token: authToken})
        const response = await request.methodGET(path)
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
    const path = ``
    const request = fetchWrapper({baseUrl: API_URL, token: authToken})
    const response = await request.methodPOST(path, JSON.stringify(analyzer))
    //TODO: Handling response and errors in next steps
    if (response.ok) {
        return;
    }
    throw new Error('Failed to create analyzer');
}

export const changeAnalyzerStatus = async (id: string, status: boolean, authToken: string) => {
    const activation_state = status ? 'activate' : 'deactivate';
    const path = `${id}/${activation_state}`
    const request = fetchWrapper({baseUrl: API_URL, token: authToken})
    const response = await request.methodPUT(path)
    //TODO: Handling response and errors in next steps
    if (response.ok) {
        return;
    }
    throw new Error('Failed to change status');
}

export const deleteAnalyzer = async (id: string, authToken: string) => {
    const path =`${id}`
    const request = fetchWrapper({baseUrl: API_URL, token: authToken})
    const response = await request.methodDELETE(path)
    //TODO: Handling response and errors in next steps
    if (response.ok) {
        return;
    }
    throw new Error('Failed to delete analyzer');
}
