import {SERVER_HOST} from "./Constants";

const API_URL = `${SERVER_HOST}/account`;

export const requestSalt = async (accountId: string) => {
    const response = await fetch(`${API_URL}/salt`, {
        method: 'GET',
        headers: {
            'Account-Address': btoa(accountId)
        }
    });

    if (response.ok) {
        return await response.text() as string;
    }

    throw Error('Failed to fetch salt for account');
}
