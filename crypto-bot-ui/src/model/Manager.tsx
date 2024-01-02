export type Manager = {
    id: string;
    chooseStrategy: string;
    customAnalyzerId: string;
    stopLoss: number | null;
    takeProfit: number | null;
    status: string;
    stopDescription: string | null;
    errorDescription: string | null;
    apiTokenId: string;
    createTime: number;
    updateTime: number;
}
