export type kLineDurations = 1 | 3 | 5 | 15 | 30 | 60 | 120 | 240 | 360 | 720;
export const kLineDurationValues: kLineDurations[] = [1, 3, 5, 15, 30, 60, 120, 240, 360, 720];

export interface AnalyzerResponse {
    id: string;
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
    pnl1: number;
    pnl12: number;
    pnl24: number;
    symbolVolatile?: number;
}

export function isGridAnalyzerResponse(obj: any): obj is GridAnalyzerResponse {
    return (
        'diapason' in obj &&
        'gridSize' in obj &&
        typeof obj.diapason === 'number' &&
        typeof obj.gridSize === 'number'
    );
}

export function isCandleTailAnalyzerResponse(obj: any): obj is CandleTailAnalyzerResponse {
    return (
        'klineDuration' in obj &&
        kLineDurationValues.includes(obj.klineDuration)
    );
}

export interface GridAnalyzerResponse extends AnalyzerResponse {
    diapason: number;
    gridSize: number;
}

export interface CandleTailAnalyzerResponse extends AnalyzerResponse {
    kLineDuration: kLineDurations;
}

export function parseAnalyzerResponse(json: any): AnalyzerResponse {
    if (isGridAnalyzerResponse(json)) {
        return json as GridAnalyzerResponse;
    } else if (isCandleTailAnalyzerResponse(json)) {
        return json as CandleTailAnalyzerResponse;
    } else {
        throw new Error('Unknown AnalyzerResponse type');
    }
}
