import {Manager} from "../model/Manager";
import {SERVER_HOST} from "./Constants";
import {fetchWrapper} from "../components/api/fetchWrapper";


const API_URL = `${SERVER_HOST}/trade-manager`;

export const fetchManagersData = async (authToken: string) => {
    try {
        const path = ``;
        const request = fetchWrapper({baseUrl: API_URL, token: authToken});
        const response = await request.methodGET(path);
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
        const path = `${managerId}`;
        const request = fetchWrapper({baseUrl: API_URL, token: authToken});
        const response = await request.methodGET(path);
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
    const path = ``;
    const request = fetchWrapper({baseUrl: API_URL, token: authToken});
    const response = await request.methodPOST(path, manager);
    //TODO: Handling response and errors in next steps
    if (response.ok) {
        return await response.text();
    }
    throw new Error('Failed to create manager data');
}

export const updateManagerStatus = async (managerId: string, status: string, authToken: string) => {
    try {
        const path = `${managerId}/status`;
        const request = fetchWrapper({baseUrl: API_URL, token: authToken});
        const response = await request.methodPUT(path, {status});
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
        const path = `${managerId}`;
        const request = fetchWrapper({baseUrl: API_URL, token: authToken});
        const response = await request.methodDELETE(path);
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

export const updateManagerData = async (manager: Manager, authToken: string) => {
    try {
        const path = ``;
        const request = fetchWrapper({baseUrl: API_URL, token: authToken});
        const response = await request.methodPUT(path, manager);
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
