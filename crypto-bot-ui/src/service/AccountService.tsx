import {SERVER_HOST} from "./Constants";
import {fetchWrapper} from "../components/api/fetchWrapper";

const API_URL = `${SERVER_HOST}/account`;

export const fetchAuthToken = async (email: string, password: string) => {
    // Base64 encode the email and password
    const encodedCredentials = btoa(`${email}:${password}`);
    const path = `token`;
    const header = {'Authorization': `Basic ${encodedCredentials}`}
    const request = fetchWrapper({baseUrl: API_URL, headers: header});
    const response = await request.methodGET(path);
    //TODO: Handling response and errors in next steps
    if (!response.ok) {
        switch (response.status) {
            case 401:
                throw 'Invalid credentials';
            case 500:
                throw 'Something went wrong on the server';
            default:
                throw 'Something went wrong';
        }
    }
    return await response.text() as string
}

export const fetchAccountInfo = async (authToken: string) => {
    try {
        const path = ``;
        const request = fetchWrapper({baseUrl: API_URL, token: authToken});
        const response = await request.methodGET(path);
        //TODO: Handling response and errors in next steps
        if (response.ok) {
            return await response.json();
        }
    } catch (error) {
        throw error;
    }
    throw new Error('Failed to fetch account info');
}

export const createAccount = async (username: string, name: string, surname: string, email: string, password: string) => {
    const body = {username: username, name: name, surname: surname, email: email, password: password};
    const path = ``;
    const request = fetchWrapper({baseUrl: API_URL});
    const response = await request.methodPOST(path, body);
    //TODO: Handling response and errors in next steps
    if (!response.ok) {
        switch (response.status) {
            case 412:
                throw 'This email is not allowed for registration';
            case 409:
                throw 'This account is already registered';
            case 500:
                throw 'Something went wrong on the server';
            default:
                throw 'Something went wrong';
        }
    }
    return response;
}

export const getApiTokens = async (authToken: string) => {
    const path = `api-token`;
    const request = fetchWrapper({baseUrl: API_URL, token: authToken});
    const response = await request.methodGET(path);
    //TODO: Handling response and errors in next steps
    if (response.ok) {
        return await response.json();
    }
    throw new Error(`Failed to fetch tokens, response code: ${response.status}`);
}

export const addApiToken = async (body: any, authToken: string) => {
    const path = `api-token`;
    const request = fetchWrapper({baseUrl: API_URL, token: authToken});
    const response = await request.methodPOST(path, body);
    //TODO: Handling response and errors in next steps
    if (response.ok) {
        return await response.text();
    }
    throw new Error(`Failed to create api token, response code: ${response.status}`);
}

export const deleteApiToken = async (id: string, authToken: string) => {
    const path = `api-token/${id}`;
    const request = fetchWrapper({baseUrl: API_URL, token: authToken});
    const response = await request.methodDELETE(path);
    //TODO: Handling response and errors in next steps
    if (response.ok) {
        return;
    }
    throw new Error(`Failed to delete api token, response code: ${response.status}`);
}
