import {AnalyzerModel} from "../model/AnalyzerModel";
import {SERVER_HOST} from "./Constants";
import {Analyzer} from "../pages/analyzer/model/Analyzer";
import {createAPIMethod} from "../components/api/fetchWrapper";
import {FetchMethods} from "../components/api/type";

const API_URL = `${SERVER_HOST}/analyzer`;


export const fetchTopAnalyzersData = createAPIMethod<{}, Analyzer[]>({
    method: FetchMethods.GET,
    url: `${API_URL}/top20`,
});

export const fetchAnalyzerData = createAPIMethod<{
    analyzerId: string
}, Analyzer>({
    method: FetchMethods.GET,
    url: `${API_URL}/{analyzerId}`,
});

export const fetchAnalyzersList = createAPIMethod<{
    authToken: string,
    page: number,
    size: number
}, Analyzer | any>({
    method: FetchMethods.GET,
    url: `${API_URL}/`,
});

export const createAnalyzer = createAPIMethod<{
    analyzer: AnalyzerModel,
    authToken: string
}, any>({
    method: FetchMethods.POST,
    url: `${API_URL}`,
});

export const changeAnalyzerStatus = createAPIMethod<{
    id: string,
    status: string,
    authToken: string
}, any>({
    method: FetchMethods.PUT,
    url: `${API_URL}/{id}/{status}`,
});

export const deleteAnalyzer = createAPIMethod<{
    id: string,
    authToken: string
}, any>({
    method: FetchMethods.DELETE,
    url: `${API_URL}/{id}`,
});
