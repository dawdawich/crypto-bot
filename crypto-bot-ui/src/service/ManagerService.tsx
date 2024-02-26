import {SERVER_HOST} from "./Constants";
import {AuthInfo} from "../model/AuthInfo";
import {ManagerRequestModel} from "../model/ManagerRequestModel";
import {ManagerResponse} from "../model/ManagerResponse";
import {UnauthorizedError} from "../utils/errors/UnauthorizedError";

const API_URL = `${SERVER_HOST}/manager`;

export const fetchManagersList = async (auth: AuthInfo) => {
    try {
        const response = await fetch(`${API_URL}`, {
            method: "GET",
            headers: {
                'Account-Address': btoa(auth.address),
                'Account-Address-Signature': btoa(auth.signature),
                'Access-Control-Allow-Origin': '*'
            }
        });
        if (response.ok) {
            return await response.json() as ManagerResponse[];
        } else if (response.status === 401) {
            throw new UnauthorizedError('Signature is invalid');
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to fetch managers data');
}
export const createManager = async (auth: AuthInfo, manager: ManagerRequestModel) => {
    const response = await fetch(`${API_URL}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify(manager)
    });
    if (response.ok) {
        return await response.text();
    } else if (response.status === 401) {
        throw new UnauthorizedError('Signature is invalid');
    }
    throw new Error('Failed to create manager data');
}

export const updateManagerStatus = async (auth: AuthInfo, managerId: string, status: string) => {
    try {
        const response = await fetch(`${API_URL}/${managerId}/status`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Account-Address': btoa(auth.address),
                'Account-Address-Signature': btoa(auth.signature),
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify({status})
        });
        if (response.ok) {
            return;
        } else if (response.status === 401) {
            throw new UnauthorizedError('Signature is invalid');
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to update manager data');
}

export const deleteManager = async (auth: AuthInfo, managerId: string) => {
    try {
        const response = await fetch(`${API_URL}/${managerId}`, {
            method: 'DELETE',
            headers: {
                'Account-Address': btoa(auth.address),
                'Account-Address-Signature': btoa(auth.signature),
                'Access-Control-Allow-Origin': '*'
            }
        });
        if (response.status === 204) {
            return;
        } else if (response.status === 401) {
            throw new UnauthorizedError('Signature is invalid');
        }
    }  catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to delete manager data');
}
