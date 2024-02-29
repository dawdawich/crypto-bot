export type AnalyzerResponse = {
    id: string;
    diapason: number;
    gridSize: number;
    multiplier: number;
    positionStopLoss: number;
    positionTakeProfit: number;
    symbol: string;
    startCapital: number;
    money: number;
    isActive: boolean;
    demoAccount: boolean;
    strategy: string;
    market: string;
    public: boolean;
    createTime: number;
    updateTime: number;
    stabilityCoef: number;
    pNl1: number;
    pNl12: number;
    pNl24: number;
}
