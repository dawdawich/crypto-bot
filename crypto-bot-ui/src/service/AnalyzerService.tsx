import {AnalyzerModel} from "../model/AnalyzerModel";

const API_URL = 'http://api-service:8080/analyzer';

export const fetchTopAnalyzersData = async () => {
    try {
        const response = await fetch(`${API_URL}/top20`);
        if (response.ok) {
            return await response.json();
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to fetch data');
}

export const fetchAnalyzerData = async (analyzerId: string) => {
    try {
        const response = await fetch(`${API_URL}/${analyzerId}`);
        if (response.ok) {
            return await response.json();
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to fetch data');
}

export const fetchAnalyzersList = async (authToken: string) => {
    try {
        const response = await fetch(`${API_URL}`, {
            headers: {
                Authorization: `Bearer ${authToken}`
            }
        });
        if (response.ok) {
            return await response.json();
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to fetch analyzers list');
}

export const createAnalyzer = async (analyzer: AnalyzerModel, authToken: string) => {
    const response = await fetch(`${API_URL}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${authToken}`
        },
        body: JSON.stringify(analyzer)
    });
    if (response.ok) {
        return;
    }
    throw new Error('Failed to create analyzer');
}

export const changeAnalyzerStatus = async (id: string, status: boolean, authToken: string) => {
    const path = status ? 'activate' : 'deactivate';
    const response = await fetch(`${API_URL}/${id}/${path}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${authToken}`
        }
    });
    if (response.ok) {
        return;
    }
    throw new Error('Failed to change status');
}

export const deleteAnalyzer = async (id: string, authToken: string) => {
    const response = await fetch(`${API_URL}/${id}`, {
        method: 'DELETE',
        headers: {
            Authorization: `Bearer ${authToken}`
        }
    });
    if (response.ok) {
        return;
    }
    throw new Error('Failed to delete analyzer');
}
