export enum FetchMethods {
  GET = 'get',
  POST = 'post',
  PUT = 'put',
  DELETE = 'delete',
}

export interface FetchWrapper {
  method: FetchMethods,
  url: string,
  headers?: object,
  token?: string | undefined,
  body?: any
}

export type CreateAPIMethod = <
  TInput extends Record<string, string|number|any>,
  TOutput
>(opts: {
  url: string;
  method: FetchMethods;
  headers?: object,
}) => (input: TInput) => Promise<TOutput>;
