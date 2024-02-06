import {Manager} from "../model/Manager";
import {SERVER_HOST} from "./Constants";
import {fetchWrapper} from "../components/api/fetchWrapper";
import {FetchMethods} from "../components/api/type";


const API_URL = `${SERVER_HOST}/trade-manager`;

export const fetchManagersData = async (authToken: string) => {
    try {
        const response = await fetchWrapper({
            url: `${API_URL}`,
            method: FetchMethods.GET,
            token: authToken
        });
        //TODO: Handling response and errors in next steps
        if (response.ok) {
            return await response.json();
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to fetch managers data');
}

export const fetchManagerData = async (authToken: string, managerId: string) => {
    try {
        const response = await fetchWrapper({
            url: `${API_URL}/${managerId}`,
            method: FetchMethods.GET,
            token: authToken
        });
        //TODO: Handling response and errors in next steps
        if (response.ok) {
            return await response.json();
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to fetch manager data');
}

export const createManager = async (manager: any, authToken: string) => {
    const response = await fetchWrapper({
        url: `${API_URL}`,
        method: FetchMethods.POST,
        token: authToken,
        body: manager
    });
    //TODO: Handling response and errors in next steps
    if (response.ok) {
        return await response.text();
    }
    throw new Error('Failed to create manager data');
}

export const updateManagerStatus = async (managerId: string, status: string, authToken: string) => {
    try {
        const response = await fetchWrapper({
            url: `${API_URL}/${managerId}/status`,
            method: FetchMethods.PUT,
            token: authToken,
            body: {status}
        });
        //TODO: Handling response and errors in next steps
        if (response.ok) {
            return true;
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to update manager data');
}

export const deleteManager = async (managerId: string, authToken: string) => {
    try {
        const response = await fetchWrapper({
            url: `${API_URL}/${managerId}`,
            method: FetchMethods.DELETE,
            token: authToken,
        });
        //TODO: Handling response and errors in next steps
        if (response.status === 204) {
            return true;
        }
    }  catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to delete manager data');
}

