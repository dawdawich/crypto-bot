export type UserRole = 'ADMIN' | 'USER'

export interface UserModel {
    id: string | null;
    username: string | null;
    name: string | null;
    surname: string | null;
    email: string | null;
    token: string;
}
