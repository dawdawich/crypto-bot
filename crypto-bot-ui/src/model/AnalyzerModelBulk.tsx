export interface AnalyzerModelBulk {
    symbols: string[];
    stopLossMin: number;
    stopLossMax: number;
    stopLossStep: number;
    takeProfitMin: number;
    takeProfitMax: number;
    takeProfitStep: number;
    diapasonMin: number;
    diapasonMax: number;
    diapasonStep: number;
    gridSizeMin: number;
    gridSizeMax: number;
    gridSizeStep: number;
    multiplierMin: number;
    multiplierMax: number;
    multiplierStep: number;
    startCapital: number;
    demoAccount: boolean;
    market: string;
    active: boolean;
    public: boolean;
    strategy: string;
    folders: string[];
}