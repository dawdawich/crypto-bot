import {AuthInfo} from "../model/AuthInfo";
import {SERVER_HOST} from "./Constants";
import {FolderModel} from "../model/FolderModel";
import {UnauthorizedError} from "../utils/errors/UnauthorizedError";

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
    } else if (response.status === 401) {
        throw new UnauthorizedError('Signature is invalid');
    }
    throw new Error('Failed to create folder');
}

export const fetchFolderList = async (auth: AuthInfo) => {
    const response = await fetch(`${API_URL}`, {
        headers: {
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        }
    });
    if (response.ok) {
        return await response.json() as FolderModel[];
    } else if (response.status === 401) {
        throw new UnauthorizedError('Signature is invalid');
    }
    throw new Error('Failed to fetch folder list');
}

export const fetchAnalyzerFolders = async (auth: AuthInfo, analyzerId: string) => {
    const response = await fetch(`${API_URL}/analyzer/${analyzerId}`, {
        headers: {
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        }
    });
    if (response.ok) {
        return await response.json() as FolderModel[];
    } else if (response.status === 401) {
        throw new UnauthorizedError('Signature is invalid');
    }
    throw new Error('Failed to fetch folder list');
}

export const deleteFolder = async (auth: AuthInfo, id: string) => {
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
    } else if (response.status === 401) {
        throw new UnauthorizedError('Signature is invalid');
    }
    throw new Error('Failed to delete folder list');
}

export const removeAnalyzerFromFolder = async (auth: AuthInfo, folderId: string, analyzerId: string) => {
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
    } else if (response.status === 401) {
        throw new UnauthorizedError('Signature is invalid');
    }
    throw new Error('Failed to delete folder list');
}

export const renameFolder = async (auth: AuthInfo, id: string, newName: string) => {
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
    } else if (response.status === 401) {
        throw new UnauthorizedError('Signature is invalid');
    }
    throw new Error('Failed to delete folder list');
}

/**
 * Adds analyzers to a folder.
 * @param {AuthInfo} auth - The authentication information.
 * @param {string} id - The ID of the folder.
 * @param {string[]} analyzerIds - The IDs of the analyzers to add. If param {all} will be provided as 'true', {analyzerIds} will be ids to skip.
 * @param {boolean} [all=false] - Indicates whether to add all analyzers or just the specified ones.
 * @throws {UnauthorizedError} If the signature is invalid.
 * @throws {Error} If failed to add analyzers to folder.
 */
export const addAnalyzersToFolder = async (auth: AuthInfo, id: string, analyzerIds: string[], all: boolean = false) => {
    const response = await fetch(`${API_URL}/${id}/analyzers`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify({ids: analyzerIds, all})
    });
    if (response.ok) {
        return;
    } else if (response.status === 401) {
        console.warn('Signature is invalid');
        throw new UnauthorizedError('Signature is invalid');
    }
    throw new Error('Failed to add analyzers to folder');
}
