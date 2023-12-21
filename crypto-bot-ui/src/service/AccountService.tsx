import {Account} from "../model/Account";

const API_URL = 'http://dawdawich.space:8080/account';

export const fetchAuthToken = async (email: string, password: string) => {
    // Base64 encode the email and password
    const encodedCredentials = btoa(`${email}:${password}`);
    try {
        const options = {
            method: 'GET',
            headers: {
                'Authorization': `Basic ${encodedCredentials}`,
                'Access-Control-Allow-Origin': '*'
            }
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
        const response = await fetch(`${API_URL}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify(body)
        });
        if (response.ok) {
            return;
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to create account');
}
