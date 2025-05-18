import {SERVER_HOST} from "./Constants";
import {AuthInfo} from "../model/AuthInfo";
import {RequestStatusModel} from "../model/RequestStatusModel";
import {BacktestRequestModel} from "../pages/backtest/dialog/CreateBackTestDialog";
import {BackTestResults} from "../pages/backtest/BackTestDetailPage";

const API_URL = `${SERVER_HOST}/backtest`;

export const fetchRequestStatuses = async (auth: AuthInfo): Promise<RequestStatusModel[]> => {
    const response = await fetch(`${API_URL}/request/all`, {
        method: 'GET',
        headers: {
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        }
    });
    if (response.ok) {
        return await response.json();
    }

    throw new Error('Failed to fetch request statuses');
}

export const fetchRequestResult = async (auth: AuthInfo, requestId: string): Promise<BackTestResults> => {
    const response = await fetch(`${API_URL}/request/${requestId}`, {
        method: 'GET',
        headers: {
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Access-Control-Allow-Origin': '*'
        }
    });
    if (response.ok) {
        return await response.json();
    }

    throw new Error('Failed to fetch request result');
}

export const createBackTest = async (request: BacktestRequestModel, auth: AuthInfo): Promise<{requestId: string}> => {
    const response = await fetch(`${API_URL}`, {
        method: 'POST',
        headers: {
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify(request)
    });
    if (response.ok) {
        return await response.json();
    }

    throw new Error('Failed to create backtest');
}
