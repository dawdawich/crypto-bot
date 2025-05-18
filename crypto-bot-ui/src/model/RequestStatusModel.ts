export interface RequestStatusModel {
    id: string;
    accountId: string;
    status: 'SUCCESS' | 'IN_PROGRESS' | 'FAILED';
}
