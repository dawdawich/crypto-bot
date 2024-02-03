import {CreateAPIMethod, FetchMethods, FetchWrapper} from './type'

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

export const createAPIMethod: CreateAPIMethod =
    (opts) => (input) => {
        const method = getHttpMethod(opts.method);
        const headers = prepareHeaders(opts.headers, input);
        const url = replaceUrlKey(opts.url, input);

        return (
            method(url, input, headers)
                // Imagine error handling here...
                .then(function (res) {

                    throw new Error('Failed to fetch data');
                })
        );
    };

const prepareHeaders = (headers: object | undefined, input: {
    [key: string]: any
}): Record<string, string> => {
    const authHeader = input.authToken ? {'Authorization': `Bearer ${input.authToken}`} : undefined
    delete input.authToken;
    return {
        'Accept': 'application/json',
        'Content-type': 'application/json; charset=UTF-8',
        'Access-Control-Allow-Origin': '*',
        ...authHeader,
        ...headers,
    }
}

const replaceUrlKey = (url: string, params: {
    [key: string]: any
}): string => {
    return url.replace(/\{([^}]+)\}/g, (match, key) => {
        if (params.hasOwnProperty(key)) {
            const value = params[key];
            delete params[key];
            return value;
        }
        return match;
    });
}

const getHttpMethod = (method: FetchMethods) => {
    switch (method) {
        case FetchMethods.GET:
            return get;
        case FetchMethods.POST:
            return post;
        case FetchMethods.PUT:
            return put;
        case FetchMethods.DELETE:
            return del;
        // Добавьте другие методы по мере необходимости
        default:
            throw new Error('Unsupported HTTP method');
    }
}


const get = async (
    url: string,
    input: Record<string, string|number>,
    headers: Record<string, string>
) => {
    const request = new Request(`${url}?${new URLSearchParams(input.toString()).toString()}`, {
        method: FetchMethods.GET,
        headers: headers
    });
    return fetch(request);
};

const post = async (
    url: string,
    input: any,
    headers: Record<string, string>
) => {
    const request = new Request(`${url}`, {
        method: FetchMethods.POST,
        headers: headers,
        body: input ? JSON.stringify(input) : null,
    });
    return fetch(request);
};

const put = async (
    url: string,
    input: Record<string, string|number>,
    headers: Record<string, string>
) => {
    const request = new Request(`${url}?${new URLSearchParams(input.toString()).toString()}`, {
        method: FetchMethods.PUT,
        headers: headers
    });
    return fetch(request);
};

const del = async (
    url: string,
    input: Record<string, string|number>,
    headers: Record<string, string>
) => {
    const request = new Request(`${url}?${new URLSearchParams(input.toString()).toString()}`, {
        method: FetchMethods.DELETE,
        headers: headers
    });
    return fetch(request);
};
