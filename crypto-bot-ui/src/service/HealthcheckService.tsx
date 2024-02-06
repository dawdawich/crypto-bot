import {SERVER_HOST} from "./Constants";
import {AuthInfo} from "../model/AuthInfo";

const API_URL = `${SERVER_HOST}/health-check`

export const fetchHealthcheckReport = async (auth: AuthInfo) => {
    const response = await fetch(`${API_URL}`, {
        method: 'GET',
        headers: {
            'Account-Address': btoa(auth.address),
            'Account-Address-Signature': btoa(auth.signature),
        }
    });
    if (response.ok) {
        return await response.json();
    }
    throw new Error('Failed to fetch symbols list');
}
