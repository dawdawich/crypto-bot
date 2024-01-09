import {SERVER_HOST} from "./Constants";

const API_URL = `${SERVER_HOST}/account`;

export const fetchAuthToken = async (email: string, password: string) => {
    // Base64 encode the email and password
    const encodedCredentials = btoa(`${email}:${password}`);
    const options = {
        method: 'GET',
        headers: {
            'Authorization': `Basic ${encodedCredentials}`,
            'Access-Control-Allow-Origin': '*'
        },
    };
    const response = await fetch(`${API_URL}/token`, options)
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
        const options = {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${authToken}`,
                'Access-Control-Allow-Origin': '*'
            }
        };
        const response = await fetch(`${API_URL}`, options)
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
    const response = await fetch(`${API_URL}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify(body)
    });
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
    const response = await fetch(`${API_URL}/api-token`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${authToken}`,
            'Access-Control-Allow-Origin': '*'
        }
    });
    if (response.ok) {
        return await response.json();
    }
    throw new Error(`Failed to fetch tokens, response code: ${response.status}`);
}

export const addApiToken = async (body: any, authToken: string) => {
    const response = await fetch(`${API_URL}/api-token`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${authToken}`,
            'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify(body)
    });
    if (response.ok) {
        return await response.text();
    }
    throw new Error(`Failed to create api token, response code: ${response.status}`);
}

export const deleteApiToken = async (id: string, authToken: string) => {
    const response = await fetch(`${API_URL}/api-token/${id}`, {
        method: 'DELETE',
        headers: {
            'Authorization': `Bearer ${authToken}`,
            'Access-Control-Allow-Origin': '*'
        },
    });
    if (response.ok) {
        return;
    }
    throw new Error(`Failed to delete api token, response code: ${response.status}`);
}
