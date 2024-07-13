import {AnalyzerModel} from "../model/AnalyzerModel";
import {SERVER_HOST} from "./Constants";
import {AnalyzerResponse, parseAnalyzerResponse} from "../model/AnalyzerResponse";
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
    throw new Error('Failed to fetch data');
}

export const fetchAnalyzersList = async (auth: AuthInfo,
                                         page: number,
                                         size: number,
                                         status: boolean | null,
                                         symbols: string[],
                                         sortOption: { name: string, direction: 'asc' | 'desc' } | null,
                                         folderId: string | null) => {
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
        const result = await response.json() as {
            analyzers: any[],
            totalSize: number,
            activeSize: number,
            notActiveSize: number
        };

        let parsedAnalyzers: AnalyzerResponse[] = [];

        result.analyzers.forEach((item) => parsedAnalyzers.push(parseAnalyzerResponse(item)));

        return {analyzers: parsedAnalyzers, totalSize: result.totalSize, activeSize: result.activeSize, notActiveSize: result.notActiveSize};
    } else if (response.status === 401) {
        throw new UnauthorizedError('Signature is invalid');
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

/**
 * Changes the status of the bulk analyzer.
 *
 * @param {AuthInfo} auth - The authentication information.
 * @param {boolean} status - The new status to set (true for activating, false for deactivating).
 * @param {string[]} ids - The array of IDs of the bulk analyzers to update.  In case where [all] is {true}, means ids to exclude from list.
 * @param {boolean} [all=false] - Optional. Indicates whether to apply the status change to all bulk analyzers.
 * @throws {UnauthorizedError} When the signature is invalid.
 * @throws {PaymentRequiredError} When there are not enough active subscriptions.
 * @throws {Error} When failed to change the status.
 */
export const changeBulkAnalyzerStatus = async (auth: AuthInfo, status: boolean, ids: string[], all: boolean = false) => {
    const path = status ? 'activate' : 'deactivate';
    const response = await fetch(`${API_URL}/${path}/bulk`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify({ids, all})
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

/**
 * Deletes analyzers in bulk.
 *
 * @async
 * @param {AuthInfo} auth - The authorization information.
 * @param {string[]} ids - The IDs of the analyzers to delete. In case where [all] is {true}, means ids to exclude from list.
 * @param {boolean} [all=false] - Optional. Indicates whether to delete all analyzers.
 * @returns {Promise<void>} - A Promise that resolves if the operation is successful, or rejects with an error if it fails.
 * @throws {UnauthorizedError} - If the signature is invalid.
 * @throws {Error} - If failed to delete the analyzer.
 */
export const deleteAnalyzerBulk = async (auth: AuthInfo, ids: string[], all: boolean = false): Promise<void> => {
    const response = await fetch(`${API_URL}`, {
        method: 'DELETE',
        headers: {
            'Content-Type': 'application/json',
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify({ids, all})
    });
    if (response.ok) {
        return;
    } else if (response.status === 401) {
        throw new UnauthorizedError('Signature is invalid');
    }
    throw new Error('Failed to delete analyzer');
}

/**
 * Resets analyzers for a bulk of items.
 *
 * @param {AuthInfo} auth - The authentication information.
 * @param {string[]} ids - The IDs of the items to reset. In case where [all] is {true}, means ids to exclude from list.
 * @param {boolean} [all=false] - Flag indicating if all items should be reset.
 * @throws {UnauthorizedError} If the signature is invalid.
 * @throws {Error} If the reset operation fails.
 */
export const resetAnalyzerBulk = async (auth: AuthInfo, ids: string[], all: boolean = false) => {
    const response = await fetch(`${API_URL}/reset`, {
        method: 'PATCH',
        headers: {
            'Content-Type': 'application/json',
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify({ids, all})
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
