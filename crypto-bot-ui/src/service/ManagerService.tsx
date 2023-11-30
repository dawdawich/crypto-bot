import {Manager} from "../pages/manager/model/Manager";

const API_URL = 'http://localhost:8080/trade-manager';

export const fetchManagersData = async () => {
    try {
        const response = await fetch(`${API_URL}`);
        if (response.ok) {
            return await response.json();
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to fetch data');
}

export const fetchManagerData = async (managerId: string) => {
    try {
        const response = await fetch(`${API_URL}/${managerId}`);
        if (response.ok) {
            return await response.json();
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to fetch data');
}

export const updateManagerData = async (manager: Manager) => {
    try {
        const response = await fetch(`${API_URL}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
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
    throw new Error('Failed to fetch data');
}
