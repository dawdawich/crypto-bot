import {SERVER_HOST} from "./Constants";
import {FetchMethods} from "../components/api/type";
import {createAPIMethod} from "../components/api/fetchWrapper";

const API_URL = `${SERVER_HOST}/health-check`

export const fetchHealthcheckReport = createAPIMethod<{
    authToken: string
}, any>({
    method: FetchMethods.GET,
    url: `${API_URL}`,
});
