import {AnalyzerModel} from "../model/AnalyzerModel";
import {SERVER_HOST} from "./Constants";
import {AnalyzerResponse} from "../model/AnalyzerResponse";
import {AuthInfo} from "../model/AuthInfo";
import {AnalyzerModelBulk} from "../model/AnalyzerModelBulk";
import {UnauthorizedError} from "../utils/errors/UnauthorizedError";
import {PaymentRequiredError} from "../utils/errors/PaymentRequiredError";

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

export const fetchAnalyzerData = async (auth: AuthInfo, analyzerId: string) => {
    try {
        const response = await fetch(`${API_URL}/${analyzerId}`, {
            headers: {
                'Account-Address': btoa(auth.address),
                'Account-Address-Signature': btoa(auth.signature),
                'Access-Control-Allow-Origin': '*'
            }
        });
        if (response.ok) {
            return await response.json();
        } else if (response.status === 401) {
            throw new UnauthorizedError('Signature is invalid');
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to fetch data');
}

export const fetchAnalyzersList = async (auth: AuthInfo,
                                         page: number,
                                         size: number,
                                         status: boolean | null,
                                         symbols: string[],
                                         sortOption: {name: string, direction: 'asc' | 'desc'} | null,
                                         folderId: string | null) => {
    try {
        let query = `page=${page}&size=${size}`;
        if (status !== null) {
            query += `&status=${status}`
        }
        if (symbols.length > 0) {
            query += `&symbols=${symbols.join(',')}`;
        }
        if (sortOption !== null) {
            query += `&field=${sortOption.name}&direction=${sortOption.direction}`;
        }
        if (folderId !== null) {
            query += `&folderId=${folderId}`;
        }
        const response = await fetch(`${API_URL}?${query}`, {
            headers: {
                'Account-Address': btoa(auth.address),
                'Account-Address-Signature': btoa(auth.signature),
                'Access-Control-Allow-Origin': '*'
            }
        });
        if (response.ok) {
            return await response.json() as {analyzers: AnalyzerResponse[], totalSize: number, activeSize: number, notActiveSize: number};
        } else if (response.status === 401) {
            throw new UnauthorizedError('Signature is invalid');
        }
    } catch (error) {
        console.error(error);
        throw error;
    }
    throw new Error('Failed to fetch analyzers list');
}

export const createAnalyzer = async (auth: AuthInfo, analyzer: AnalyzerModel) => {
    const response = await fetch(`${API_URL}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify(analyzer)
    });
    if (response.ok) {
        return;
    } else if (response.status === 401) {
        throw new UnauthorizedError('Signature is invalid');
    } else if (response.status === 402) {
        throw new PaymentRequiredError('Not enough active subscriptions');
    }
    throw new Error('Failed to create analyzer');
}

export const createAnalyzerBulk = async (auth: AuthInfo, analyzer: AnalyzerModelBulk) => {
    const response = await fetch(`${API_URL}/bulk`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify(analyzer)
    });
    if (response.ok) {
        return;
    } else if (response.status === 401) {
        throw new UnauthorizedError('Signature is invalid');
    } else if (response.status === 402) {
        throw new PaymentRequiredError('Not enough active subscriptions');
    }
    throw new Error('Failed to create analyzer');
}

export const changeBulkAnalyzerStatus = async (auth: AuthInfo, ids: string[], status: boolean) => {
    const path = status ? 'activate' : 'deactivate';
    const response = await fetch(`${API_URL}/${path}/bulk`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify({ids: ids})
    });
    if (response.ok) {
        return;
    } else if (response.status === 401) {
        throw new UnauthorizedError('Signature is invalid');
    } else if (response.status === 402) {
        throw new PaymentRequiredError('Not enough active subscriptions');
    }
    throw new Error('Failed to change status');
}

export const deleteAnalyzerBulk = async (auth: AuthInfo, ids: string[]) => {
    const response = await fetch(`${API_URL}`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify({ids: ids})
    });
    if (response.ok) {
        return;
    } else if (response.status === 401) {
        throw new UnauthorizedError('Signature is invalid');
    }
    throw new Error('Failed to delete analyzer');
}

export const resetAnalyzerBulk = async (auth: AuthInfo, ids: string[]) => {
    const response = await fetch(`${API_URL}/reset`, {
        method: 'PATCH',
        headers: {
            'Content-Type': 'application/json',
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify({ids: ids})
    });
    if (response.ok) {
        return;
    } else if (response.status === 401) {
        throw new UnauthorizedError('Signature is invalid');
    }
    throw new Error('Failed to reset analyzer');
}

export const getActiveAnalyzersCount = async (auth: AuthInfo) => {
    const response = await fetch(`${API_URL}/active/count`, {
        method: 'GET',
        headers: {
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        }
    });

    if (response.ok) {
        return parseInt(await response.text());
    } else if (response.status === 401) {
        throw new UnauthorizedError('Signature is invalid');
    }
    throw new Error('Failed to reset analyzer');
}
