export interface AnalyzerModel {
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

export interface GridAnalyzerModel extends AnalyzerModel {
    diapason: number;
    gridSize: number;
}

export interface CandleAnalyzerModel extends AnalyzerModel {
    kLineDuration: number;
}

export function isGridAnalyzerModel(obj: any): obj is GridAnalyzerModel {
    return (
        'diapason' in obj &&
        'gridSize' in obj &&
        typeof obj.diapason === 'number' &&
        typeof obj.gridSize === 'number'
    );
}

// Type guard for CandleAnalyzerModel
export function isCandleAnalyzerModel(obj: any): obj is CandleAnalyzerModel {
    return (
        'kLineDuration' in obj &&
        typeof obj.kLineDuration === 'number'
    );
}

// Function to parse JSON and map to the appropriate interface
export function parseAnalyzerModel(json: any): AnalyzerModel {
    if (isGridAnalyzerModel(json)) {
        return json as GridAnalyzerModel;
    } else if (isCandleAnalyzerModel(json)) {
        return json as CandleAnalyzerModel;
    } else {
        throw new Error('Unknown AnalyzerModel type');
    }
}
