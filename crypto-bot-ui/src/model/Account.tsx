import {ApiToken} from "./ApiToken";

export type Account = {
    id: string;
    username: string;
    name: string;
    surname: string;
    email: string;
    createTime: number;
    tokens: ApiToken[];
}
