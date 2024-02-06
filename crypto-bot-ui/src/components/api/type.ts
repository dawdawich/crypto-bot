export enum FetchMethods {
  GET = 'get',
  POST = 'post',
  PUT = 'put',
  PATCH = 'patch',
  DELETE = 'delete',
}

export interface FetchWrapper {
  method: FetchMethods,
  url: string,
  headers?: object,
  token?: string | undefined,
  body?: any
}
