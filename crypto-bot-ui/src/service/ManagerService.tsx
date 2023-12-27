import {Manager} from "../model/Manager";
import {SERVER_HOST} from "./Constants";

const API_URL = `${SERVER_HOST}/trade-manager`;

export const fetchManagersData = async (authToken: string) => {
    try {
        const response = await fetch(`${API_URL}`, {
            method: "GET",
            headers: {
                'Authorization': `Bearer ${authToken}`,
                'Access-Control-Allow-Origin': '*'
            }
        });
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
        const response = await fetch(`${API_URL}/${managerId}`, {
            method: "GET",
            headers: {
                'Authorization': `Bearer ${authToken}`,
                'Access-Control-Allow-Origin': '*'
            }
        });
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
    const response = await fetch(`${API_URL}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${authToken}`,
            'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify(manager)
    });
    if (response.ok) {
        return await response.text();
    }
    throw new Error('Failed to create manager data');
}

export const updateManagerStatus = async (managerId: string, status: boolean, authToken: string) => {
    try {
        const response = await fetch(`${API_URL}/${managerId}/status`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${authToken}`,
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify({status})
        });
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
        const response = await fetch(`${API_URL}/${managerId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${authToken}`,
                'Access-Control-Allow-Origin': '*'
            }
        });
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
        const response = await fetch(`${API_URL}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${authToken}`,
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify(manager)
        });
        if (response.ok) {
            return true;
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to update manager data');
}
