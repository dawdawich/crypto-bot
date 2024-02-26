export interface AnalyzerModel {
    diapason: number;
    gridSize: number;
    multiplier: number;
    stopLoss: number;
    takeProfit: number;
    symbol: string;
    startCapital: number;
    active: boolean;
    public: boolean;
    folders: string[];
    strategy: string;
    market: string;
    demoAccount: boolean;
}
