import {SERVER_HOST} from "./Constants";

const API_URL = `${SERVER_HOST}/health-check`

export const fetchHealthcheckReport = async (authToken: string) => {
    const response = await fetch(`${API_URL}`, {
        method: 'GET',
        headers: {
            Authorization: `Bearer ${authToken}`
        }
    });
    if (response.ok) {
        return await response.json();
    }
    throw new Error('Failed to fetch symbols list');
}
