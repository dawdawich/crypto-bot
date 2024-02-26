import {SERVER_HOST} from "./Constants";
import {AuthInfo} from "../model/AuthInfo";
import {UnauthorizedError} from "../utils/errors/UnauthorizedError";

const API_URL = `${SERVER_HOST}/account`;


export const requestSalt = async (accountId: string) => {
    const response = await fetch(`${API_URL}/salt`, {
        method: 'GET',
        headers: {
            'Account-Address': btoa(accountId)
        }
    });

    if (response.ok) {
        return await response.text() as string;
    }

    throw Error('Failed to fetch salt for account');
}

export const getApiTokens = async (auth: AuthInfo) => {
    const response = await fetch(`${API_URL}/api-token`, {
        method: 'GET',
        headers: {
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature)
        }
    });
    if (response.ok) {
        return await response.json();
    } else if (response.status === 401) {
        throw new UnauthorizedError('Signature is invalid');
    }
    throw new Error(`Failed to fetch tokens, response code: ${response.status}`);
}

export const addApiToken = async (body: any, auth: AuthInfo) => {
    const response = await fetch(`${API_URL}/api-token`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify(body)
    });
    if (response.ok) {
        return await response.text();
    } else if (response.status === 401) {
        throw new UnauthorizedError('Signature is invalid');
    }
    throw new Error(`Failed to create api token, response code: ${response.status}`);
}

export const deleteApiToken = async (id: string, auth: AuthInfo) => {
    const response = await fetch(`${API_URL}/api-token/${id}`, {
        method: 'DELETE',
        headers: {
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        },
    });
    if (response.ok) {
        return;
    } else if (response.status === 401) {
        throw new UnauthorizedError('Signature is invalid');
    }
    throw new Error(`Failed to delete api token, response code: ${response.status}`);
}
