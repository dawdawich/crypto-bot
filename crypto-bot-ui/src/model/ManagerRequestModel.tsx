export type ManagerRequestModel = {
    apiTokenId: string;
    customName: string | null;
    status: 'ACTIVE' | 'INACTIVE';
    analyzerChooseStrategy: 'BIGGEST_BY_MONEY' | 'MOST_STABLE';
    refreshAnalyzerTime: number;
    stopLoss: number | null;
    takeProfit: number | null;
    folder: string | 'ALL';
}
