export interface State {
  baseUrl: string
  token: string | undefined
  headers?: object
  data: null | any
}

export interface FetchWrapperInit {
  baseUrl: string
  headers?: object
  token?: string
}

export enum FetchMethods {
  GET = 'get',
  POST = 'post',
  PUT = 'put',
  PATCH = 'patch',
  DELETE = 'delete',
}

export type WithoutId<T> = Omit<T, 'id'>

export interface ApiMethods {
  get: <T>(path: string) => Promise<Response>
  post: <T extends WithoutId<T>>(path: string, data?: T) => Promise<Response>
  patch: <T>(path: string, data: Partial<T>) => Promise<Response>
  put: <T>(path: string, data: Partial<T>) => Promise<Response>
  delete: <T>(path: string, data?: T) => Promise<Response>
}

export interface FetchWrapperResponse<T> {
  success: boolean
  status: number
  data: T | null
  statusText: string
}