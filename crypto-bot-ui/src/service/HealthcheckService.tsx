import {SERVER_HOST} from "./Constants";
import {fetchWrapper} from "../components/api/fetchWrapper";

const API_URL = `${SERVER_HOST}/health-check`

export const fetchHealthcheckReport = async (authToken: string) => {
    const path = ``;
    const request = fetchWrapper({baseUrl: API_URL, token: authToken});
    const response = await request.methodGET(path);
    //TODO: Handling response and errors in next steps
    if (response.ok) {
        return await response.json();
    }
    throw new Error('Failed to fetch symbols list');
}
