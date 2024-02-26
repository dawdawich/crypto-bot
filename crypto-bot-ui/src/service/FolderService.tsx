import {AuthInfo} from "../model/AuthInfo";
import {SERVER_HOST} from "./Constants";
import {FolderModel} from "../model/FolderModel";

const API_URL = `${SERVER_HOST}/folder`;

export const createFolder = async (auth: AuthInfo, name: string) => {
    const response = await fetch(`${API_URL}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify({name: name})
    });
    if (response.ok) {
        return await response.json();
    }
    throw new Error('Failed to create folder');
}

export const fetchFolderList = async (auth: AuthInfo) => {
    try {
        const response = await fetch(`${API_URL}`, {
            headers: {
                'Account-Address': btoa(auth.address),
                'Account-Address-Signature': btoa(auth.signature),
                'Access-Control-Allow-Origin': '*'
            }
        });
        if (response.ok) {
            return await response.json() as FolderModel[];
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to fetch folder list');
}

export const fetchAnalyzerFolders = async (auth: AuthInfo, analyzerId: string) => {
    try {
        const response = await fetch(`${API_URL}/analyzer/${analyzerId}`, {
            headers: {
                'Account-Address': btoa(auth.address),
                'Account-Address-Signature': btoa(auth.signature),
                'Access-Control-Allow-Origin': '*'
            }
        });
        if (response.ok) {
            return await response.json() as FolderModel[];
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to fetch folder list');
}

export const deleteFolder = async (auth: AuthInfo, id: string) => {
    try {
        const response = await fetch(`${API_URL}/${id}`, {
            method: 'DELETE',
            headers: {
                'Account-Address': btoa(auth.address),
                'Account-Address-Signature': btoa(auth.signature),
                'Access-Control-Allow-Origin': '*'
            }
        });
        if (response.ok) {
            return;
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to delete folder list');
}

export const removeAnalyzerFromFolder = async (auth: AuthInfo, folderId: string, analyzerId: string) => {
    try {
        const response = await fetch(`${API_URL}/${folderId}/analyzers`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json',
                'Account-Address': btoa(auth.address),
                'Account-Address-Signature': btoa(auth.signature),
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify({ids: [analyzerId]})
        });
        if (response.ok) {
            return;
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to delete folder list');
}

export const renameFolder = async (auth: AuthInfo, id: string, newName: string) => {
    try {
        const response = await fetch(`${API_URL}/${id}`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                'Account-Address': btoa(auth.address),
                'Account-Address-Signature': btoa(auth.signature),
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify({name: newName})
        });
        if (response.ok) {
            return true;
        } else if (response.status === 409) {
            return false;
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to delete folder list');
}

export const addAnalyzersToFolder = async (auth: AuthInfo, id: string, analyzerIds: string[]) => {
    try {
        const response = await fetch(`${API_URL}/${id}/analyzers`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Account-Address': btoa(auth.address),
                'Account-Address-Signature': btoa(auth.signature),
                'Access-Control-Allow-Origin': '*'
            },
            body: JSON.stringify({ ids: analyzerIds })
        });
        if (response.ok) {
            return;
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to add analyzers to folder');
}
