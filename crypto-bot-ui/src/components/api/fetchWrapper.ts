import type { FetchWrapperInit, State } from './type'
import { FetchMethods } from './type'

export function fetchWrapper(init: FetchWrapperInit) {
  const state: State = {
    baseUrl: init.baseUrl,
    token: init.token,
    headers: init.headers,
    data: null,
  }

  async function http<T>(url: string, config: RequestInit): Promise<Response> {
    const request = new Request(url, {
      ...config,
      headers: {
        'Accept': 'application/json',
        'Content-type': 'application/json; charset=UTF-8',
        'Access-Control-Allow-Origin': '*',
        'Authorization': `Bearer ${state.token || ''}`,
        ...state.headers,
      },
      body: config.body ? JSON.stringify(config.body) : null,
    })

    return await fetch(request)
  }

  function getPath(path: string): string {
    return `${state.baseUrl}/${path}`
  }

  async function methodGET<T>(path: string){
    return http<T>(getPath(path), {
      method: FetchMethods.GET,
    })
  }

  async function methodPOST<T>(path: string, data?: any){
    return http<T>(getPath(path), {
      method: FetchMethods.POST,
      body: data
    })
  }

  async function methodPATCH<T>(path: string, data: any){
    return http<T>(getPath(path), {
      method: FetchMethods.PATCH,
      body: data,
    })
  }

  async function methodPUT<T>(path: string, data?: any){
    return http<T>(getPath(path), {
      method: FetchMethods.PUT,
      body: data,
    })
  }

  async function methodDELETE(path: string) {
    return http(getPath(path), {
      method: FetchMethods.DELETE,
    })
  }

  return {
    methodGET,
    methodPOST,
    methodPATCH,
    methodPUT,
    methodDELETE,
  }
}