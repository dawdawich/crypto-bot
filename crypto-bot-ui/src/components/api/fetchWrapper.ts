import {FetchWrapper} from './type'

export const fetchWrapper = (init: FetchWrapper): Promise<Response> => {
    const authHeader = init.token ? {'Authorization': `Bearer ${init.token}`} : undefined

    const request = new Request(init.url, {
        method: init.method,
        headers: {
            'Accept': 'application/json',
            'Content-type': 'application/json; charset=UTF-8',
            'Access-Control-Allow-Origin': '*',
            ...authHeader,
            ...init.headers,
        },
        body: init.body ? JSON.stringify(init.body) : null,
    })

    return fetch(request)
}
