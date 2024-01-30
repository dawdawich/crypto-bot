import {AnalyzerModel} from "../model/AnalyzerModel";
import {SERVER_HOST} from "./Constants";
import {Analyzer} from "../pages/analyzer/model/Analyzer";

const API_URL = `${SERVER_HOST}/analyzer`;

export const fetchTopAnalyzersData = async () => {
    try {
        const response = await fetch(`${API_URL}/top20`, {
            headers: {
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
    throw new Error('Failed to fetch data');
}

export const fetchAnalyzerData = async (analyzerId: string, authToken: string) => {
    try {
        const response = await fetch(`${API_URL}/${analyzerId}`, {
            headers: {
                Authorization: `Bearer ${authToken}`,
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
    throw new Error('Failed to fetch data');
}

export const fetchAnalyzersList = async (authToken: string, page: number, size: number) => {
    try {
        const response = await fetch(`${API_URL}?page=${page}&size=${size}`, {
            headers: {
                Authorization: `Bearer ${authToken}`,
                'Access-Control-Allow-Origin': '*'
            }
        });
        if (response.ok) {
            return await response.json() as {analyzers: Analyzer[], totalSize: number};
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
            Authorization: `Bearer ${authToken}`,
            'Access-Control-Allow-Origin': '*'
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
            Authorization: `Bearer ${authToken}`,
            'Access-Control-Allow-Origin': '*'
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
            Authorization: `Bearer ${authToken}`,
            'Access-Control-Allow-Origin': '*'
        }
    });
    if (response.ok) {
        return;
    }
    throw new Error('Failed to delete analyzer');
}
