import {SERVER_HOST} from "./Constants";

const API_URL = `${SERVER_HOST}/account`;

export const fetchAuthToken = async (email: string, password: string) => {
    // Base64 encode the email and password
    const encodedCredentials = btoa(`${email}:${password}`);
    try {
        const options = {
            method: 'GET',
            headers: {
                'Authorization': `Basic ${encodedCredentials}`,
                'Access-Control-Allow-Origin': '*'
            },
            mode: 'cors' as RequestMode
        };
        const response = await fetch(`${API_URL}/token`, options)
        if (response.ok) {
            return response.headers.get('Authorization') as string
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to login');
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
        console.error(error);
        throw error;
    }
    throw new Error('Failed to fetch account info');
}

export const createAccount = async (username: string, name: string, surname: string, email: string, password: string) => {
    const body = {username: username, name: name, surname: surname, email: email, password: password};
    try {
        console.log('Start request');
        const response = await fetch(`${API_URL}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify(body)
        });
        console.log('Request finish');
        if (response.ok) {
            return true;
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to create account');
}

export const getApiTokens = async (authToken: string) => {
    console.log(authToken) // TODO: remove
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
