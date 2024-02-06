import {SERVER_HOST} from "./Constants";
import {fetchWrapper} from "../components/api/fetchWrapper";
import {FetchMethods} from "../components/api/type";

const API_URL = `${SERVER_HOST}/health-check`

export const fetchHealthcheckReport = async (authToken: string) => {
    const response = await fetchWrapper({
        url: `${API_URL}`,
        method: FetchMethods.GET,
        token: authToken
    });
    //TODO: Handling response and errors in next steps
    if (response.ok) {
        return await response.json();
    }
    throw new Error('Failed to fetch symbols list');
}
